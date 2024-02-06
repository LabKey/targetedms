/*
 * Copyright (c) 2012-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.targetedms.IModification;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.targetedms.TargetedMSManager.getSchema;

public class ModificationManager
{
    private static final int CACHE_SIZE = 10; // Cache results for up to 10 runs.
    private static final Cache<String, Map<Long, Set<Pair<Integer, Integer>>>> PEPTIDE_STR_MOD_INDEXES = CacheManager.getCache(CACHE_SIZE, CacheManager.DAY, "Peptide structural modification indexes");
    private static final Cache<String, Map<Pair<Long,Long>, Set<Integer>>> PEPTIDE_ISOTOPE_MOD_INDEXES = CacheManager.getCache(CACHE_SIZE, CacheManager.DAY, "Peptide isotope modification indexes");

    private ModificationManager() {}

    public static IModification.IStructuralModification getStructuralModification(long id)
    {
        // This table does not contain rows scoped to a container so we don't need a container filter
        return new TableSelector(TargetedMSManager.getTableInfoStructuralModification()).getObject(id, PeptideSettings.StructuralModification.class);
    }

    public static IModification.IIsotopeModification getIsotopeModification(long id)
    {
        // This table does not contain rows scoped to a container so we don't need a container filter
        return new TableSelector(TargetedMSManager.getTableInfoIsotopeModification()).getObject(id, PeptideSettings.IsotopeModification.class);
    }

    /**
     * @param peptideId
     * @return Map of the modified amino acid index in the peptide sequence
     *         and the mass of the modification.
     *         ModifiedIndex -> MassDiff
     */
    public static Map<Pair<Integer, Integer>, Double> getPeptideStructuralModsMap(long peptideId)
    {
        final Map<Pair<Integer, Integer>, Double> strModIndexMassDiff = new HashMap<>();
        String sql = "SELECT IndexAa, MassDiff, PeptideIndex "+
                     "FROM "+ TargetedMSManager.getTableInfoPeptideStructuralModification()+" "+
                     "WHERE PeptideId=?";
        SQLFragment sf = new SQLFragment(sql, peptideId);

        new SqlSelector(getSchema(), sf).forEach(rs -> {
            int indexAA = rs.getInt("IndexAa");
            int peptideIndex = rs.getInt("PeptideIndex");
            double massDiff = rs.getDouble("MassDiff");

            Pair<Integer, Integer> key = Pair.of(peptideIndex, indexAA);

            Double diffAtIndex = strModIndexMassDiff.get(key);
            if (diffAtIndex != null)
            {
                massDiff += diffAtIndex;
            }
            strModIndexMassDiff.put(key, massDiff);
        });

        return strModIndexMassDiff;
    }

    /**
     * @param peptideId
     * @return Map of the modified amino acid index in the peptide sequence
     *         and the mass of the modification.
     *         ModifiedIndex -> MassDiff
     */
    public static Map<Integer, Double> getPeptideIsotopeModsMap(long peptideId, long isotopeLabelId)
    {
        final Map<Integer, Double> isotopeModIndexMassDiff = new HashMap<>();
        String sql = "SELECT pm.IndexAa, pm.MassDiff "+
                     "FROM "+
                     TargetedMSManager.getTableInfoPeptideIsotopeModification()+" AS pm, "+
                     TargetedMSManager.getTableInfoRunIsotopeModification()+" AS m "+
                     "WHERE pm.IsotopeModId=m.IsotopeModId "+
                     "AND pm.PeptideId=? "+
                     "AND m.IsotopeLabelId=?";
        SQLFragment sf = new SQLFragment(sql, peptideId, isotopeLabelId);

        new SqlSelector(getSchema(), sf).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                int index = rs.getInt("IndexAa");
                double massDiff = rs.getDouble("MassDiff");

                Double diffAtIndex = isotopeModIndexMassDiff.get(index);
                if(diffAtIndex != null)
                {
                    massDiff += diffAtIndex;
                }
                isotopeModIndexMassDiff.put(index, massDiff);
            }
        });

        return isotopeModIndexMassDiff;
    }

    /**
     * Returns a list of isotopic modifications for a run (Skyline document).  This includes all the isotopic modifications
     * that were checked in the Peptide Settings > Modifications tab in Skyline.  The modifications are under
     * <peptide_settings> -> <peptide_modification> -> <heavy_modifications> element in the .sky file.  They are stored
     * in the targetedms.RunIsotopeModification table.
     * @param runId
     * @return List of isotopic modifications
     */
    public static List<PeptideSettings.RunIsotopeModification> getIsotopeModificationsForRun(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoIsotopeModification(), "mod");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRunIsotopeModification(), "rmod");
        sql.append(" WHERE");
        sql.append(" mod.Id = rmod.IsotopeModId");
        sql.append(" AND rmod.RunId = ?");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideSettings.RunIsotopeModification.class);
    }

    /**
     * Returns a list of structural modifications for a run (Skyline document).  This includes all the structural modifications
     * that were checked in the Peptide Settings > Modifications tab in Skyline.  The modifications are under
     * <peptide_settings> -> <peptide_modification> -> <static_modifications> element in the .sky file.  They are stored
     * in the targetedms.RunStructuralModification table.
     * @param runId
     * @return List of structural modifications
     */
    public static List<PeptideSettings.RunStructuralModification> getStructuralModificationsForRun(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoStructuralModification(), "mod");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRunStructuralModification(), "rmod");
        sql.append(" WHERE");
        sql.append(" mod.Id = rmod.StructuralModId");
        sql.append(" AND rmod.RunId = ?");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideSettings.RunStructuralModification.class);
    }

    /**
     * Returns a list of isotopic modifications found in at least one peptide in the run (Skyline document).
     * @param runId
     * @return List of isotopic modifications
     */
    public static List<PeptideSettings.IsotopeModification> getIsotopeModificationsUsedInRun(long runId)
    {
        return getModificationsUsedInRun(runId, TargetedMSManager.getTableInfoPeptideIsotopeModification(),
                TargetedMSManager.getTableInfoIsotopeModification(), "IsotopeModId",
                PeptideSettings.IsotopeModification.class);
    }

    /**
     * Returns a list of structural modifications found in at least one peptide in the run (Skyline document).
     * @param runId
     * @return List of structural modifications
     */
    public static List<PeptideSettings.StructuralModification> getStructuralModificationsUsedInRun(long runId)
    {
        return getModificationsUsedInRun(runId, TargetedMSManager.getTableInfoPeptideStructuralModification(),
                TargetedMSManager.getTableInfoStructuralModification(), "StructuralModId",
                PeptideSettings.StructuralModification.class);
    }

    private static <T> List<T> getModificationsUsedInRun(long runId, TableInfo peptideModsTable, TableInfo modsTable, String modIdCol, Class<T> type)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT DISTINCT (pmod.").append(modIdCol).append("), mod.* FROM ");
        sql.append(peptideModsTable, "pmod");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "m");
        sql.append(" ON pmod.peptideId = m.Id ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON m.peptideGroupId = pg.Id ");
        sql.append(" INNER JOIN ");
        sql.append(modsTable, "mod");
        sql.append(" ON pmod.").append(modIdCol).append(" = mod.Id ");
        sql.append(" WHERE pg.RunId = ? ");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(type);
    }

    public static List<PeptideSettings.PotentialLoss> getPotentialLossesForStructuralMod(long modId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoStructuralModLoss(),
                                 new SimpleFilter(FieldKey.fromParts("StructuralModId"), modId),
                                 null)
                                .getArrayList(PeptideSettings.PotentialLoss.class);
    }

    public static List<Peptide.StructuralModification> getPeptideStructuralModifications(long peptideId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPeptideStructuralModification(),
                                  new SimpleFilter(FieldKey.fromParts("PeptideId"), peptideId),
                                  null)
                                  .getArrayList(Peptide.StructuralModification.class);
    }

    public static List<Peptide.IsotopeModification> getPeptideIsotopelModifications(long peptideId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPeptideIsotopeModification(),
                                  new SimpleFilter(FieldKey.fromParts("PeptideId"), peptideId),
                                  null)
                                  .getArrayList(Peptide.IsotopeModification.class);
    }

    public static List<Peptide.IsotopeModification> getPeptideIsotopelModifications(long peptideId, long isotopeLabelId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT pi.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideIsotopeModification(), "pi");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRunIsotopeModification(), "i");
        sql.append(" WHERE ");
        sql.append(" pi.peptideId = ?");
        sql.add(peptideId);
        sql.append(" AND i.isotopeLabelId = ?");
        sql.add(isotopeLabelId);
        sql.append(" AND pi.IsotopeModId=i.IsotopeModId");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Peptide.IsotopeModification.class);
    }

    public static PeptideSettings.ModificationSettings getSettings(long runId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoModificationSettings()).getObject(runId, PeptideSettings.ModificationSettings.class);
    }


    /**
     * @return    PeptideId -> (Set of indexes where the peptide has structural modifications)
     */
    private static Map<Long, Set<Pair<Integer, Integer>>> getPeptideStructuralModIndexMap(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append(" SELECT mod.PeptideId AS peptideId, mod.IndexAA AS indexAA, mod.PeptideIndex AS peptideIndex");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideStructuralModification(), "mod");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE run.id = ? ");
        sql.add(runId);
        sql.append(" AND ");
        sql.append(" pg.runId = run.id ");
        sql.append(" AND ");
        sql.append(" gm.peptideGroupId = pg.id ");
        sql.append(" AND ");
        sql.append(" mod.peptideId = gm.id ");

        final Map<Long, Set<Pair<Integer, Integer>>> peptideModIndexMap = new HashMap<>();

        new SqlSelector(getSchema(), sql).forEach(rs -> {
            long peptideId = rs.getLong("peptideId");
            int indexAA = rs.getInt("indexAa");
            int peptideIndex = rs.getInt("peptideIndex");

            Set<Pair<Integer, Integer>> modIndexes = peptideModIndexMap.computeIfAbsent(peptideId, k -> new HashSet<>());
            modIndexes.add(Pair.of(peptideIndex, indexAA));
        });

        Map<Long, Set<Pair<Integer, Integer>>> immutableMap = new HashMap<>();
        peptideModIndexMap.forEach((k, v) -> immutableMap.put(k, Collections.unmodifiableSet(v)));

        return Collections.unmodifiableMap(immutableMap);
    }

    /**
     * @return  PeptideId/IsotopeLabelId -> Set of indexes where the PeptideId/IsotopeLabelId has a isotope modification
     */
    private static Map<Pair<Long,Long>, Set<Integer>> getPeptideIsotopeModIndexMap(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append(" SELECT mod.PeptideId AS peptideId, rmod.isotopeLabelId AS isotopeLabelId, mod.IndexAA AS indexAA ");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideIsotopeModification(), "mod");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoRunIsotopeModification(), "rmod");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE rmod.runId = ? ");
        sql.add(runId);
        sql.append(" AND ");
        sql.append(" pg.runId = rmod.runId ");
        sql.append(" AND ");
        sql.append(" gm.peptideGroupId = pg.id ");
        sql.append(" AND ");
        sql.append(" mod.peptideId = gm.id ");
        sql.append(" AND ");
        sql.append(" mod.isotopeModId = rmod.isotopeModId ");

        final Map<Pair<Long, Long>, Set<Integer>> peptideModIndexMap = new HashMap<>();

        new SqlSelector(getSchema(), sql).forEach(rs -> {
            Pair<Long, Long> key = new Pair<>(rs.getLong("peptideId"), rs.getLong("isotopeLabelId"));
            int index = rs.getInt("indexAa");

            Set<Integer> modIndexes = peptideModIndexMap.computeIfAbsent(key, k -> new HashSet<>());
            modIndexes.add(index);
        });

        return Collections.unmodifiableMap(peptideModIndexMap);
    }

    /** Pair is the index of the cross-linked peptide (or 0 for non-cross-linked peptides) and the amino acid index */
    public static Set<Pair<Integer, Integer>> getStructuralModIndexes(long peptideId, Long runId)
    {
        if(runId == null)
        {
            Map<Pair<Integer, Integer>, Double> modMap = getPeptideStructuralModsMap(peptideId);
            return new HashSet<>(modMap.keySet());
        }
        else
        {
            Map<Long, Set<Pair<Integer, Integer>>> modIndexesForRun = PEPTIDE_STR_MOD_INDEXES.get(String.valueOf(runId), null, (runId1, argument) -> getPeptideStructuralModIndexMap(Long.valueOf(runId1)));
            return modIndexesForRun.getOrDefault(peptideId, Collections.emptySet());
        }
    }

    public static Set<Integer> getIsotopeModIndexes(long peptideId, long isotopeLabelId, Long runId)
    {
        if(runId == null)
        {
            Map<Integer, Double> modMap = ModificationManager.getPeptideIsotopeModsMap(peptideId, isotopeLabelId);
            return new HashSet<>(modMap.keySet());
        }
        else
        {
            Map<Pair<Long,Long>, Set<Integer>> modIndexesForRun = PEPTIDE_ISOTOPE_MOD_INDEXES.get(String.valueOf(runId), null,
                    (runId1, argument) -> getPeptideIsotopeModIndexMap(Long.valueOf(runId1)));

            return modIndexesForRun.get(new Pair<>(peptideId, isotopeLabelId));
        }
    }

    public static void removeRunCachedResults(List<Long> deletedRunIds)
    {
        if(deletedRunIds == null)
            return;

        for(Long runId: deletedRunIds)
        {
            PEPTIDE_ISOTOPE_MOD_INDEXES.remove(String.valueOf(runId));
            PEPTIDE_STR_MOD_INDEXES.remove(String.valueOf(runId));
        }
    }
}
