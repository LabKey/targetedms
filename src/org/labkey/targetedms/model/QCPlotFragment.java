package org.labkey.targetedms.model;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.util.JsonUtil;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Peptide for QC Plot
 * */
public class QCPlotFragment
{
    private String seriesLabel;
    private String dataType;
    private Double mZ;
    private List<RawMetricDataSet> qcPlotData;
    private List<GuideSetStats> guideSetStats;
    @Nullable
    private Color _seriesColor;

    public String getSeriesLabel()
    {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel)
    {
        this.seriesLabel = seriesLabel;
    }

    public String getDataType()
    {
        return dataType;
    }

    public void setDataType(String dataType)
    {
        this.dataType = dataType;
    }

    public Double getmZ()
    {
        return mZ;
    }

    public void setmZ(Double mZ)
    {
        this.mZ = mZ;
    }

    public List<RawMetricDataSet> getQcPlotData()
    {
        return qcPlotData;
    }

    public void setQcPlotData(List<RawMetricDataSet> qcPlotData)
    {
        this.qcPlotData = qcPlotData;
    }

    public List<GuideSetStats> getGuideSetStats()
    {
        return guideSetStats;
    }

    public void setGuideSetStats(List<GuideSetStats> guideSetStats)
    {
        this.guideSetStats = guideSetStats;
    }

    public JSONObject toJSON(boolean includeLJ, boolean includeMR, boolean includeMeanCusum, boolean includeVariableCusum, boolean showExcluded, boolean includeTrailingMean, boolean includeTrailingCV, Map<GuideSetKey, GuideSetStats> targetedStats)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("DataType", getDataType());
        jsonObject.put("SeriesLabel", getSeriesLabel());
        if (_seriesColor != null)
        {
            jsonObject.put("SeriesColor", "#" + Integer.toHexString(_seriesColor.getRGB()).substring(2).toUpperCase());
        }
        jsonObject.put("mz", getmZ());

        JSONArray guideSetArray = new JSONArray();
        for (GuideSetStats stats : getGuideSetStats())
        {
            JSONObject statsJSONObject = new JSONObject();
            statsJSONObject.put("GuideSetId", stats.getKey().getGuideSetId());
            statsJSONObject.put("SeriesType", stats.getKey().getMetricSeriesIndex());
            if (includeLJ)
            {
                JsonUtil.safePut(statsJSONObject, "LJStdDev", stats.getStandardDeviation());
                statsJSONObject.put("LJMean", stats.getAverage());
            }
            if (includeMR)
            {
                statsJSONObject.put("StdDevMR", stats.getMovingRangeStdDev());
                statsJSONObject.put("MeanMR", stats.getMovingRangeAverage());
            }
            statsJSONObject.put("Comment", stats.getGuideSet().getComment());
            statsJSONObject.put("ReferenceEnd", stats.getGuideSet().getReferenceEnd());
            statsJSONObject.put("TrainingStart", stats.getGuideSet().getTrainingStart());
            statsJSONObject.put("TrainingEnd", stats.getGuideSet().getTrainingEnd());
            statsJSONObject.put("NumRecords", stats.getNumRecords());
            guideSetArray.put(statsJSONObject);
        }
        jsonObject.put("GuideSetStats", guideSetArray);

        JSONArray dataJsonArray = new JSONArray();
        for (RawMetricDataSet plotData : getQcPlotData())
        {
            JSONObject dataJsonObject = new JSONObject();
            dataJsonObject.put("Value", plotData.getMetricValue());
            if (plotData.isLeveyJenningsOutlier(targetedStats.get(plotData.getGuideSetKey())))
            {
                dataJsonObject.put("LJOutlier", true);
            }
            dataJsonObject.put("SampleFileId", plotData.getSampleFile().getId());
            dataJsonObject.put("PrecursorChromInfoId", plotData.getPrecursorChromInfoId());
            boolean ignoreInQC = plotData.getSampleFile().isIgnoreInQC(plotData.getMetricId());
            if (!ignoreInQC || showExcluded)
            {
                // Issue 45701: Samples excluded for single metric don't get hidden in QC plots
                if (ignoreInQC)
                {
                    // Reduce JSON payload size by only sending value for rows where it's true
                    dataJsonObject.put("IgnoreInQC", true);
                }
                dataJsonObject.put("PrecursorId", plotData.getPrecursorId());
                dataJsonObject.put("SeriesType", plotData.getMetricSeriesIndex());
                if (includeMR)
                {
                    dataJsonObject.put("MR", plotData.getmR());
                    if (plotData.isMovingRangeOutlier(targetedStats.get(plotData.getGuideSetKey())))
                    {
                        dataJsonObject.put("MROutlier", true);
                    }
                }
                if (includeMeanCusum)
                {
                    dataJsonObject.put("CUSUMmN", plotData.getCUSUMmN());
                    dataJsonObject.put("CUSUMmP", plotData.getCUSUMmP());
                    if (plotData.isCUSUMmNOutlier())
                    {
                        dataJsonObject.put("CUSUMmNOutlier", true);
                    }
                    if (plotData.isCUSUMmPOutlier())
                    {
                        dataJsonObject.put("CUSUMmPOutlier", true);
                    }
                }
                if (includeVariableCusum)
                {
                    dataJsonObject.put("CUSUMvP", plotData.getCUSUMvP());
                    dataJsonObject.put("CUSUMvN", plotData.getCUSUMvN());
                    if (plotData.isCUSUMvNOutlier())
                    {
                        dataJsonObject.put("CUSUMvNOutlier", true);
                    }
                    if (plotData.isCUSUMvPOutlier())
                    {
                        dataJsonObject.put("CUSUMvPOutlier", true);
                    }
                }
                if (includeTrailingMean)
                {
                    dataJsonObject.put("TrailingMean", plotData.getTrailingMean());
                }
                if (includeTrailingCV)
                {
                    dataJsonObject.put("TrailingCV", plotData.getTrailingCV());
                }
                if (includeTrailingMean || includeTrailingCV)
                {
                    dataJsonObject.put("TrailingStartDate", plotData.getTrailingStart());
                }
                dataJsonArray.put(dataJsonObject);
            }
        }

        jsonObject.put("data", dataJsonArray);
        return jsonObject;
    }

    public void setSeriesColor(Color seriesColor)
    {
        _seriesColor = seriesColor;
    }

    @Nullable
    public Color getSeriesColor()
    {
        return _seriesColor;
    }
}
