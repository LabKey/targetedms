/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
