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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.GuideSetKey;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.QCPlotFragment;
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.parser.SampleFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutlierGenerator
{
    private static final OutlierGenerator INSTANCE = new OutlierGenerator();

    private OutlierGenerator() {}

    public static OutlierGenerator get()
    {
        return INSTANCE;
    }

    private String getEachSeriesTypePlotDataSql(int seriesIndex, int id, String schemaName, String queryName, List<AnnotationGroup> annotationGroups)
    {
        StringBuilder sql = new StringBuilder("(SELECT PrecursorChromInfoId, SampleFileId, SampleFileId.FilePath, SampleFileId.ReplicateId.Id AS ReplicateId ,");
        sql.append(" CAST(IFDEFINED(SeriesLabel) AS VARCHAR) AS SeriesLabel, ");
        sql.append("\nMetricValue, ").append(seriesIndex).append(" AS MetricSeriesIndex, ").append(id).append(" AS MetricId");
        sql.append("\n FROM ").append(schemaName).append('.').append(queryName);
        if (!annotationGroups.isEmpty())
        {
            sql.append(" WHERE ");
            StringBuilder filterClause = new StringBuilder("SampleFileId.ReplicateId in (");
            var intersect = "";
            var selectSql = "(SELECT ReplicateId FROM targetedms.ReplicateAnnotation WHERE ";
            for (AnnotationGroup annotation : annotationGroups)
            {
                filterClause.append(intersect).append(selectSql).append(" Name='").append(annotation.getName()).append("' AND ( ");
                var or = "";
                for (String value : annotation.getValues())
                {
                    filterClause.append(or).append("Value='").append(value).append("'");
                    or = " OR ";
                }
                filterClause.append(" ) ) ");
                intersect = " INTERSECT ";
            }
            filterClause.append(") ");
            sql.append(filterClause.toString());
        }
        sql.append(")");
        return sql.toString();
    }

    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations, String startDate, String endDate, List<AnnotationGroup> annotationGroups)
    {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT X.MetricSeriesIndex, X.MetricId, X.SampleFileId, ");

        sql.append(" X.FilePath, X.ReplicateId, ");

        sql.append("\nCOALESCE(pci.PrecursorId.Id, pci.MoleculePrecursorId.Id) AS PrecursorId,");

        sql.append("\nCOALESCE(X.SeriesLabel, COALESCE(pci.PrecursorId.ModifiedSequence,");
        sql.append("\n           ((CASE WHEN pci.MoleculePrecursorId.CustomIonName IS NULL THEN '' ELSE (pci.MoleculePrecursorId.CustomIonName || ', ') END)");
        sql.append("\n            || (CASE WHEN pci.MoleculePrecursorId.IonFormula IS NULL THEN '' ELSE (pci.MoleculePrecursorId.IonFormula || ', ') END)");
        sql.append("\n            || ('[' || CAST (ROUND(pci.MoleculePrecursorId.massMonoisotopic, 4) AS VARCHAR) || '/'");
        sql.append("\n            || CAST (ROUND(pci.MoleculePrecursorId.massAverage, 4) AS VARCHAR) || '] ')");
        sql.append("\n            ))");
        sql.append("\n    || CAST (ROUND(COALESCE (pci.PrecursorId.Mz, pci.MoleculePrecursorId.Mz), 4) AS VARCHAR)");
        sql.append("\n    || (CASE WHEN COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) > 0 THEN ' +' ELSE ' ' END)");
        sql.append("\n    || CAST(COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) AS VARCHAR)) AS SeriesLabel,");

        sql.append("\nCASE WHEN pci.PrecursorId.Id IS NOT NULL THEN 'Peptide' WHEN pci.MoleculePrecursorId.Id IS NOT NULL THEN 'Fragment' ELSE 'Other' END AS DataType,");
        sql.append("\nCOALESCE(pci.PrecursorId.Mz, pci.MoleculePrecursorId.Mz) AS MZ,");

        sql.append("\nX.PrecursorChromInfoId, sf.AcquiredTime, X.MetricValue, COALESCE(gs.RowId, 0) AS GuideSetId,");
        sql.append("\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,");
        sql.append("\nCASE WHEN (sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange");
        sql.append("\nFROM (");

        String sep = "";
        for (QCMetricConfiguration configuration : configurations)
        {
            int id = configuration.getId();
            String schema1Name = configuration.getSeries1SchemaName();
            String query1Name = configuration.getSeries1QueryName();
            sql.append(sep).append(getEachSeriesTypePlotDataSql(1, id, schema1Name, query1Name, annotationGroups));
            sep = "\nUNION\n";

            if (configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null)
            {
                String schema2Name = configuration.getSeries2SchemaName();
                String query2Name = configuration.getSeries2QueryName();
                sql.append(sep).append(getEachSeriesTypePlotDataSql(2, id, schema2Name, query2Name, annotationGroups));
            }
        }

        sql.append(") X");
        sql.append("\nINNER JOIN SampleFile sf ON X.SampleFileId = sf.Id");
        sql.append("\nLEFT JOIN PrecursorChromInfo pci ON pci.Id = X.PrecursorChromInfoId");
        sql.append("\nLEFT JOIN QCMetricExclusion exclusion");
        sql.append("\nON sf.ReplicateId = exclusion.ReplicateId AND (exclusion.MetricId IS NULL OR exclusion.MetricId = x.MetricId)");
        sql.append("\nLEFT JOIN GuideSetForOutliers gs");
        sql.append("\nON ((sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime < gs.ReferenceEnd) OR (sf.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))");
        if (null != startDate && null != endDate)
        {
            sql.append("\nWHERE sf.AcquiredTime >= '");
            sql.append(startDate);
            sql.append("' AND ");
            sql.append("\n sf.AcquiredTime < TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('");
            sql.append(endDate);
            sql.append("' AS TIMESTAMP))");
        }
        else
        {
            sql.append("\nWHERE sf.AcquiredTime IS NOT NULL");
        }

        return sql.toString();
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations, @Nullable String startDate, @Nullable String endDate, List<AnnotationGroup> annotationGroups)
    {
        String labkeySQL = queryContainerSampleFileRawData(configurations, startDate, endDate, annotationGroups);

        return QueryService.get().selector(
                new TargetedMSSchema(user, container),
                labkeySQL,
                TableSelector.ALL_COLUMNS,
                null,
                new Sort("MetricSeriesIndex,seriesLabel,acquiredTime")).getArrayList(RawMetricDataSet.class);
    }

    /**
     * Calculate guide set stats for Levey-Jennings and moving range comparisons.
     * @param guideSets id to GuideSet
     */
    public Map<GuideSetKey, GuideSetStats> getAllProcessedMetricGuideSets(List<RawMetricDataSet> rawMetricData, Map<Integer, GuideSet> guideSets)
    {
        Map<GuideSetKey, GuideSetStats> result = new HashMap<>();

        for (RawMetricDataSet row : rawMetricData)
        {
            GuideSetKey key = row.getGuideSetKey();
            GuideSetStats stats = result.computeIfAbsent(row.getGuideSetKey(), x -> new GuideSetStats(key, guideSets.get(key.getGuideSetId())));
            stats.addRow(row);
        }

        result.values().forEach(GuideSetStats::calculateStats);
        return result;
    }

    /**
     * @param metrics id to QC metric  */
    public List<SampleFileInfo> getSampleFiles(List<RawMetricDataSet> dataRows, Map<GuideSetKey, GuideSetStats> allStats, Map<Integer, QCMetricConfiguration> metrics, Container container, Integer limit)
    {
        List<SampleFileInfo> result = TargetedMSManager.getSampleFiles(container, new SQLFragment("sf.AcquiredTime IS NOT NULL")).stream().map(SampleFile::toSampleFileInfo).collect(Collectors.toList());
        Map<Integer, SampleFileInfo> sampleFiles = result.stream().collect(Collectors.toMap(SampleFileInfo::getSampleId, Function.identity()));

        for (RawMetricDataSet dataRow : dataRows)
        {
            SampleFileInfo sampleFile = sampleFiles.get(dataRow.getSampleFileId());
            GuideSetStats stats = allStats.get(dataRow.getGuideSetKey());

            // If data was deleted after the full metric data was queried, but before we got here, the sample file
            // might not be present anymore. Not a real-world scenario, but turns up when TeamCity is deleting
            // the container at the end of the test run immediately after the crawler has fired a bunch of requests
            if (sampleFile != null)
            {
                dataRow.increment(sampleFile, stats);

                String metricLabel = getMetricLabel(metrics, dataRow);
                dataRow.increment(sampleFile.getMetricCounts(metricLabel), stats);
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
        String metricLabel;
        switch (dataRow.getMetricSeriesIndex())
        {
            case 1:
                metricLabel = metric.getSeries1Label();
                break;
            case 2:
                metricLabel = metric.getSeries2Label();
                break;
            default:
                throw new IllegalArgumentException("Unexpected metric series index: " + dataRow.getMetricSeriesIndex());
        }
        return metricLabel;
    }
    /**
     * returns the separated plots data per peptide
     * */
    public List<QCPlotFragment> getQCPlotFragment(List<RawMetricDataSet> rawMetricData, Map<GuideSetKey, GuideSetStats> stats)
    {
        List<QCPlotFragment> qcPlotFragments = new ArrayList<>();
        Map<String, List<RawMetricDataSet>> rawMetricDataSetMapByLabel = new HashMap<>();
        for (RawMetricDataSet rawMetricDataSet : rawMetricData)
        {
            if (null == rawMetricDataSetMapByLabel.get(rawMetricDataSet.getSeriesLabel()))
            {
                List<RawMetricDataSet> rawMetricDataSets = new ArrayList<>();
                rawMetricDataSets.add(rawMetricDataSet);
                rawMetricDataSetMapByLabel.put(rawMetricDataSet.getSeriesLabel(), rawMetricDataSets);
            }
            else
            {
                rawMetricDataSetMapByLabel.get(rawMetricDataSet.getSeriesLabel()).add(rawMetricDataSet);
            }
        }

        for (Map.Entry<String, List<RawMetricDataSet>> entry : rawMetricDataSetMapByLabel.entrySet())
        {
            QCPlotFragment qcPlotFragment = new QCPlotFragment();
            qcPlotFragment.setSeriesLabel(entry.getKey());

            /* Common values for the whole peptide */
            qcPlotFragment.setDataType(entry.getValue().get(0).getDataType());
            qcPlotFragment.setIgnoreInQC(entry.getValue().get(0).isIgnoreInQC());
            qcPlotFragment.setmZ(entry.getValue().get(0).getMz());
            qcPlotFragment.setPrecursorId(entry.getValue().get(0).getPrecursorId());
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

        return qcPlotFragments;
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
}
