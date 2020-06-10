package org.labkey.targetedms.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

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

    public JSONObject toJSON(boolean includeLJ, boolean includeMR, boolean includeMeanCusum, boolean includeVariableCusum)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("DataType", getDataType());
        jsonObject.put("SeriesLabel", getSeriesLabel());
        jsonObject.put("mz", getmZ());

        JSONArray guideSetArray = new JSONArray();
        for (GuideSetStats stats : getGuideSetStats())
        {
            JSONObject statsJSONObject = new JSONObject();
            statsJSONObject.put("GuideSetId", stats.getKey().getGuideSetId());
            if (includeLJ)
            {
                statsJSONObject.put("LJStdDev", stats.getStandardDeviation());
                statsJSONObject.put("LJMean", stats.getAverage());
            }
            if (includeMR)
            {
                statsJSONObject.put("MRStdDev", stats.getMovingRangeStdDev());
                statsJSONObject.put("MRMean", stats.getMovingRangeAverage());
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
            dataJsonObject.put("SampleFileId", plotData.getSampleFileId());
            dataJsonObject.put("PrecursorChromInfoId", plotData.getPrecursorChromInfoId());
            dataJsonObject.put("InGuideSetTrainingRange", plotData.isInGuideSetTrainingRange());
            dataJsonObject.put("GuideSetId", plotData.getGuideSetId());
            dataJsonObject.put("IgnoreInQC", plotData.isIgnoreInQC());
            dataJsonObject.put("PrecursorId", plotData.getPrecursorId());
            if (includeMR)
            {
                dataJsonObject.put("MR", plotData.getmR());
            }
            if (includeMeanCusum)
            {
                dataJsonObject.put("CUSUMmN", plotData.getCUSUMmN());
                dataJsonObject.put("CUSUMmP", plotData.getCUSUMmP());
            }
            if (includeVariableCusum)
            {
                dataJsonObject.put("CUSUMvP", plotData.getCUSUMvP());
                dataJsonObject.put("CUSUMvN", plotData.getCUSUMvN());
            }
            dataJsonArray.put(dataJsonObject);
        }

        jsonObject.put("data", dataJsonArray);
        return jsonObject;
    }

}
