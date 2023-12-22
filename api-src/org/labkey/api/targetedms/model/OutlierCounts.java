package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class OutlierCounts
{
    private final QCMetricConfiguration _metric;
    private int _CUSUMmP;
    private int _CUSUMvP;
    private int _CUSUMmN;
    private int _CUSUMvN;
    private int _mR;
    /** Either Levey-Jennings or value cutoff, depending on config */
    private int _value;

    /** Total number of data points under consideration */
    private int _totalCount;

    public OutlierCounts()
    {
        _metric = null;
    }

    public OutlierCounts(QCMetricConfiguration metric)
    {
        _metric = metric;
    }

    public int getCUSUMm()
    {
        return _CUSUMmP + _CUSUMmN;
    }

    public int getCUSUMv()
    {
        return getCUSUMvP() + getCUSUMvN();
    }

    public int getCUSUMmN()
    {
        return _CUSUMmN;
    }

    public void incrementCUSUMmN()
    {
        _CUSUMmN++;
    }

    public int getCUSUMmP()
    {
        return _CUSUMmP;
    }

    public void incrementCUSUMmP()
    {
        _CUSUMmP++;
    }

    public int getCUSUMvP()
    {
        return _CUSUMvP;
    }

    public void incrementCUSUMvP()
    {
        _CUSUMvP++;
    }

    public int getCUSUMvN()
    {
        return _CUSUMvN;
    }

    public void incrementCUSUMvN()
    {
        _CUSUMvN++;
    }

    public int getmR()
    {
        return _mR;
    }

    public void incrementMR()
    {
        _mR++;
    }

    public int getValue()
    {
        return _value;
    }

    public void incrementValue()
    {
        _value++;
    }

    public int getTotalCount()
    {
        return _totalCount;
    }

    public void incrementTotalCount()
    {
        _totalCount++;
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("TotalCount", getTotalCount());
        jsonObject.put("CUSUMm", getCUSUMm());
        jsonObject.put("CUSUMv", getCUSUMv());
        jsonObject.put("CUSUMmN", getCUSUMmN());
        jsonObject.put("CUSUMmP", getCUSUMmP());
        jsonObject.put("CUSUMvN", getCUSUMvN());
        jsonObject.put("CUSUMvP", getCUSUMvP());
        jsonObject.put("mR", getmR());
        jsonObject.put("Value", getValue());
        if (_metric != null)
        {
            jsonObject.put("MetricId", _metric.getId());
            jsonObject.put("MetricStatus", _metric.getStatus());
        }

        return jsonObject;
    }
}
