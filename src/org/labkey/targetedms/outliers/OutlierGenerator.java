/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.outliers;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.ColorGenerator;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.GuideSetKey;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.api.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.PeptideOutliers;
import org.labkey.targetedms.model.QCPlotFragment;
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.model.SampleFileQCMetadata;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.MoleculePrecursorManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutlierGenerator
{
    private static final OutlierGenerator INSTANCE = new OutlierGenerator();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");

    private OutlierGenerator() {}

    public static OutlierGenerator get()
    {
        return INSTANCE;
    }

    private String getEachSeriesTypePlotDataSql(int seriesIndex, QCMetricConfiguration configuration, List<AnnotationGroup> annotationGroups)
    {
        String schemaName;
        String queryName;
        if (seriesIndex == 1)
        {
            schemaName = configuration.getSeries1SchemaName();
            queryName = configuration.getSeries1QueryName();
        }
        else
        {
            schemaName = configuration.getSeries2SchemaName();
            queryName = configuration.getSeries2QueryName();
        }
        StringBuilder sql = new StringBuilder();

        // handle trace metrics
        if (configuration.getTraceName() != null)
        {
            sql.append("(SELECT 0 AS PrecursorChromInfoId, SampleFileId, ");
            sql.append(" metric.Name AS SeriesLabel, ");
            sql.append("\nvalue as MetricValue, metric, ").append(seriesIndex).append(" AS MetricSeriesIndex, ").append(configuration.getId()).append(" AS MetricId");
            sql.append("\n FROM ").append(schemaName).append('.').append(TargetedMSManager.getTableQCTraceMetricValues().getName());
            sql.append(" WHERE metric = ").append(configuration.getId());
            sql.append(")");
        }
        else
        {
            sql.append("(SELECT PrecursorChromInfoId, SampleFileId, ");
            sql.append(" CAST(IFDEFINED(SeriesLabel) AS VARCHAR) AS SeriesLabel, ");
            sql.append("\nMetricValue, 0 as metric, ").append(seriesIndex).append(" AS MetricSeriesIndex, ").append(configuration.getId()).append(" AS MetricId");

            sql.append("\n FROM ").append(schemaName).append('.').append(queryName);

            if (!annotationGroups.isEmpty())
            {
                sql.append(" WHERE ");
                StringBuilder filterClause = new StringBuilder("SampleFileId.ReplicateId IN (");
                var intersect = "";
                var selectSql = "(SELECT ReplicateId FROM targetedms.ReplicateAnnotation WHERE ";
                for (AnnotationGroup annotation : annotationGroups)
                {
                    filterClause.append(intersect)
                            .append(selectSql)
                            .append(" Name='")
                            .append(annotation.getName().replace("'", "''"))
                            .append("'");


                    var annotationValues = annotation.getValues();
                    if (!annotationValues.isEmpty())
                    {
                        var quoteEscapedVals = annotationValues.stream().map(s -> s.replace("'", "''")).toList( );
                        var vals = "'" + StringUtils.join(quoteEscapedVals, "','") + "'";
                        filterClause.append(" AND  Value IN (").append(vals).append(" )");
                    }
                    filterClause.append(" ) ");
                    intersect = " INTERSECT ";
                }
                filterClause.append(") ");
                sql.append(filterClause);
            }
            if (configuration.getTraceName() != null)
            {
                sql.append(" WHERE metric = ").append(configuration.getId());
            }
            sql.append(")");
        }
        return sql.toString();
    }

    private Map<String, QCMetricConfiguration> getPreferredMetrics(List<QCMetricConfiguration> configurations, boolean forOutlierSummary)
    {
        Map<String, QCMetricConfiguration> preferredConfigs = new LinkedHashMap<>();
        // Deduplicate for metrics that are shown both standalone and paired with another
        for (QCMetricConfiguration configuration : configurations)
        {
            if (forOutlierSummary)
            {
                if (configuration.getSeries2Label() == null)
                {
                    preferredConfigs.put(configuration.getSeries1Label(), configuration);
                }
            }
            else
            {
                String label1 = configuration.getSeries1Label();
                retainIfPreferred(preferredConfigs, configuration, label1);
                String label2 = configuration.getSeries2Label();
                if (label2 != null)
                {
                    retainIfPreferred(preferredConfigs, configuration, label2);
                }
            }
        }
        return preferredConfigs;
    }

    /** @return LabKey SQL to fetch all the values for the specified metrics */
    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations, Date startDate,
                                                   Date endDate, List<AnnotationGroup> annotationGroups,
                                                   boolean showExcluded, boolean forOutlierSummary)
    {
        // Copy so that we can use our preferred sort
        configurations = new ArrayList<>(configurations);
        // Sort to make sure we have deterministic behavior in a given container
        configurations.sort(Comparator.comparingInt(QCMetricConfiguration::getId));

        Map<String, QCMetricConfiguration> preferredConfigs = getPreferredMetrics(configurations, forOutlierSummary);
        
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT X.* FROM (\n");

        Set<Pair<Integer, Integer>> alreadyAdded = new HashSet<>();

        String sep = "";
        for (QCMetricConfiguration configuration : preferredConfigs.values())
        {
            if (alreadyAdded.add(Pair.of(configuration.getId(), 1)))
            {
                sql.append(sep).append(getEachSeriesTypePlotDataSql(1, configuration, annotationGroups));
            }
            sep = "\nUNION ALL\n";
            if (configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null)
            {
                if (alreadyAdded.add(Pair.of(configuration.getId(), 2)))
                {
                    sql.append(sep).append(getEachSeriesTypePlotDataSql(2, configuration, annotationGroups));
                }
            }
        }

        sql.append(") X");
        sql.append("\nINNER JOIN SampleFile sf ON X.SampleFileId = sf.Id");
        if (null != startDate || null != endDate)
        {
            var sqlSeparator = "WHERE";

            if (null != startDate)
            {
                sql.append("\n").append(sqlSeparator);
                sql.append(" sf.AcquiredTime >= '");
                sql.append(startDate);
                sql.append("' ");
                sqlSeparator = "AND";
            }

            if (null != endDate)
            {
                sql.append("\n").append(sqlSeparator);
                sql.append("\n sf.AcquiredTime < TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('");
                sql.append(endDate);
                sql.append("' AS TIMESTAMP))");
            }
        }
        else
        {
            sql.append("\nWHERE sf.AcquiredTime IS NOT NULL");
        }
        if (!showExcluded)
        {
            sql.append(" AND sf.Excluded = false");
        }

        return sql.toString();
    }

    /** Prefer the standalone variant of a metric if it's also part of a paired config so that we avoid double-counting */
    private void retainIfPreferred(Map<String, QCMetricConfiguration> preferredConfigs, QCMetricConfiguration configuration, String label)
    {
        QCMetricConfiguration existingConfig1 = preferredConfigs.get(label);
        if (existingConfig1 == null || existingConfig1.getSeries2Label() == null)
        {
            preferredConfigs.put(label, configuration);
        }
    }

    public List<RawMetricDataSet> getRawMetricDataSets(TargetedMSSchema schema, List<QCMetricConfiguration> configurations, Date startDate, Date endDate, List<AnnotationGroup> annotationGroups, boolean showExcluded, boolean showExcludedPrecursors, boolean forOutlierSummary)
    {
        List<RawMetricDataSet> result = new ArrayList<>();

        TableInfo sampleFileForQC = schema.getTable("SampleFileForQC");
        List<SampleFileQCMetadata> sfs = new TableSelector(sampleFileForQC).getArrayList(SampleFileQCMetadata.class);

        Map<Long, SampleFileQCMetadata> sampleFiles = new HashMap<>();
        for (SampleFileQCMetadata sf : sfs)
        {
            sampleFiles.put(sf.getId(), sf);
        }

        String labkeySQL = queryContainerSampleFileRawData(configurations, startDate, endDate, annotationGroups, showExcluded, forOutlierSummary);

        // Use strictColumnList = false to avoid a potentially expensive injected join for the Container via lookups
        TableInfo ti = QueryService.get().createTable(schema, labkeySQL, null, true);

        SQLFragment sql = new SQLFragment("SELECT lk.*, pci.PrecursorId ");
        sql.append(" FROM ");
        sql.append(ti, "lk");
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" ON lk.PrecursorChromInfoId = pci.Id ");

        try
        {
            Map<Long, Object> excludedPrecursorIds = new HashMap<>();
            Map<Long, RawMetricDataSet.PrecursorInfo> precursors = loadPrecursors(schema, excludedPrecursorIds, showExcludedPrecursors);

            Map<Integer, QCMetricConfiguration> metrics = new HashMap<>();
            configurations.forEach(m -> metrics.put(m.getId(), m));

            try (ResultSet rs = new SqlSelector(TargetedMSManager.getSchema(), sql).getResultSet(false))
            {
                while (rs.next())
                {
                    long sampleFileId = rs.getLong("SampleFileId");
                    Long precursorId = getLong(rs, "PrecursorId");

                    if (excludedPrecursorIds.containsKey(precursorId))
                        continue;

                    // Sample-scoped metrics won't have an associated precursor
                    RawMetricDataSet.PrecursorInfo precursor = null;
                    if (precursorId != null)
                    {
                        precursor = precursors.get(precursorId);
                        if (precursor == null)
                        {
                            throw new IllegalStateException("Could not find Precursor with Id " + precursorId);
                        }
                    }

                    RawMetricDataSet row = new RawMetricDataSet(sampleFiles.get(sampleFileId), precursor);

                    row.setMetricSeriesIndex(rs.getInt("MetricSeriesIndex"));
                    row.setMetric(metrics.get(rs.getInt("MetricId"))); // this datarow is not setting the correct metric
                    row.setSeriesLabel(rs.getString("SeriesLabel"));
                    row.setPrecursorChromInfoId(getLong(rs, "PrecursorChromInfoId"));
                    row.setMetricValue(getDouble(rs, "MetricValue"));
                    result.add(row);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        result.sort(Comparator.comparing(RawMetricDataSet::getMetricSeriesIndex).
                thenComparing(RawMetricDataSet::getSeriesLabel).
                thenComparing(x -> x.getSampleFile().getAcquiredTime()));

        return result;
    }

    /**
     * Fetch all the precursors in this folder. Loaded separately from the metric values because a given precursor will
     * have many metrics, so for DB query and Java memory use it's more efficient to not flatten them into a single
     * set of results.
     */
    @NotNull
    private Map<Long, RawMetricDataSet.PrecursorInfo> loadPrecursors(TargetedMSSchema schema, Map<Long, Object> excludedPrecursorsIds, boolean showExcludedPrecursors) throws SQLException
    {
        Map<Long, RawMetricDataSet.PrecursorInfo> precursors = new HashMap<>();

        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(4);

        Collection<ExcludedPrecursor> excludedPrecursors = new TableSelector(schema.getTable("ExcludedPrecursors")).getCollection(ExcludedPrecursor.class);

        // First the proteomics side, passing a minimal list of columns to simplify the query and execution plan
        try (ResultSet rs = new TableSelector(schema.getTable(TargetedMSSchema.TABLE_PRECURSOR),
                new LinkedHashSet<>(List.of("Id", "MZ", "Charge", "ModifiedSequence"))).getResultSet(false))
        {
            while (rs.next())
            {
                //skip Excluded peptide precursors
                if (!showExcludedPrecursors && isExcludedPrecursorPeptide(excludedPrecursors,
                        rs.getString("ModifiedSequence"),
                        rs.getInt("Charge"),
                        getDouble(rs, "MZ")))
                {
                    excludedPrecursorsIds.put(rs.getLong("Id"), null);
                    continue;
                }
                RawMetricDataSet.PrecursorInfo p = createPrecursor(format, rs, precursors);
                p.setModifiedSequence(rs.getString("ModifiedSequence"));
            }
        }

        // And now the small molecules, passing a minimal list of columns to simplify the query and execution plan
        try (ResultSet rs = new TableSelector(schema.getTable(TargetedMSSchema.TABLE_MOLECULE_PRECURSOR),
                new LinkedHashSet<>(List.of("Id", "MZ", "Charge", "CustomIonName", "IonFormula", "massMonoisotopic", "massAverage"))).getResultSet(false))
        {
            while (rs.next())
            {
                //skip Excluded molecule precursors
                if (!showExcludedPrecursors && isExcludedPrecursorMolecule(excludedPrecursors,
                        rs.getString("CustomIonName"),
                        rs.getString("IonFormula"),
                        getDouble(rs, "massMonoisotopic"),
                        getDouble(rs, "massAverage"),
                        rs.getInt("Charge"),
                        getDouble(rs, "MZ")))
                {
                    excludedPrecursorsIds.put(rs.getLong("Id"), null);
                    continue;
                }
                RawMetricDataSet.PrecursorInfo p = createPrecursor(format, rs, precursors);
                p.setCustomIonName(rs.getString("CustomIonName"));
                p.setIonFormula(rs.getString("IonFormula"));
                p.setMassMonoisotopic(getDouble(rs, "massMonoisotopic"));
                p.setMassAverage(getDouble(rs, "massAverage"));
            }
        }
        return precursors;
    }

    private boolean isExcludedPrecursorPeptide(Collection<ExcludedPrecursor> excludedPrecursors, String modifiedSeq, int charge, double mz)
    {
        return excludedPrecursors.stream().anyMatch(ep -> Objects.equals(ep.getModifiedSequence(), modifiedSeq) &&
                                                        Objects.equals(ep.getCharge(), charge) &&
                                                        Objects.equals(ep.getMz(), mz));
    }

    private boolean isExcludedPrecursorMolecule(Collection<ExcludedPrecursor> excludedPrecursors, String customIonName,
                                                String ionFormula, double massMonoisotopic, double massAverage, int charge, double mz)
    {
        return excludedPrecursors.stream().anyMatch(ep -> Objects.equals(ep.getCustomIonName(), customIonName) &&
                                                        Objects.equals(ep.getIonFormula(), ionFormula) &&
                                                        Objects.equals(ep.getMassMonoisotopic(), massMonoisotopic) &&
                                                        Objects.equals(ep.getMassAverage(), massAverage) &&
                                                        Objects.equals(ep.getCharge(), charge) &&
                                                        Objects.equals(ep.getMz(), mz));
    }

    @NotNull
    private RawMetricDataSet.PrecursorInfo createPrecursor(DecimalFormat format, ResultSet rs, Map<Long, RawMetricDataSet.PrecursorInfo> precursors) throws SQLException
    {
        RawMetricDataSet.PrecursorInfo p = new RawMetricDataSet.PrecursorInfo(format);
        p.setPrecursorId(rs.getLong("Id"));
        p.setMz(rs.getDouble("MZ"));
        p.setPrecursorCharge(rs.getInt("Charge"));
        precursors.put(p.getPrecursorId(), p);
        return p;
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException
    {
        long result = rs.getLong(columnName);
        return result == 0L && rs.wasNull() ? null : result;
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException
    {
        double result = rs.getDouble(columnName);
        return result == 0.0 && rs.wasNull() ? null : result;
    }

    /**
     * Calculate guide set stats for Levey-Jennings and moving range comparisons.
     * @param guideSets id to GuideSet
     */

    public Map<GuideSetKey, GuideSetStats> getAllProcessedMetricGuideSets(List<RawMetricDataSet> rawMetricData, Map<Integer, GuideSet> guideSets)
    {
        return getAllProcessedMetricGuideSets(rawMetricData, guideSets, null);
    }
    public Map<GuideSetKey, GuideSetStats> getAllProcessedMetricGuideSets(List<RawMetricDataSet> rawMetricData, Map<Integer, GuideSet> guideSets, @Nullable Integer trailingRuns)
    {
        Map<GuideSetKey, GuideSetStats> result = new HashMap<>();

        for (RawMetricDataSet row : rawMetricData)
        {
            GuideSetKey key = row.getGuideSetKey();
            GuideSetStats stats = result.computeIfAbsent(key, x -> new GuideSetStats(key, guideSets.get(key.getGuideSetId())));
            stats.addRow(row);
        }

        result.values().forEach(g -> g.calculateStats(trailingRuns));
        return result;
    }

    /**
     * @param metrics id to QC metric  */
    public List<SampleFileInfo> getSampleFiles(List<RawMetricDataSet> dataRows, Map<GuideSetKey, GuideSetStats> allStats, Map<Integer, QCMetricConfiguration> metrics, TargetedMSSchema schema, Integer limit)
    {
        TableInfo sampleFileForQC = schema.getTable("SampleFileForQC");
        List<SampleFileQCMetadata> sfs = new TableSelector(sampleFileForQC).getArrayList(SampleFileQCMetadata.class);

        List<SampleFileInfo> result = sfs.stream().map(SampleFile::toSampleFileInfo).collect(Collectors.toList());
        Map<Long, SampleFileInfo> sampleFiles = result.stream().collect(Collectors.toMap(SampleFileInfo::getSampleId, Function.identity(), (a, b) -> a));

        for (RawMetricDataSet dataRow : dataRows)
        {
            SampleFileInfo sampleFile = sampleFiles.get(dataRow.getSampleFile().getId());
            GuideSetStats stats = allStats.get(dataRow.getGuideSetKey());

            // If data was deleted after the full metric data was queried, but before we got here, the sample file
            // might not be present anymore. Not a real-world scenario, but turns up when TeamCity is deleting
            // the container at the end of the test run immediately after the crawler has fired a bunch of requests
            if (sampleFile != null)
            {
                dataRow.increment(sampleFile, stats);

                String metricLabel = getMetricLabel(metrics, dataRow);
                dataRow.increment(sampleFile.getMetricCounts(metricLabel, dataRow.getMetric()), stats);
            }
        }

        // Order so most recent are at the top, and limit if requested
        result.sort(Comparator.comparing(SampleFileInfo::getAcquiredTime).reversed());
        if (limit != null && result.size() > limit.intValue())
        {
            result = result.subList(0, limit.intValue());
        }

        return result;
    }

    /** @param metrics id to QC metric */
    public String getMetricLabel(Map<Integer, QCMetricConfiguration> metrics, RawMetricDataSet dataRow)
    {
        QCMetricConfiguration metric = metrics.get(dataRow.getMetricId());
        return switch (dataRow.getMetricSeriesIndex())
                {
                    case 1 -> metric.getSeries1Label();
                    case 2 -> metric.getSeries2Label();
                    default -> throw new IllegalArgumentException("Unexpected metric series index: " + dataRow.getMetricSeriesIndex());
                };
    }
    /**
     * returns the separated plots data per peptide
     * */
    public List<QCPlotFragment> getQCPlotFragment(List<RawMetricDataSet> rawMetricData, Map<GuideSetKey, GuideSetStats> stats, Container c, User u)
    {
        List<QCPlotFragment> qcPlotFragments = new ArrayList<>();
        Map<String, List<RawMetricDataSet>> rawMetricDataSetMapByLabel = new HashMap<>();
        for (RawMetricDataSet rawMetricDataSet : rawMetricData)
        {
            rawMetricDataSetMapByLabel.computeIfAbsent(rawMetricDataSet.getSeriesLabel(), label -> new ArrayList<>());
            rawMetricDataSetMapByLabel.get(rawMetricDataSet.getSeriesLabel()).add(rawMetricDataSet);
        }

        // Track all of the precursors that need to be assigned a color
        Map<Long, QCPlotFragment> fragmentsByPrecursorId = new TreeMap<>();

        for (Map.Entry<String, List<RawMetricDataSet>> entry : rawMetricDataSetMapByLabel.entrySet())
        {
            QCPlotFragment qcPlotFragment = new QCPlotFragment();

            RawMetricDataSet firstValue = entry.getValue().get(0);

            /* Common values for the whole peptide */
            qcPlotFragment.setDataType(firstValue.getDataType());
            qcPlotFragment.setmZ(firstValue.getMz());

            // In case the data has been imported across multiple documents, find the lowest ID value for any of the precursor records
            Optional<RawMetricDataSet> bestPrecursorIdRow = entry.getValue().stream().filter(x -> x.getPrecursorId() != null).min(Comparator.comparing(RawMetricDataSet::getPrecursorId));

            // Remember the precursor ID so that we can assign a series color based on Skyline's algorithm
            bestPrecursorIdRow.ifPresent(rawMetricDataSet -> fragmentsByPrecursorId.put(rawMetricDataSet.getPrecursorId(), qcPlotFragment));

            qcPlotFragment.setSeriesLabel(entry.getKey());
            qcPlotFragment.setQcPlotData(entry.getValue());

            qcPlotFragments.add(qcPlotFragment);

            List<GuideSetStats> guideSetStatsList = new ArrayList<>();
            stats.forEach(((guideSetKey, guideSetStats) -> {
                if (guideSetKey.getSeriesLabel().equalsIgnoreCase(qcPlotFragment.getSeriesLabel()))
                {
                    guideSetStatsList.add(guideSetStats);
                }
            }));
            qcPlotFragment.setGuideSetStats(guideSetStatsList);
        }

        // Now that we have all the precursor IDs, in order (important so that we de-dupe the colors in a stable order),
        // run through them and choose a color
        Set<Color> seriesColors = new HashSet<>();
        for (Map.Entry<Long, QCPlotFragment> entry : fragmentsByPrecursorId.entrySet())
        {
            long precursorId = entry.getKey();
            // It could be either a small molecule or a peptide, so look up both options. There's also a small
            // chance that it's been deleted
            GeneralMolecule molecule = null;
            GeneralPrecursor<?> precursor = PrecursorManager.getPrecursor(c, precursorId, u);
            if (precursor == null)
            {
                precursor = MoleculePrecursorManager.getPrecursor(c, precursorId, u);
                if (precursor != null)
                {
                    molecule = MoleculeManager.getMolecule(c, precursor.getGeneralMoleculeId());
                }
            }
            else
            {
                molecule = PeptideManager.getPeptide(c, precursor.getGeneralMoleculeId());
            }

            if (molecule != null)
            {
                // Choose the color, remembering it so that we can avoid ones that are too similar to each other.

                // We need a separate color per precursor. Use the molecule's text ID, and rely on the similarity comparison
                // to ensure additional precursors for a single molecule get unique colors.
                Color color = ColorGenerator.getColor(molecule.getTextId(), seriesColors);
                entry.getValue().setSeriesColor(color);
                seriesColors.add(color);
            }
        }

        qcPlotFragments.sort(Comparator.comparing(QCPlotFragment::getSeriesLabel));
        return qcPlotFragments;
    }

    public List<PeptideOutliers> getPeptideOutliers(List<RawMetricDataSet> rawMetricData, Map<GuideSetKey, GuideSetStats> stats)
    {
        List<PeptideOutliers> peptideOutliers = new ArrayList<>();
        Map<String, List<RawMetricDataSet>> rawMetricDataSetMapByLabel = new HashMap<>();
        for (RawMetricDataSet rawMetricDataSet : rawMetricData)
        {
            rawMetricDataSetMapByLabel.computeIfAbsent(rawMetricDataSet.getSeriesLabel(), label -> new ArrayList<>());
            rawMetricDataSetMapByLabel.get(rawMetricDataSet.getSeriesLabel()).add(rawMetricDataSet);
        }

        for (Map.Entry<String, List<RawMetricDataSet>> entry : rawMetricDataSetMapByLabel.entrySet())
        {
            int totalOutliers = 0;
            PeptideOutliers peptideOutlier = new PeptideOutliers();
            Map<String, Integer> outlierCountsPerMetric = new HashMap<>();
            peptideOutlier.setPeptide(entry.getKey());
            for (RawMetricDataSet rawMetricDataSet : entry.getValue())
            {
                outlierCountsPerMetric.putIfAbsent(rawMetricDataSet.getMetric().getName(), 0);
                if (rawMetricDataSet.isValueOutlier(stats.get(rawMetricDataSet.getGuideSetKey())))
                {
                    totalOutliers++;
                    outlierCountsPerMetric.put(rawMetricDataSet.getMetric().getName(), outlierCountsPerMetric.get(rawMetricDataSet.getMetric().getName()) + 1);
                }
            }
            peptideOutlier.setPrecursorId(entry.getValue().get(0).getPrecursorChromInfoId());
            peptideOutlier.setOutlierCountsPerMetric(outlierCountsPerMetric);
            peptideOutlier.setTotalOutliers(totalOutliers);
            peptideOutliers.add(peptideOutlier);
        }

        // first sort all the precursor metrics alphabetically and then non-precursor based metrics alphabetically
        peptideOutliers.sort(Comparator.comparingInt((PeptideOutliers o) -> {
                    if (o.getPrecursorId() > 1) return 0;
                    if (o.getPrecursorId() == 0) return 2;
                    return 1;
                })
                .thenComparing(PeptideOutliers::getPeptide));
        return peptideOutliers;
    }

    public static class AnnotationGroup
    {
        private String name;
        private List<String> values;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List<String> getValues()
        {
            return values;
        }

        public void setValues(List<String> values)
        {
            this.values = values;
        }
    }

    public static class ExcludedPrecursor
    {
        int rowId;
        double mz;
        int charge;
        String modifiedSequence;
        String customIonName;
        String ionFormula;
        Double massMonoisotopic;
        Double massAverage;

        public int getRowId()
        {
            return rowId;
        }

        public void setRowId(int rowId)
        {
            this.rowId = rowId;
        }

        public double getMz()
        {
            return mz;
        }

        public void setMz(double mz)
        {
            this.mz = mz;
        }

        public int getCharge()
        {
            return charge;
        }

        public void setCharge(int charge)
        {
            this.charge = charge;
        }

        public String getModifiedSequence()
        {
            return modifiedSequence;
        }

        public void setModifiedSequence(String modifiedSequence)
        {
            this.modifiedSequence = modifiedSequence;
        }

        public String getCustomIonName()
        {
            return customIonName;
        }

        public void setCustomIonName(String customIonName)
        {
            this.customIonName = customIonName;
        }

        public String getIonFormula()
        {
            return ionFormula;
        }

        public void setIonFormula(String ionFormula)
        {
            this.ionFormula = ionFormula;
        }

        public Double getMassMonoisotopic()
        {
            return massMonoisotopic;
        }

        public void setMassMonoisotopic(Double massMonoisotopic)
        {
            this.massMonoisotopic = massMonoisotopic;
        }

        public Double getMassAverage()
        {
            return massAverage;
        }

        public void setMassAverage(Double massAverage)
        {
            this.massAverage = massAverage;
        }
    }
}
