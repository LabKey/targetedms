/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DatabaseCache;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableResultSet;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.model.PrecursorChromInfoLitePlus;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.RepresentativeDataState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:08 PM
 */
public class PrecursorManager
{
    private static final int CACHE_SIZE = 10; // Cache results for upto 10 runs.
    private static PrecursorIdsWithChromatograms _precursorIdsWithChromatograms = new PrecursorIdsWithChromatograms();
    private static PrecursorIdsWithSpectra _precursorIdsWithSpectra = new PrecursorIdsWithSpectra();


    private PrecursorManager() {}

    public static Precursor get(int precursorId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPrecursor(),
                                 new SimpleFilter(FieldKey.fromParts("Id"), precursorId),
                                 null).getObject(Precursor.class);
    }

    public static Precursor getPrecursor(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT pre.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pre.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Precursor.class);
    }

    public static PrecursorChromInfo getPrecursorChromInfo(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT pci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pci.PrecursorId = pre.Id AND pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(PrecursorChromInfo.class);
    }

    public static List<Precursor> getPrecursorsForPeptide(int peptideId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PeptideId"), peptideId);

        Sort sort = new Sort("Charge, IsotopeLabelId");

        List<Precursor> precursors = new TableSelector(TargetedMSManager.getTableInfoPrecursor(), filter, sort).getArrayList(Precursor.class);

        if (precursors.isEmpty())
        {
            throw new NotFoundException(String.format("No precursors found for peptideId %d", peptideId));
        }

        return precursors;
    }

    public static List<PrecursorChromInfo> getSortedPrecursorChromInfosForPrecursor(int precursorId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorId"), precursorId);

        return new TableSelector(TargetedMSManager.getTableInfoPrecursorChromInfo(),
                                   filter,
                                   new Sort("-TotalArea"))
                                  .getArrayList(PrecursorChromInfo.class);
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPrecursor(int precursorId)
    {
        return getBestPrecursorChromInfoForPrecursorAndReplicate(precursorId, -1);
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPrecursorAndReplicate(int precursorId, int replicateId)
    {
        // Get a list of chrom infos sorted by peak area (descending).
        List<PrecursorChromInfo> chromInfos = getSortedPrecursorChromInfosForPrecursor(precursorId);

        if(chromInfos == null || chromInfos.size() == 0)
        {
            return null;
        }
        else
        {
            // If we only want the best sample file from a replicate, get a list of sample file Ids for the
            // given replicate.
            Set<Integer> sampleFileIds = getSampleFileIdsForReplicate(replicateId);

            // Look for one that has area information and the peaks are not truncated
            for (PrecursorChromInfo chromInfo : chromInfos)
            {

                if (chromInfo.getTotalArea() != null && (chromInfo.getNumTruncated() == null || chromInfo.getNumTruncated().intValue() == 0))
                {
                    if(sampleFileIds.size() == 0 || sampleFileIds.contains(chromInfo.getSampleFileId()))
                    {
                        return chromInfo;
                    }
                }
            }

            // If we did not find a chrom info with a non-null precursor area and non-truncated peaks,
            // return the first chrom info with non-null precursor area.
            for(PrecursorChromInfo chromInfo: chromInfos)
            {
                if(chromInfo.getTotalArea() != null)
                    return chromInfo;
            }
            return chromInfos.get(0);
        }
    }

    public static Set<Integer> getSampleFileIdsForReplicate(int replicateId)
    {
        if(replicateId <= 0)
            return Collections.emptySet();

        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("ReplicateId"), replicateId);

        Collection<Integer> result =  new TableSelector(TargetedMSManager.getTableInfoSampleFile(),
                                   Collections.singleton("Id"),
                                   filter,
                                   null)
                                  .getCollection(Integer.class);
        return new HashSet<>(result);
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPeptide(int peptideId)
    {
        List<PrecursorChromInfo> chromInfos = getPrecursorChromInfosForPeptide(peptideId);

        if(chromInfos == null || chromInfos.size() == 0)
        {
            return null;
        }
        else
        {
            Collections.sort(chromInfos, new Comparator<PrecursorChromInfo>()
            {
                @Override
                public int compare(PrecursorChromInfo o1, PrecursorChromInfo o2)
                {
                    if( (o1 == o2 ) || (o1.getTotalArea() == o2.getTotalArea()) ) { return 0; }
                    else if(o1.getTotalArea() == null) {return 1;}
                    else if(o2.getTotalArea() == null) {return -1;}

                    return o2.getTotalArea().compareTo(o1.getTotalArea());
                }
            });

            for (PrecursorChromInfo chromInfo : chromInfos)
            {
                if (chromInfo.getTotalArea() != null && (chromInfo.getNumTruncated() == null || chromInfo.getNumTruncated().intValue() == 0))
                {
                    return chromInfo;
                }
            }
            return chromInfos.get(0);
        }
    }

    public static List<PrecursorChromInfo> getPrecursorChromInfosForPeptide(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.*");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(" ON ");
        sql.append("prec.Id = pci.PrecursorId ");
        sql.append(" WHERE ");
        sql.append("prec.PeptideId=? ");
        sql.add(peptideId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfo.class);
    }

    public static Map<String, Object> getPrecursorSummary(int precursorId)
    {
        SQLFragment sf = new SQLFragment("SELECT pre.Mz, pre.Charge, pep.Sequence, label.Name FROM ");
        sf.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label ");
        sf.append(" WHERE pre.PeptideId = pep.Id AND pre.IsotopeLabelId = label.Id AND pre.Id = ? ");
        sf.add(precursorId);

        try (TableResultSet rs = new SqlSelector(TargetedMSManager.getSchema(), sf).getResultSet())
        {
            if (rs.next())
            {
                Map<String, Object> result = new HashMap<>();

                result.put("charge", rs.getInt("charge"));
                result.put("mz", rs.getDouble("Mz"));
                result.put("sequence", rs.getString("Sequence"));
                result.put("label", rs.getString("Name"));

                return result;
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }

        throw new IllegalStateException("Cannot get summary for precursor "+precursorId);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptideGroup(int peptideGroupId)
    {
        return getChromInfosLitePlusForPeptideGroup(peptideGroupId, 0);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptideGroup(int peptideGroupId, int sampleFileId)
    {
        return getPrecursorChromInfoLitePlusList(peptideGroupId, true, false, sampleFileId);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptide(int peptideId)
    {
        return getPrecursorChromInfoLitePlusList(peptideId, false, false, 0);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPrecursor(int precursorId)
    {
        return getPrecursorChromInfoLitePlusList(precursorId, false, true, 0);
    }

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForPeptide(int peptideId, int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.* , pg.Label AS groupName, pep.Sequence, pep.PeptideModifiedSequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel, label.Id AS isotopeLabelId");
        sql.append(" FROM ");
        joinTablesForPrecursorChromInfo(sql);
        sql.append(" WHERE ");
        sql.append("pep.Id=? ");
        sql.add(peptideId);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoPlus.class);
    }

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForPeptideChromInfo(int pepChromInfoId, int precursorId, int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.* , pg.Label AS groupName, pep.Sequence, pep.PeptideModifiedSequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel, label.Id AS isotopeLabelId");
        sql.append(" FROM ");
        joinTablesForPrecursorChromInfo(sql);
        sql.append(" WHERE ");
        sql.append("pci.PeptideChromInfoId=? ");
        sql.add(pepChromInfoId);
        sql.append(" AND ");
        sql.append("pci.PrecursorId=? ");
        sql.add(precursorId);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoPlus.class);
    }

    private static void joinTablesForPrecursorChromInfo(SQLFragment sql)
    {
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(" ON ");
        sql.append("pg.Id = pep.PeptideGroupId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(" ON ");
        sql.append("pep.Id = prec.PeptideId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" ON ");
        sql.append("prec.Id = pci.PrecursorId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label");
        sql.append(" ON ");
        sql.append("prec.IsotopeLabelId = label.Id ");
    }

    private static List<PrecursorChromInfoLitePlus> getPrecursorChromInfoLitePlusList(int id, boolean forPeptideGroup, boolean forPrecursor, int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.id, pci.precursorId, pci.sampleFileId, pci.bestRetentionTime, pci.minStartTime, pci.maxEndTime, pci.TotalArea, pci.maxfwhm, pci.maxHeight");
        sql.append(", pg.Label AS groupName, pep.Sequence, pep.PeptideModifiedSequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel, label.Id AS isotopeLabelId");
        sql.append(" FROM ");
        joinTablesForPrecursorChromInfo(sql);
        sql.append(" WHERE ");
        if(forPeptideGroup)
        {
            sql.append("pg.Id=? ");
        }
        else if(forPrecursor)
        {
            sql.append("prec.Id=? ");
        }
        else
        {
            sql.append("pep.Id=? ");
        }
        sql.add(id);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoLitePlus.class);
    }

    public static List<Precursor> getRepresentativePrecursors(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT prec.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE");
        sql.append(" prec.PeptideId = pep.Id");
        sql.append(" AND");
        sql.append(" pep.PeptideGroupId = pg.Id");
        sql.append(" AND");
        sql.append(" pg.RunId = ?");
        sql.add(runId);
        sql.append(" AND");
        sql.append(" prec.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Representative.ordinal());

        Precursor[] reprPrecursors = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(Precursor.class);
        return Arrays.asList(reprPrecursors);
    }

    public static Precursor getLastDeprecatedPrecursor(Precursor prec, Container container)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT prec.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE");
        sql.append(" prec.PeptideId = pep.Id");
        sql.append(" AND");
        sql.append(" pep.PeptideGroupId = pg.Id");
        sql.append(" AND");
        sql.append(" pg.RunId = run.Id");
        sql.append(" AND");
        sql.append(" run.Container = ?");
        sql.add(container);
        sql.append(" AND");
        sql.append(" prec.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Deprecated.ordinal());
        sql.append(" AND");
        sql.append(" prec.ModifiedSequence = ?");
        sql.add(prec.getModifiedSequence());
        sql.append(" ORDER BY prec.Modified DESC ");

        Precursor[] deprecatedPrecursors = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(Precursor.class);
        if (deprecatedPrecursors.length == 0)
        {
            return null;
        }
        return deprecatedPrecursors[0];
    }

    public static int setRepresentativeState(int runId, RepresentativeDataState state)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        sql.append(" SET RepresentativeDataState = ?");
        sql.add(state.ordinal());
        sql.append(" FROM "+TargetedMSManager.getTableInfoPeptide());
        sql.append(", "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" WHERE ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup()+".Id = "+TargetedMSManager.getTableInfoPeptide()+".PeptideGroupId");
        sql.append(" AND ");
        sql.append(TargetedMSManager.getTableInfoPeptide()+".Id = "+TargetedMSManager.getTableInfoPrecursor()+".PeptideId");
        sql.append(" AND ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup()+".RunId = ?");
        sql.add(runId);
        return new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static void updateRepresentativeStatus(int[] precursorIds, RepresentativeDataState representativeState)
    {
        if(precursorIds == null || precursorIds.length == 0)
            return;

        List<Integer> precursorIdList = new ArrayList<>(precursorIds.length);
        for(int i = 0; i < precursorIds.length; i++)
        {
            precursorIdList.add(precursorIds[i]);
        }
       updateRepresentativeStatus(precursorIdList, representativeState);
    }

    public static void updateRepresentativeStatus(List<Integer> precursorIds, RepresentativeDataState representativeState)
    {
        if(precursorIds == null || precursorIds.size() == 0)
            return;

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        sql.append(" SET RepresentativeDataState = ? ");
        sql.add(representativeState.ordinal());
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPrecursor()+".Id IN (");
        sql.append(StringUtils.join(precursorIds, ","));
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    // Set to either NotRepresentative or Representative_Deprecated.
    // If the original status was Representative it will be updated to Representative_Deprecated.
    // If the original status was Conflicted it will be update to NotRepresentative.
    public static void updateStatusToDeprecatedOrNotRepresentative(int[] precursorIds)
    {
        if(precursorIds == null || precursorIds.length == 0)
            return;

        List<Integer> precursorIdList = new ArrayList<>(precursorIds.length);
        for(int i = 0; i < precursorIds.length; i++)
        {
            precursorIdList.add(precursorIds[i]);
        }
        updateStatusToDeprecatedOrNotRepresentative(precursorIdList);
    }

    public static void updateStatusToDeprecatedOrNotRepresentative(List<Integer> precursorIds)
    {
        if(precursorIds == null || precursorIds.size() == 0)
            return;

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        sql.append(" SET RepresentativeDataState = ");
        sql.append(" CASE WHEN RepresentativeDataState = "+RepresentativeDataState.Conflicted.ordinal());
        sql.append(" THEN "+RepresentativeDataState.NotRepresentative.ordinal());
        sql.append(" ELSE "+RepresentativeDataState.Deprecated.ordinal());
        sql.append(" END");
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPrecursor()+".Id IN (");
        sql.append(StringUtils.join(precursorIds, ","));
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static boolean ensureContainerMembership(int[] precursorIds, Container container)
    {
        if(precursorIds == null || precursorIds.length == 0)
            return false;

        StringBuilder precIds = new StringBuilder();
        Arrays.sort(precursorIds);
        int lastPrecursorId = 0;
        int precursorIdCount = 0;
        for(int id: precursorIds)
        {
            // Ignore duplicates
            if( id != lastPrecursorId)
            {
                precIds.append(",").append(id);
                precursorIdCount++;
            }
            lastPrecursorId = id;
        }
        if(precIds.length() > 0)
            precIds.deleteCharAt(0);
        SQLFragment sql = new SQLFragment("SELECT COUNT(pg.Id) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Container = ? AND pre.Id IN ("+precIds+")");
        sql.add(container.getId());

        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null && count == precursorIdCount;
    }

    public static double getMaxPrecursorIntensity(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(precHeight) FROM (");
        sql.append("SELECT SUM(tci.Height) AS precHeight FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideChromInfo(), "pepci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE");
        sql.append(" pepci.Id = preci.PeptideChromInfoId");
        sql.append(" AND");
        sql.append(" preci.Id = tci.PrecursorChromInfoId");
        sql.append(" AND");
        sql.append(" pepci.PeptideId=?");
        sql.add(peptideId);
        sql.append(" GROUP BY tci.PrecursorChromInfoId");
        sql.append(" ) a");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static boolean hasChromatograms(int precursorId, Integer runId)
    {
        if(runId == null)
        {
            SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
            sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
            sql.append(" WHERE ");
            sql.append("pci.PrecursorId=?");
            sql.add(precursorId);

            Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
            return count != null ? count > 0 : false;
        }
        else
        {
            Set<Integer> precursorIds = _precursorIdsWithChromatograms.get(String.valueOf(runId), null, new CacheLoader<String, Set<Integer>>() {
                @Override
                public Set<Integer> load(String runId, @Nullable Object argument)
                {
                    SQLFragment sql = new SQLFragment("SELECT DISTINCT pci.PrecursorId FROM ");
                    sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
                    sql.append(" , ");
                    sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
                    sql.append(" , ");
                    sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                    sql.append(" , ");
                    sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                    sql.append(" WHERE ");
                    sql.append(" pci.PrecursorId = prec.Id ");
                    sql.append(" AND ");
                    sql.append(" prec.PeptideId = pep.Id ");
                    sql.append(" AND ");
                    sql.append(" pep.PeptideGroupId = pg.Id ");
                    sql.append(" AND ");
                    sql.append("pg.RunId = ?");
                    sql.add(Integer.valueOf(runId));
                    return Collections.unmodifiableSet(new HashSet<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Integer.class)));
                }
            });

            return precursorIds.contains(precursorId);
        }
    }

    public static boolean hasLibrarySpectra(int precursorId, Integer runId)
    {
        if(runId == null)
        {
            SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
            sql.append(TargetedMSManager.getTableInfoPrecursorLibInfo(), "pcilib");
            sql.append(" WHERE ");
            sql.append("pcilib.PrecursorId=?");
            sql.add(precursorId);

            Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
            return count != null ? count > 0 : false;
        }
        else
        {
            Set<Integer> precursorIds = _precursorIdsWithSpectra.get(String.valueOf(runId), null, new CacheLoader<String, Set<Integer>>(){
                @Override
                public Set<Integer> load(String runId, @Nullable Object argument)
                {
                    SQLFragment sql = new SQLFragment("SELECT DISTINCT pcilib.PrecursorId FROM ");
                    sql.append(TargetedMSManager.getTableInfoPrecursorLibInfo(), "pcilib");
                    sql.append(" , ");
                    sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                    sql.append(" WHERE ");
                    sql.append("pcilib.SpectrumLibraryId = specLib.Id");
                    sql.append(" AND ");
                    sql.append("specLib.RunId = ?");
                    sql.add(Integer.valueOf(runId));
                    return Collections.unmodifiableSet(new HashSet<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Integer.class)));
                }
            });

            return precursorIds.contains(precursorId);
        }
    }

    public static void removeRunCachedResults(List<Integer> deletedRunIds)
    {
        for(Integer runId: deletedRunIds)
        {
            _precursorIdsWithChromatograms.remove(String.valueOf(runId));
            _precursorIdsWithSpectra.remove(String.valueOf(runId));
        }
    }

    public static boolean canBeSplitView(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT tra.fragmenttype FROM ");
        sql.append(TargetedMSManager.getTableInfoTransition(), "tra");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(" WHERE tra.precursorid = pre.Id AND  ");
        sql.append("pre.peptideid = "+peptideId);
        String[] sqls = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(String.class);

        if(sqls.length > 1)
        {
            for(String result: sqls)
            {
                if(result.equals("precursor"))
                    return true;
            }
        }
        return false;
    }

    public static boolean hasOptimizationPeaks(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT pci.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");

        sql.append(" WHERE pep.id = pre.PeptideId ");
        sql.append(" AND ");
        sql.append(" pre.Id = pci.PrecursorId ");
        sql.append(" AND ");
        sql.append(" pci.OptimizationStep IS NOT NULL ");
        sql.append(" AND ");
        sql.append(" pep.Id = " + peptideId);
        return new SqlSelector(TargetedMSManager.getSchema(), sql).exists();
    }

    private static class PrecursorIdsWithSpectra extends DatabaseCache<Set<Integer>>
    {
       public PrecursorIdsWithSpectra()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Precursors having library spectra");
        }
    }

    private static class PrecursorIdsWithChromatograms extends DatabaseCache<Set<Integer>>
    {
        public PrecursorIdsWithChromatograms()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Precursors having chromatograms");
        }
    }
}
