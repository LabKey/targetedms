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
    private int _leveyJennings;
    private int _valueCutoff;

    /** Total number of data points under consideration */
    private int _totalCount;
    private int _totalConfiguredOutlierCount;

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

    public int getLeveyJennings()
    {
        return _leveyJennings;
    }

    public void incrementLeveyJennings(QCMetricConfiguration metric)
    {
        _leveyJennings++;
        if (metric != null && metric.getStatus() == QCMetricStatus.LeveyJennings)
        {
            _totalConfiguredOutlierCount++;
        }
    }

    public int getTotalCount()
    {
        return _totalCount;
    }

    public void incrementTotalCount()
    {
        _totalCount++;
    }

    public int getTotalConfiguredOutlierCount()
    {
        return _totalConfiguredOutlierCount;
    }

    public int getValueCutoff()
    {
        return _valueCutoff;
    }

    public void incrementValueCutoff(QCMetricConfiguration metric)
    {
        _valueCutoff++;
        if (metric != null && metric.getStatus() == QCMetricStatus.ValueCutoff)
        {
            _totalConfiguredOutlierCount++;
        }
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
        jsonObject.put("LeveyJennings", getLeveyJennings());
        jsonObject.put("ValueCutoff", getValueCutoff());
        jsonObject.put("TotalConfiguredOutlierCount", getTotalConfiguredOutlierCount());
        if (_metric != null)
        {
            jsonObject.put("MetricId", _metric.getId());
            jsonObject.put("MetricStatus", _metric.getStatus());
        }

        return jsonObject;
    }
}
