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

import org.json.JSONArray;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.GuideSetKey;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.parser.SampleFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutlierGenerator
{
    private String getEachSeriesTypePlotDataSql(int seriesIndex, int id, String schemaName, String queryName)
    {
        return "(SELECT PrecursorId, PrecursorChromInfoId, SampleFileId, SeriesLabel, DataType, MetricValue, MZ, "
                + "\n " + seriesIndex + " AS MetricSeriesIndex, " + id + " AS MetricId"
                + "\n FROM " + schemaName + '.' + queryName + ")";
    }

    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations)
    {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT X.MetricSeriesIndex, X.MetricId, X.SampleFileId, ");
        sql.append("\nX.PrecursorId, X.PrecursorChromInfoId, X.SeriesLabel, X.DataType, X.MZ, sf.AcquiredTime,");
        sql.append("\nX.MetricValue, gs.RowId AS GuideSetId,");
        sql.append("\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,");
        sql.append("\nCASE WHEN (sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange");
        sql.append("\nFROM (");

        String sep = "";
        for (QCMetricConfiguration configuration: configurations)
        {
            int id = configuration.getId();
            String schema1Name = configuration.getSeries1SchemaName();
            String query1Name = configuration.getSeries1QueryName();
            sql.append(sep).append(getEachSeriesTypePlotDataSql(1, id, schema1Name, query1Name));
            sep = "\nUNION\n";

            if (configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null)
            {
                String schema2Name = configuration.getSeries2SchemaName();
                String query2Name = configuration.getSeries2QueryName();
                sql.append(sep).append(getEachSeriesTypePlotDataSql(2, id, schema2Name, query2Name));
            }
        }

        sql.append(") X");
        sql.append("\nINNER JOIN SampleFile sf ON X.SampleFileId = sf.Id");
        sql.append("\nLEFT JOIN QCMetricExclusion exclusion");
        sql.append("\nON sf.ReplicateId = exclusion.ReplicateId AND (exclusion.MetricId IS NULL OR exclusion.MetricId = x.MetricId)");
        sql.append("\nLEFT JOIN GuideSetForOutliers gs");
        sql.append("\nON ((sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime < gs.ReferenceEnd) OR (sf.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))");

        return sql.toString();
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations)
    {
        String labkeySQL = queryContainerSampleFileRawData(configurations);

        return QueryService.get().selector(
                new TargetedMSSchema(user, container),
                labkeySQL,
                TableSelector.ALL_COLUMNS,
                null,
                new Sort("MetricSeriesIndex,seriesLabel,acquiredTime")).getArrayList(RawMetricDataSet.class);
    }

    /** Calculate guide set stats for Levey-Jennings and moving range comparisons */
    public Map<GuideSetKey, GuideSetStats> getAllProcessedMetricGuideSets(List<RawMetricDataSet> rawMetricData)
    {
        Map<GuideSetKey, List<RawMetricDataSet>> metricGuideSet = new HashMap<>();
        Map<GuideSetKey, GuideSetStats> result = new HashMap<>();

        for (RawMetricDataSet row : rawMetricData)
        {
            List<RawMetricDataSet> rowSubset = metricGuideSet.computeIfAbsent(row.getGuideSetKey(), x -> new ArrayList<>());
            rowSubset.add(row);
        }

        metricGuideSet.forEach((metricType, val) -> result.putAll(getGuideSetAvgMRs(val)));
        return result;
    }


    public Map<GuideSetKey, GuideSetStats> getGuideSetAvgMRs(List<RawMetricDataSet> rawGuideSet)
    {
        Map<GuideSetKey, List<Double>> guideSetDataMap = new LinkedHashMap<>();

        // Bucket the values based on guide set, series, and metric type
        for (RawMetricDataSet row : rawGuideSet)
        {
            GuideSetKey key = row.getGuideSetKey();

            List<Double> metricValues = guideSetDataMap.computeIfAbsent(key, x -> new ArrayList<>());
            if (row.getMetricValue() != null)
            {
                metricValues.add(row.getMetricValue());
            }
        }

        Map<GuideSetKey, GuideSetStats> result = new HashMap<>();
        guideSetDataMap.forEach((key, metricVals) ->
        {
            if (metricVals.size() == 0)
                return;

            Double[] values = metricVals.toArray(new Double[0]);

            GuideSetStats stats = new GuideSetStats(key, values);
            result.put(key, stats);
        });

        return result;
    }

    private static class RowsAndMetricValues
    {
        private final List<RawMetricDataSet> rows = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();

        public void append(RawMetricDataSet row)
        {
            rows.add(row);
            if(row.getMetricValue() == null)
                values.add(0.0d);
            else
                values.add((double) Math.round(row.getMetricValue() * 10000) / 10000.0);
        }
    }

    public void calculateMovingRangeAndCUSUM(List<RawMetricDataSet> plotDataRows)
    {
        Map<GuideSetKey, RowsAndMetricValues> plotDataMap = new LinkedHashMap<>();

        plotDataRows.forEach(row ->
        {
            RowsAndMetricValues values = plotDataMap.computeIfAbsent(row.getGuideSetKey(), x -> new RowsAndMetricValues());
            values.append(row);
        });

        plotDataMap.forEach((plotData, values) ->
        {
            List<Double> metricValsList = values.values;
            Double[] metricVals = metricValsList.toArray(new Double[0]);

            Double[] mRs = Stats.getMovingRanges(metricVals, false, null);

            double[] positiveCUSUMm = Stats.getCUSUMS(metricVals, false, false, false, null);
            double[] negativeCUSUMm = Stats.getCUSUMS(metricVals, true, false, false, null);

            double[] positiveCUSUMv = Stats.getCUSUMS(metricVals, false, true, false, null);
            double[] negativeCUSUMv = Stats.getCUSUMS(metricVals, true, true, false, null);

            List<RawMetricDataSet> serTypeObjList = values.rows;

            for (int i = 0; i < serTypeObjList.size(); i++)
            {
                RawMetricDataSet row = serTypeObjList.get(i);
                row.setmR(mRs[i]);
                row.setCUSUMmP(positiveCUSUMm[i]);
                row.setCUSUMmN(negativeCUSUMm[i]);
                row.setCUSUMvP(positiveCUSUMv[i]);
                row.setCUSUMvN(negativeCUSUMv[i]);
            }
        });
    }

    public List<SampleFileInfo> getSampleFiles(List<RawMetricDataSet> dataRows, Map<GuideSetKey, GuideSetStats> stats, Map<Integer, QCMetricConfiguration> metrics, Container container)
    {
        List<SampleFileInfo> result = TargetedMSManager.getSampleFiles(container, null).stream().map(SampleFile::toSampleFileInfo).collect(Collectors.toList());
        Map<Integer, SampleFileInfo> sampleFiles = result.stream().collect(Collectors.toMap(SampleFileInfo::getSampleId, Function.identity()));

        for (RawMetricDataSet dataRow : dataRows)
        {
            SampleFileInfo info = sampleFiles.get(dataRow.getSampleFileId());
            dataRow.increment(info, stats.get(dataRow.getGuideSetKey()));

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

            dataRow.increment(info.getMetricCounts(metricLabel), stats.get(dataRow.getGuideSetKey()));
        }
        return result;
    }

    /** Subset the rows to just those that are part of a guide set */
    public List<RawMetricDataSet> filterToTrainingData(List<RawMetricDataSet> rawRows, List<GuideSet> guideSets)
    {
        List<RawMetricDataSet> result = new ArrayList<>();
        for (RawMetricDataSet raw : rawRows)
        {
            for (GuideSet guideSet : guideSets)
            {
                if (!raw.isIgnoreInQC() &&
                        guideSet.getTrainingStart().compareTo(raw.getAcquiredTime()) <= 0 &&
                        (guideSet.getTrainingEnd() == null || guideSet.getTrainingEnd().compareTo(raw.getAcquiredTime()) >= 0))
                {
                    result.add(raw);
                    break;
                }
            }
        }
        return result;
    }

    public JSONArray getSampleFilesJSON(List<SampleFileInfo> files, Integer limit)
    {
        JSONArray result = new JSONArray();

        files.sort(Comparator.comparing(SampleFileInfo::getAcquiredTime).reversed());

        if (limit != null && files.size() > limit.intValue())
        {
            files = files.subList(0, limit.intValue() - 1);
        }

        files.forEach(sample -> result.put(sample.toJSON()));

        return result;
    }
}
