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

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.security.User;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.api.targetedms.model.LJOutlier;
import org.labkey.targetedms.model.RawMetricDataSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class CUSUMOutliers extends  Outliers
{

    private String getEachSeriesTypePlotDataSql(String type, int id, String schemaName, String queryName, String whereClause, String metricType)
    {
        return "(SELECT '" + type + "' AS SeriesType, X.SampleFile, " +
                "(SELECT " + metricType + " FROM qcmetricconfiguration where id = " + id + ")" + " AS MetricType, " +
                "\nX.PrecursorId, X.PrecursorChromInfoId, X.SeriesLabel, X.DataType, X.mz, X.AcquiredTime,"
                + "\nX.FilePath, X.MetricValue, x.ReplicateId, gs.RowId AS GuideSetId,"
                + "\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,"
                + "\nCASE WHEN (X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange"
                + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.FilePath AS FilePath,"
                + "\n      SampleFileId.SampleName AS SampleFile, SampleFileId.ReplicateId AS ReplicateId"
                + "\n      FROM " + schemaName + '.' + queryName + whereClause + ") X "
                + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + id + ") exclusion"
                + "\nON X.ReplicateId = exclusion.ReplicateId"
                + "\nLEFT JOIN GuideSetForOutliers gs"
                + "\nON ((X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime < gs.ReferenceEnd) OR (X.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))"
                + "\nORDER BY X.SeriesLabel, SeriesType, X.AcquiredTime)";
    }

    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        String sep = "";
        String where ="";

        for (QCMetricConfiguration configuration: configurations)
        {
            int id = configuration.getId();
            String schema1Name = configuration.getSeries1SchemaName();
            String query1Name = configuration.getSeries1QueryName();
            sqlBuilder.append(sep).append("(").append(getEachSeriesTypePlotDataSql("series1", id, schema1Name, query1Name, where, "Series1Label")).append(")");
            sep = "\nUNION\n";

            if(configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null) {
                String schema2Name = configuration.getSeries2SchemaName();
                String query2Name = configuration.getSeries2QueryName();
                sqlBuilder.append(sep).append("(").append(getEachSeriesTypePlotDataSql("series2", id, schema2Name, query2Name, where, "Series2Label")).append(")");
            }
        }
        return "SELECT * FROM (" + sqlBuilder.toString() + ") a"; //wrap unioned results in sql to support sorting
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations)
    {
        Set<String> columnNames = Set.of("seriesType","sampleFile","metricType","precursorId","precursorChromInfoId","seriesLabel",
                "dataType","mz","acquiredTime","filePath","metricValue","replicateId","guideSetId","ignoreInQC","inGuideSetTrainingRange");

        return executeQuery(container, user, queryContainerSampleFileRawData(configurations), columnNames, new Sort("seriesType,seriesLabel,acquiredTime")).getArrayList(RawMetricDataSet.class);
    }

    /** @return metric type -> calculated values per precursor/guide set combo */
    private Map<String, Map<GuideSetStats.Key, GuideSetStats>> getAllProcessedMetricGuideSets(List<RawMetricDataSet> rawMetricData)
    {
        Map<String, List<RawMetricDataSet>> metricGuideSet = new TreeMap<>();
        Map<String, Map<GuideSetStats.Key, GuideSetStats>> processedMetricGuides = new TreeMap<>();
        MovingRangeOutliers mr = new MovingRangeOutliers();

        for (RawMetricDataSet row : rawMetricData)
        {
            String metricType = row.getMetricType();
            if(metricGuideSet.get(metricType) == null) {
                List<RawMetricDataSet> sets = new ArrayList<>();
                rawMetricData.forEach(gs->{
                    if(gs.getMetricType() != null && gs.getMetricType().equals(metricType))
                        sets.add(gs);
                });
                metricGuideSet.put(row.getMetricType(), sets);
            }
        }

        metricGuideSet.forEach((metricType, val) -> processedMetricGuides.put(metricType, mr.getGuideSetAvgMRs(val)));
        return processedMetricGuides;
    }

    private static class PlotData
    {
        String seriesLabel;
        String series;
        String seriesType;

        public String getSeriesLabel()
        {
            return seriesLabel;
        }

        public void setSeriesLabel(String seriesLabel)
        {
            this.seriesLabel = seriesLabel;
        }

        public void setSeries(String series)
        {
            this.series = series;
        }

        public String getSeriesType()
        {
            return seriesType;
        }

        public void setSeriesType(String seriesType)
        {
            this.seriesType = seriesType;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlotData plotData = (PlotData) o;
            return seriesLabel.equals(plotData.seriesLabel) &&
                    series.equals(plotData.series) &&
                    seriesType.equals(plotData.seriesType);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(seriesLabel, series, seriesType);
        }
    }

    private static class RowsAndMetricValues
    {
        private final List<RawMetricDataSet> rows;
        private final List<Double> values;

        public RowsAndMetricValues(List<RawMetricDataSet> rows, List<Double> values)
        {
            this.rows = rows;
            this.values = values;
        }
    }

    private Map<PlotData, RowsAndMetricValues> preprocessPlotData(List<RawMetricDataSet> plotDataRows)
    {
        Map<PlotData, RowsAndMetricValues> plotDataMap = new LinkedHashMap<>();

        if (plotDataRows.size() > 0)
        {
            plotDataRows.forEach(row-> {
                PlotData plotData = new PlotData();
                plotData.setSeriesLabel(row.getSeriesLabel());
                plotData.setSeries("Series");
                plotData.setSeriesType(row.getSeriesType());

                if (plotDataMap.get(plotData) == null)
                {
                    List<RawMetricDataSet> rowsList = new ArrayList<>();
                    List<Double> metricValuesList = new ArrayList<>();

                    plotDataRows.forEach(pR ->{
                        if(pR.getSeriesLabel().equalsIgnoreCase(plotData.getSeriesLabel()) &&
                            pR.getSeriesType().equalsIgnoreCase(plotData.getSeriesType()))
                        {
                            rowsList.add(pR);
                            if(pR.getMetricValue() == null)
                                metricValuesList.add(0.0d);
                            else
                               metricValuesList.add((double) Math.round(pR.getMetricValue() * 10000) / 10000.0);

                        }
                    });

                    plotDataMap.put(plotData, new RowsAndMetricValues(rowsList, metricValuesList));
                }
            });

            plotDataMap.forEach((plotData, values) ->  {
                List<Double> metricValsList = values.values;
                Double[] metricVals = metricValsList.toArray(new Double[0]);

                Double[] mRs = Stats.getMovingRanges(metricVals, false, null);

                double[] positiveCUSUMm = Stats.getCUSUMS(metricVals, false, false, false, null);
                double[] negativeCUSUMm = Stats.getCUSUMS(metricVals, true, false, false, null);

                double[] positiveCUSUMv = Stats.getCUSUMS(metricVals, false, true, false, null);
                double[] negativeCUSUMv = Stats.getCUSUMS(metricVals, true, true, false, null);

                List<RawMetricDataSet> serTypeObjList = values.rows;
                if (serTypeObjList.size() == positiveCUSUMm.length)
                {
                    for (int i = 0; i < serTypeObjList.size(); i++)
                    {
                        RawMetricDataSet row = serTypeObjList.get(i);
                        row.setmR(mRs[i]);
                        row.setcUSUMmP(positiveCUSUMm[i]);
                        row.setcUSUMmN(negativeCUSUMm[i]);
                        row.setCUSUMvP(positiveCUSUMv[i]);
                        row.setCUSUMvN(negativeCUSUMv[i]);
                    }
                }
            });
        }

        return plotDataMap;
    }

    private Map<String, Map<Integer, Map<PlotData, RowsAndMetricValues>>> getAllProcessedMetricDataSets(List<RawMetricDataSet> rawMetricDataSets)
    {
        Map<String, Map<Integer, List<RawMetricDataSet>>> metricDataSet = new LinkedHashMap<>();
        rawMetricDataSets.forEach(row-> {
            if (metricDataSet.get(row.getMetricType()) == null)
            {
                Map<Integer, List<RawMetricDataSet>> metTypeMap = new LinkedHashMap<>();
                metricDataSet.put(row.getMetricType(), metTypeMap);
            }

            if (metricDataSet.get(row.getMetricType()).get(row.getGuideSetId()) == null)
            {
                List<RawMetricDataSet> setList = new ArrayList<>();

                String metricType = row.getMetricType();
                Integer guideId = row.getGuideSetId();
                rawMetricDataSets.forEach(ds-> {
                    if(ds.getMetricType().equalsIgnoreCase(metricType) && guideId.equals(ds.getGuideSetId()))
                        setList.add(ds);
                });
                metricDataSet.get(row.getMetricType()).put(row.getGuideSetId(), setList);
            }
        });

        Map<String, Map<Integer, Map<PlotData, RowsAndMetricValues>>> processedMetricDataSet = new LinkedHashMap<>();
        metricDataSet.forEach((metric, guides) -> {
            if (processedMetricDataSet.get(metric) == null)
            {
                Map<Integer, Map<PlotData, RowsAndMetricValues>> metricMap = new LinkedHashMap<>();
                processedMetricDataSet.put(metric, metricMap);
            }
            guides.forEach((guideId, guideset) -> processedMetricDataSet.get(metric).put(guideId, preprocessPlotData(guideset)));
        });

        return processedMetricDataSet;
    }

    private static class PlotOutlier
    {
        int totalCount;
        Map<String, Map<String, Integer>> outliers;

        public int getTotalCount()
        {
            return totalCount;
        }

        public void setTotalCount(int totalCount)
        {
            this.totalCount = totalCount;
        }

        public Map<String, Map<String, Integer>> getOutliers()
        {
            return outliers;
        }

        public void setOutliers(Map<String, Map<String, Integer>> outliers)
        {
            this.outliers = outliers;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlotOutlier that = (PlotOutlier) o;
            return totalCount == that.totalCount &&
                    outliers.equals(that.outliers);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(totalCount, outliers);
        }
    }

    private Map<String, PlotOutlier> getQCPlotMetricOutliers(Map<String, Map<GuideSetStats.Key, GuideSetStats>> processedMetricGuides, Map<String, Map<Integer, Map<PlotData, RowsAndMetricValues>>> processedMetricDataSet, Set<String> sampleFiles)
    {
        Map<String, PlotOutlier> plotOutliers = new LinkedHashMap<>();
        processedMetricDataSet.forEach((metric, metricVal) -> {
            Map<String, Integer> countCUSUMmN = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMmP = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMvP = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMvN = new LinkedHashMap<>();
            Map<String, Integer> countMR = new LinkedHashMap<>();


            Map<String, Map<String, Integer>> outliersByTypeAndSampleFile = new HashMap<>();

            outliersByTypeAndSampleFile.put("CUSUMmN", countCUSUMmN);
            outliersByTypeAndSampleFile.put("CUSUMmP", countCUSUMmP);
            outliersByTypeAndSampleFile.put("CUSUMvP", countCUSUMvP);
            outliersByTypeAndSampleFile.put("CUSUMvN", countCUSUMvN);
            outliersByTypeAndSampleFile.put("mR", countMR);
            PlotOutlier plotOutlier = new PlotOutlier();

            metricVal.forEach((guideSetId, peptides) -> {

                int totalCount = peptides.keySet().size();

                plotOutlier.setTotalCount(totalCount);

                peptides.forEach((plotData, plotDataList) -> {
                    if (plotDataList == null)
                        return;

                    List<RawMetricDataSet> rows = plotDataList.rows;

                    rows.forEach(data -> {
                        String sampleFileString = SampleFileInfo.getKey(data.getSampleFile(), data.getAcquiredTime());
                        if (data.getcUSUMmN() != null && data.getcUSUMmN() > Stats.CUSUM_CONTROL_LIMIT)
                        {
                            processEachOutlier(countCUSUMmN, sampleFiles, sampleFileString);
                        }
                        if (data.getcUSUMmP() != null && data.getcUSUMmP() > Stats.CUSUM_CONTROL_LIMIT)
                        {
                            processEachOutlier(countCUSUMmP, sampleFiles, sampleFileString);
                        }
                        if (data.getCUSUMvN() != null && data.getCUSUMvN() > Stats.CUSUM_CONTROL_LIMIT)
                        {
                            processEachOutlier(countCUSUMvN, sampleFiles, sampleFileString);
                        }
                        if (data.getCUSUMvP() != null && data.getCUSUMvP() > Stats.CUSUM_CONTROL_LIMIT)
                        {
                            processEachOutlier(countCUSUMvP, sampleFiles, sampleFileString);
                        }
                        if (processedMetricGuides.get(metric) != null)
                        {
                            Map<GuideSetStats.Key, GuideSetStats> averages = processedMetricGuides.get(metric);
                            GuideSetStats stats = averages.get(new GuideSetStats.Key(guideSetId, plotData.getSeriesLabel(), plotData.getSeriesType()));
                            if (stats != null)
                            {
                                if (data.getmR() != null && data.getmR() > Stats.MOVING_RANGE_UPPER_LIMIT_WEIGHT * stats.getMovingRangeAverage())
                                {
                                    processEachOutlier(countMR, sampleFiles, sampleFileString);
                                }
                            }
                        }
                    });
                });
            });
            plotOutlier.setOutliers(outliersByTypeAndSampleFile);

            plotOutliers.put(metric, plotOutlier);

        });

        return plotOutliers;
    }


    /**
     * @param countObj count of the out of range metric - cusum or moving range.
     * @param sampleFiles Set of uploaded sample files.
     * @param sampleFileString unique string to identify each sample file
     */
    private void processEachOutlier(Map<String, Integer> countObj, Set<String> sampleFiles, String sampleFileString)
    {
        if (sampleFiles.contains(sampleFileString))
        {
            int count = countObj.get(sampleFileString) != null ? countObj.get(sampleFileString) : 0;
            countObj.put(sampleFileString, ++count);
        }
    }

    private Map<String, Map<String, Map<String, Integer>>> getMetricOutliersByFileOrGuideSetGroup(Map<String, PlotOutlier> metricOutlier)
    {
        Map<String, Map<String, Map<String, Integer>>> transformedOutliers = new LinkedHashMap<>();
        metricOutlier.forEach((metric, vals) -> {
            int totalCount = vals.getTotalCount();
            Map<String, Map<String, Integer>> outliers = vals.getOutliers();

            outliers.forEach((type, groups) -> {

                if (groups.size() > 0) {
                    groups.forEach((group, count) -> {
                        if(transformedOutliers.get(group) == null)
                        {
                            Map<String, Map<String, Integer>> groupMap = new LinkedHashMap<>();
                            transformedOutliers.put(group, groupMap);
                        }
                        Map<String, Integer> metricMap = new LinkedHashMap<>();

                        outliers.forEach((t, g) -> {
                            g.forEach((gp, ct) -> {

                                if(group.equalsIgnoreCase(gp))
                                {
                                    metricMap.put("TotalCount", totalCount);
                                    metricMap.put(t, ct);
                                }
                            });
                        });

                        transformedOutliers.get(group).put(metric, metricMap);
                    });
                }
            });
        });
        return transformedOutliers;
    }

    public Map<String, SampleFileInfo> getSampleFiles(List<LJOutlier> ljOutliers, List<RawMetricDataSet> rawMetricDataSets, List<GuideSet> guideSets)
    {
        Map<String, SampleFileInfo> sampleFiles = setSampleFiles(ljOutliers);

        List<RawMetricDataSet> trainingData = filterToTrainingData(rawMetricDataSets, guideSets);
        Map<String, Map<GuideSetStats.Key, GuideSetStats>> processedMetricGuides = getAllProcessedMetricGuideSets(trainingData);

        List<LJOutlier> outliers2 = new ArrayList<>();

        for (RawMetricDataSet rawMetricDataSet : rawMetricDataSets)
        {
            Map<GuideSetStats.Key, GuideSetStats> allStats = processedMetricGuides.get(rawMetricDataSet.getMetricType());
            GuideSetStats stat = allStats.get(new GuideSetStats.Key(rawMetricDataSet.getGuideSetId(), rawMetricDataSet.getSeriesLabel(), rawMetricDataSet.getSeriesType()));

            if (stat != null)
            {
                double upperLimit = stat.getAverage() + stat.getStandardDeviation() * 3;
                double lowerLimit = stat.getAverage() - stat.getStandardDeviation() * 3;

                if (!rawMetricDataSet.isIgnoreInQC() && rawMetricDataSet.getMetricValue() != null &&
                        (rawMetricDataSet.getMetricValue().doubleValue() > upperLimit || rawMetricDataSet.getMetricValue().doubleValue() < lowerLimit))
                {
                    LJOutlier ljOutlier = new LJOutlier();
                    ljOutlier.setAcquiredTime(rawMetricDataSet.getAcquiredTime());
                    ljOutlier.setGuideSetId(rawMetricDataSet.getGuideSetId());
                    ljOutlier.setIgnoreInQC(rawMetricDataSet.isIgnoreInQC());
                    ljOutlier.setSampleFile(rawMetricDataSet.getSampleFile());
                    //                ljOutlier.setMetricId();
                    ljOutlier.setMetricName(rawMetricDataSet.getMetricType());
                    //                ljOutlier.setMetricLabel(rawMetricDataSet.getM());
                    //                ljOutlier.setTotalCount();

                    outliers2.add(ljOutlier);
                }
            }
        }

        Map<String, Map<Integer, Map<PlotData, RowsAndMetricValues>>> processedMetricDataSet = getAllProcessedMetricDataSets(rawMetricDataSets);
        Map<String, PlotOutlier> metricOutlier = getQCPlotMetricOutliers(processedMetricGuides, processedMetricDataSet, sampleFiles.keySet());
        Map<String, Map<String, Map<String, Integer>>> transformedOutliers = getMetricOutliersByFileOrGuideSetGroup(metricOutlier);

        transformedOutliers.forEach((fileName, metrics) -> {
            SampleFileInfo sampleFileInfo = sampleFiles.get(fileName);
            metrics.forEach((metric, outliers) -> {
                LJOutlier matchedItem = null;
                for(LJOutlier item : sampleFileInfo.getItems())
                {
                    if (item.getMetricLabel() != null && item.getMetricLabel().equalsIgnoreCase(metric))
                    {
                        matchedItem = item;
                    }
                }

                if (matchedItem != null)
                {
                    for(Map.Entry<String, Integer> outlier : outliers.entrySet())
                    {
                        if (outlier.getKey().equalsIgnoreCase("mr"))
                            matchedItem.setmR(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMmP"))
                            matchedItem.setCUSUMmP(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMmN"))
                            matchedItem.setCUSUMmN(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMvP"))
                            matchedItem.setCUSUMvP(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMvN"))
                            matchedItem.setCUSUMvN(outlier.getValue());
                    }
                }
            });

        });

        sampleFiles.forEach((name, sample) -> {
            int CUSUMmP = 0, CUSUMmN = 0, CUSUMvP = 0, CUSUMvN = 0, mR = 0;
            for (LJOutlier item : sample.getItems())
            {
                CUSUMmN += item.getCUSUMmN();
                CUSUMmP += item.getCUSUMmP();
                CUSUMvP += item.getCUSUMvP();
                CUSUMvN += item.getCUSUMvN();
                mR += item.getmR();
            }
            sample.setCUSUMmP(CUSUMmP);
            sample.setCUSUMvP(CUSUMvP);
            sample.setCUSUMmN(CUSUMmN);
            sample.setCUSUMvN(CUSUMvN);
            sample.setmR(mR);
        });
        return sampleFiles;
    }

    /** Subset the rows to just those that are part of a guide set */
    private List<RawMetricDataSet> filterToTrainingData(List<RawMetricDataSet> rawRows, List<GuideSet> guideSets)
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

    public JSONObject getOtherQCSampleFileStats(List<LJOutlier> ljOutliers, List<RawMetricDataSet> rawMetricDataSets, List<GuideSet> guideSets, Integer limit)
    {
        return getSampleFilesJSON(getSampleFiles(ljOutliers, rawMetricDataSets, guideSets), limit);
    }

    public JSONObject getSampleFilesJSON(Map<String, SampleFileInfo> sampleFiles, Integer limit)
    {
        JSONObject sampleFilesJSON = new JSONObject();

        List<SampleFileInfo> files = new ArrayList<>(sampleFiles.values());
        files.sort(Comparator.comparing(SampleFileInfo::getAcquiredTime));

        if (limit != null && files.size() > limit.intValue())
        {
            files = files.subList(files.size() - 1 - limit.intValue(), files.size() - 1);
        }

        files.forEach(sample -> sampleFilesJSON.put(sample.getKey(), sample.toJSON()));

        return sampleFilesJSON;
    }

    private Map<String, SampleFileInfo> setSampleFiles(List<LJOutlier> ljOutliers)
    {
        if (ljOutliers.isEmpty())
            return Collections.emptyMap();

        int index = 1;
        SampleFileInfo sampleFileInfo = null;
        Map<String, SampleFileInfo> sampleFiles = new HashMap<>();

        for (LJOutlier ljOutlier : ljOutliers)
        {
            String sampleFileString = SampleFileInfo.getKey(ljOutlier.getSampleFile(), ljOutlier.getAcquiredTime());
            if (sampleFileInfo == null || (!(ljOutlier.getSampleFile() != null && sampleFileString.equals(getUniqueSampleFile(sampleFileInfo)))))
            {
                sampleFileInfo = new SampleFileInfo();
                sampleFileInfo.setIndex(index++);
                sampleFileInfo.setSampleFile(ljOutlier.getSampleFile());
                sampleFileInfo.setAcquiredTime(ljOutlier.getAcquiredTime());
                sampleFileInfo.setGuideSetId(ljOutlier.getGuideSetId());
                sampleFileInfo.setIgnoreForAllMetric(ljOutlier.isIgnoreInQC());
                sampleFiles.put(getUniqueSampleFile(sampleFileInfo), sampleFileInfo);
            }

            sampleFileInfo.setIgnoreForAllMetric(ljOutlier.isIgnoreInQC() && sampleFileInfo.isIgnoreForAllMetric());
            if (!ljOutlier.isIgnoreInQC())
            {
                sampleFileInfo.setMetrics(sampleFileInfo.getMetrics() + 1);
                sampleFileInfo.setLeveyJennings(sampleFileInfo.getLeveyJennings() + ljOutlier.getLeveyJennings());
                sampleFileInfo.setTotalCount(sampleFileInfo.getTotalCount() + ljOutlier.getTotalCount());
            }
            sampleFileInfo.getItems().add(ljOutlier);
        }

        return sampleFiles;
    }

    @NotNull
    private String getUniqueSampleFile(SampleFileInfo sampleFileInfo)
    {
        return sampleFileInfo.getSampleFile() + "_" + sampleFileInfo.getAcquiredTime();
    }
}
