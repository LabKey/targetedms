package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SampleFileInfo extends OutlierCounts
{
    final int sampleId;
    final String sampleFile;
    final Date acquiredTime;
    int guideSetId;
    boolean ignoreForAllMetric;
    /** Use a TreeMap to keep the metrics sorted by name */
    final Map<String, OutlierCounts> byMetric = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public SampleFileInfo(int sampleId, Date acquiredTime, String sampleFile)
    {
        this.sampleId = sampleId;
        this.acquiredTime = acquiredTime;
        this.sampleFile = sampleFile;
    }

    public String getSampleFile()
    {
        return sampleFile;
    }

    public Date getAcquiredTime()
    {
        return acquiredTime;
    }

    public int getGuideSetId()
    {
        return guideSetId;
    }

    // TODO - set!
    public void setGuideSetId(int guideSetId)
    {
        this.guideSetId = guideSetId;
    }

    public boolean isIgnoreForAllMetric()
    {
        return ignoreForAllMetric;
    }

    // TODO - SET!
    public void setIgnoreForAllMetric(boolean ignoreForAllMetric)
    {
        this.ignoreForAllMetric = ignoreForAllMetric;
    }

    public int getSampleId()
    {
        return sampleId;
    }

    public Map<String, OutlierCounts> getByMetric()
    {
        return byMetric;
    }

    public JSONArray getMetricsJSON()
    {
        JSONArray result = new JSONArray();
        for (Map.Entry<String, OutlierCounts> entry : byMetric.entrySet())
        {
            JSONObject metricCounts = entry.getValue().toJSON();
            metricCounts.put("MetricLabel", entry.getKey());
            result.put(metricCounts);
        }
        return result;
    }

    public OutlierCounts getMetricCounts(String metricLabel)
    {
        return byMetric.computeIfAbsent(metricLabel, x -> new OutlierCounts());
    }

    @Override @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = super.toJSON();

        jsonObject.put("SampleFile", sampleFile);
        jsonObject.put("AcquiredTime", acquiredTime);
        jsonObject.put("GuideSetId",  guideSetId);
        jsonObject.put("IgnoreForAllMetric",  ignoreForAllMetric);
        jsonObject.put("Metrics", getMetricsJSON());

        return jsonObject;
    }

    public static String getKey(String sampleName, Date acquiredTime)
    {
        return sampleName + "_" + acquiredTime;
    }

    public String getKey()
    {
        return getKey(sampleFile, acquiredTime);
    }
}
