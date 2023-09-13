/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.protein.PeptideCharacteristic;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.Peptide;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PeptideManager
{
    private static final int CACHE_SIZE = 10;
    private static final Cache<String, Set<Long>> PEPTIDE_IDS_WITH_SPECTRA = CacheManager.getCache(CACHE_SIZE, CacheManager.DAY, "Peptide IDs with library spectra");

    private PeptideManager() {}

    public static Peptide getPeptide(Container c, long id)
    {
        SQLFragment sql = new SQLFragment("SELECT pep.*, gm.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("gm.PeptideGroupId = pg.Id AND pep.Id = gm.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pep.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Peptide.class);
    }

    public static GeneralMoleculeChromInfo getGeneralMoleculeChromInfo(Container c, long id)
    {
        SQLFragment sql = new SQLFragment("SELECT gmci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE gmci.GeneralMoleculeId = gm.Id AND  ");
        sql.append("gm.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND gmci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(GeneralMoleculeChromInfo.class);
    }

    public static Collection<Peptide> getPeptidesForGroup(long peptideGroupId)
    {
        SQLFragment sql = new SQLFragment("SELECT gm.id, gm.id, gm.peptidegroupid, gm.rtcalculatorscore, gm.predictedretentiontime, ");
        sql.append("gm.avgmeasuredretentiontime, gm.note, gm.explicitretentiontime, ");
        sql.append("gm.normalizationmethod, gm.standardtype, gm.concentrationmultiplier, gm.internalstandardconcentration, ");
        sql.append("p.id, p.sequence, p.startindex, p.endindex, p.previousaa, p.nextaa, ");
        sql.append("p.calcneutralmass, p.nummissedcleavages, p.rank, p.decoy, p.peptidemodifiedsequence, ");
        sql.append("gm.standardtype FROM targetedms.generalmolecule gm, targetedms.peptide p WHERE ");
        sql.append("p.id = gm.id AND gm.peptidegroupid=? ORDER BY gm.Id");
        sql.add(peptideGroupId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Peptide.class);
    }

    private static String appendLog10IntensitySql(SqlDialect sqlDialect)
    {
        var pgLog10Intensity = " CASE WHEN MAX(X.Intensity) IS NOT NULL AND MAX(X.Intensity) != 0 THEN LOG(" + sqlDialect.getNumericCast(new SQLFragment("MAX(X.Intensity)")).getSQL() + ") ELSE 0 END ";
        var sqlServerLog10Intensity = " CASE WHEN MAX(X.Intensity) IS NOT NULL AND MAX(X.Intensity) != 0 THEN LOG10(MAX(X.Intensity)) ELSE 0 END ";
        return sqlDialect.isPostgreSQL() ? pgLog10Intensity : sqlServerLog10Intensity;
    }

    private static String appendLog10ConfidenceSql(SqlDialect sqlDialect)
    {
        var pgConfidenceValueToRound = " CASE WHEN MAX(X.Confidence) IS NOT NULL AND MAX(X.Confidence) != 0 THEN -LOG(" + sqlDialect.getNumericCast(new SQLFragment("MAX(X.Confidence)")).getSQL() + ")  ELSE 0 END ";
        var pgLog10Confidence = "ROUND(" + sqlDialect.getNumericCast(new SQLFragment(pgConfidenceValueToRound)).getSQL() + ",4)";
        var sqlServerLog10Confidence = " CASE WHEN MAX(X.Confidence) IS NOT NULL AND MAX(X.Confidence) != 0 THEN ROUND(-LOG10(MAX(X.Confidence)),4)  ELSE 0 END ";
        return sqlDialect.isPostgreSQL() ? pgLog10Confidence : sqlServerLog10Confidence;
    }

    public static List<PeptideCharacteristic> getCombinedPeptideCharacteristics(long peptideGroupId, @Nullable Long replicateId)
    {
        var shouldFilterAndGroupByReplicate = null != replicateId && !Objects.equals(replicateId, Long.valueOf(0));
        var sqlDialect = TargetedMSManager.getSqlDialect();

        SQLFragment sql = new SQLFragment("SELECT X.Sequence, " + appendLog10IntensitySql(sqlDialect) + " AS Intensity, " + appendLog10ConfidenceSql(sqlDialect) + " AS Confidence, ");
        sql.append(" MAX(X.Intensity) AS RawIntensity, MAX(X.Confidence) AS RawConfidence FROM ");
        sql.append("(SELECT pep.Sequence, ");
        sql.append(" CASE WHEN SUM(TotalArea) IS NULL OR SUM(TotalArea) < 1 THEN 1 ELSE SUM(TotalArea) END AS Intensity, ");
        sql.append(" MAX(qvalue) AS Confidence FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptide(),"pep");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "p");
        sql.append(" ON p.GeneralMoleculeId = pep.Id");
        sql.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoGeneralMolecule(),"gm");
        sql.append(" ON gm.id = pep.Id");
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" ON p.Id = pci.PrecursorId ");
        sql.append(" LEFT OUTER JOIN ").append(TargetedMSManager.getTableInfoSampleFile(), "sf");
        sql.append(" ON sf.Id = pci.SampleFileId");
        if (shouldFilterAndGroupByReplicate)
        {
            sql.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoReplicate(), "rep");
            sql.append(" ON rep.Id = sf.ReplicateId");
        }
        sql.append(" WHERE gm.PeptideGroupId=? ");
        sql.add(peptideGroupId);
        if (shouldFilterAndGroupByReplicate)
        {
            sql.append(" AND rep.Id=? ");
            sql.add(replicateId);
        }
        sql.append(" AND pep.StartIndex IS NOT NULL AND pep.EndIndex IS NOT NULL ");
        sql.append(" GROUP BY pep.Sequence,pci.SampleFileId ) X ");
        sql.append(" GROUP BY X.Sequence ");
        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideCharacteristic.class);
    }

    public static List<PeptideCharacteristic> getModifiedPeptideCharacteristics(long peptideGroupId, @Nullable Long replicateId)
    {
        var shouldFilterAndGroupByReplicate = null != replicateId && !Objects.equals(replicateId, Long.valueOf(0));
        var sqlDialect = TargetedMSManager.getSqlDialect();

        SQLFragment sql = new SQLFragment("SELECT X.Sequence, X.PeptideModifiedSequence AS ModifiedSequence, ");
        sql.append(appendLog10IntensitySql(sqlDialect)).append(" AS Intensity, ").append(appendLog10ConfidenceSql(sqlDialect)).append(" AS Confidence, ");
        sql.append(" MAX(X.Intensity) AS RawIntensity, MAX(X.Confidence) AS RawConfidence FROM ");
        sql.append("(SELECT pep.Sequence, pep.PeptideModifiedSequence, ");
        sql.append(" CASE WHEN SUM(TotalArea) IS NULL OR SUM(TotalArea) < 1 THEN 1 ELSE SUM(TotalArea) END AS Intensity, ");
        sql.append(" MAX(qvalue) AS Confidence FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptide(),"pep");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "p");
        sql.append(" ON p.GeneralMoleculeId = pep.Id");
        sql.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoGeneralMolecule(),"gm");
        sql.append(" ON gm.id = pep.Id");
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" ON p.Id = pci.PrecursorId ");
        sql.append(" LEFT OUTER JOIN ").append(TargetedMSManager.getTableInfoSampleFile(), "sf");
        sql.append(" ON sf.Id = pci.SampleFileId");
        if (shouldFilterAndGroupByReplicate)
        {
            sql.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoReplicate(), "rep");
            sql.append(" ON rep.Id = sf.ReplicateId");
        }
        sql.append(" WHERE gm.PeptideGroupId=? ");
        sql.add(peptideGroupId);
        if (shouldFilterAndGroupByReplicate)
        {
            sql.append(" AND rep.Id=? ");
            sql.add(replicateId);
        }
        sql.append(" AND pep.StartIndex IS NOT NULL AND pep.EndIndex IS NOT NULL ");
        sql.append(" GROUP BY pep.Sequence,pci.SampleFileId, pep.PeptideModifiedSequence ) X ");
        sql.append(" GROUP BY X.Sequence, X.PeptideModifiedSequence ");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideCharacteristic.class);
    }

    public static boolean hasSpectrumLibraryInformation(long peptideId, Long runId)
    {
        if(runId == null)
        {
            SQLFragment sql = new SQLFragment("SELECT gp.Id FROM ");
            sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoBibliospec(), "bib");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
            sql.append(" WHERE ");
            sql.append("pre.Id=bib.PrecursorId");
            sql.append(" AND ");
            sql.append("gp.Id=pre.Id");
            sql.append(" AND ");
            sql.append("gp.GeneralMoleculeId=?");
            sql.add(peptideId);
            sql.append(" UNION ");
            sql.append("SELECT gp.Id FROM ");
            sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoHunterLib(), "hun");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
            sql.append(" WHERE ");
            sql.append("pre.Id=hun.PrecursorId");
            sql.append(" AND ");
            sql.append("gp.Id=pre.Id");
            sql.append(" AND ");
            sql.append("gp.GeneralMoleculeId=?");
            sql.add(peptideId);
            sql.append(" UNION ");
            sql.append("SELECT gp.Id FROM ");
            sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoNistLib(), "nis");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
            sql.append(" WHERE ");
            sql.append("pre.Id=nis.PrecursorId");
            sql.append(" AND ");
            sql.append("gp.Id=pre.Id");
            sql.append(" AND ");
            sql.append("gp.GeneralMoleculeId=?");
            sql.add(peptideId);
            sql.append(" UNION ");
            sql.append("SELECT gp.Id FROM ");
            sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoSpectrastLib(), "sp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
            sql.append(" WHERE ");
            sql.append("pre.Id=sp.PrecursorId");
            sql.append(" AND ");
            sql.append("gp.Id=pre.Id");
            sql.append(" AND ");
            sql.append("gp.GeneralMoleculeId=?");
            sql.add(peptideId);
            sql.append(" UNION ");
            sql.append("SELECT gp.Id FROM ");
            sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoChromatogramLib(), "ch");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
            sql.append(" WHERE ");
            sql.append("pre.Id=ch.PrecursorId");
            sql.append(" AND ");
            sql.append("gp.Id=pre.Id");
            sql.append(" AND ");
            sql.append("gp.GeneralMoleculeId=?");
            sql.add(peptideId);

            return new SqlSelector(TargetedMSManager.getSchema(), sql).exists();
        }
        else
        {
            Set<Long> peptideIds = PEPTIDE_IDS_WITH_SPECTRA.get(String.valueOf(runId), null, (runId1, argument) -> {
                SQLFragment sql = new SQLFragment("SELECT DISTINCT pep.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoBibliospec(), "bib");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "pep");
                sql.append(" WHERE ");
                sql.append(" bib.SpectrumLibraryId = specLib.Id ");
                sql.append(" AND ");
                sql.append(" pre.GeneralMoleculeId = pep.Id ");
                sql.append(" AND ");
                sql.append(" bib.PrecursorId = pre.Id ");
                sql.append(" AND ");
                sql.append(" specLib.RunId = ? ");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT pep.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoHunterLib(), "hun");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "pep");
                sql.append(" WHERE ");
                sql.append(" hun.SpectrumLibraryId = specLib.Id ");
                sql.append(" AND ");
                sql.append(" pre.GeneralMoleculeId = pep.Id ");
                sql.append(" AND ");
                sql.append(" hun.PrecursorId = pre.Id ");
                sql.append(" AND ");
                sql.append(" specLib.RunId = ? ");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT pep.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoNistLib(), "nis");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "pep");
                sql.append(" WHERE ");
                sql.append(" nis.SpectrumLibraryId = specLib.Id ");
                sql.append(" AND ");
                sql.append(" pre.GeneralMoleculeId = pep.Id ");
                sql.append(" AND ");
                sql.append(" nis.PrecursorId = pre.Id ");
                sql.append(" AND ");
                sql.append(" specLib.RunId = ? ");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT pep.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoSpectrastLib(), "sp");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "pep");
                sql.append(" WHERE ");
                sql.append(" sp.SpectrumLibraryId = specLib.Id ");
                sql.append(" AND ");
                sql.append(" pre.GeneralMoleculeId = pep.Id ");
                sql.append(" AND ");
                sql.append(" sp.PrecursorId = pre.Id ");
                sql.append(" AND ");
                sql.append(" specLib.RunId = ? ");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT pep.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoChromatogramLib(), "ch");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "pep");
                sql.append(" WHERE ");
                sql.append(" ch.SpectrumLibraryId = specLib.Id ");
                sql.append(" AND ");
                sql.append(" pre.GeneralMoleculeId = pep.Id ");
                sql.append(" AND ");
                sql.append(" ch.PrecursorId = pre.Id ");
                sql.append(" AND ");
                sql.append(" specLib.RunId = ? ");
                sql.add(Long.valueOf(runId1));

                return Set.copyOf(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Long.class));
            });

            return peptideIds.contains(peptideId);
        }
    }

    public static void removeRunCachedResults(List<Long> deletedRunIds)
    {
        for (Long runId: deletedRunIds)
        {
            PEPTIDE_IDS_WITH_SPECTRA.remove(String.valueOf(runId));
        }
    }
}
