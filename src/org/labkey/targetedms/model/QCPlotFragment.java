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
    private boolean ignoreInQC;
    private Integer precursorId;
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

    public boolean isIgnoreInQC()
    {
        return ignoreInQC;
    }

    public void setIgnoreInQC(boolean ignoreInQC)
    {
        this.ignoreInQC = ignoreInQC;
    }

    public Integer getPrecursorId()
    {
        return precursorId;
    }

    public void setPrecursorId(Integer precursorId)
    {
        this.precursorId = precursorId;
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

    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("DataType", getDataType());
        jsonObject.put("SeriesLabel", getSeriesLabel());
        jsonObject.put("IgnoreInQC", isIgnoreInQC());
        jsonObject.put("PrecursorId", getPrecursorId());
        jsonObject.put("mz", getmZ());

        JSONArray guideSetArray = new JSONArray();
        for (GuideSetStats stats : getGuideSetStats())
        {
            JSONObject statsJSONObject = new JSONObject();
            statsJSONObject.put("GuideSetId", stats.getKey().getGuideSetId());
            statsJSONObject.put("LJStdDev", stats.getStandardDeviation());
            statsJSONObject.put("LJMean", stats.getAverage());
            statsJSONObject.put("MRStdDev", stats.getMovingRangeStdDev());
            statsJSONObject.put("MRMean", stats.getMovingRangeAverage());
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
            dataJsonObject.put("AcquiredTime", plotData.getAcquiredTime());
            dataJsonObject.put("Value", plotData.getMetricValue());
            dataJsonObject.put("SampleFileId", plotData.getSampleFileId());
            dataJsonObject.put("PrecursorChromInfoId", plotData.getPrecursorChromInfoId());
            dataJsonObject.put("InGuideSetTrainingRange", plotData.isInGuideSetTrainingRange());
            dataJsonObject.put("GuideSetId", plotData.getGuideSetId());
            dataJsonObject.put("MR", plotData.getmR());
            dataJsonObject.put("CUSUMmN", plotData.getCUSUMmN());
            dataJsonObject.put("CUSUMmP", plotData.getCUSUMmP());
            dataJsonObject.put("CUSUMvP", plotData.getmR());
            dataJsonObject.put("CUSUMvN", plotData.getmR());
            dataJsonArray.put(dataJsonObject);
        }

        jsonObject.put("data", dataJsonArray);
        return jsonObject;
    }

}
