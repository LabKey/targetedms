/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

public class QCMetricConfiguration implements Comparable<QCMetricConfiguration>
{
    private int _id;
    private String _name;
    private String _queryName;
    private boolean _precursorScoped;
    private String _enabledQueryName;
    private String _enabledSchemaName;
    private QCMetricStatus _status;
    private String _traceName;
    private Double _minTimeValue;
    private Double _maxTimeValue;
    private String _timeValueOption;
    private Double _traceValue;
    private String _yAxisLabel;
    private Double _upperBound;
    private Double _lowerBound;

    public int getId()
    {
        return _id;
    }

    public void setId(int rowId)
    {
        _id = rowId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    public void setQueryName(String queryName)
    {
        _queryName = queryName;
    }

    public boolean isPrecursorScoped()
    {
        return _precursorScoped;
    }

    public void setPrecursorScoped(boolean precursorScoped)
    {
        _precursorScoped = precursorScoped;
    }

    public String getEnabledQueryName()
    {
        return _enabledQueryName;
    }

    public void setEnabledQueryName(String enabledQueryName)
    {
        _enabledQueryName = enabledQueryName;
    }

    public QCMetricStatus getStatus()
    {
        return _status;
    }

    public void setStatus(QCMetricStatus status)
    {
        _status = status;
    }

    public String getEnabledSchemaName()
    {
        return _enabledSchemaName;
    }

    public void setEnabledSchemaName(String enabledSchemaName)
    {
        _enabledSchemaName = enabledSchemaName;
    }

    public String getTraceName()
    {
        return _traceName;
    }

    public void setTraceName(String traceName)
    {
        _traceName = traceName;
    }

    public Double getMinTimeValue()
    {
        return _minTimeValue;
    }

    public void setMinTimeValue(Double minTimeValue)
    {
        _minTimeValue = minTimeValue;
    }

    public Double getMaxTimeValue()
    {
        return _maxTimeValue;
    }

    public void setMaxTimeValue(Double maxTimeValue)
    {
        _maxTimeValue = maxTimeValue;
    }

    public String getTimeValueOption()
    {
        return _timeValueOption;
    }

    public void setTimeValueOption(String timeValueOption)
    {
        _timeValueOption = timeValueOption;
    }

    public Double getTraceValue()
    {
        return _traceValue;
    }

    public void setTraceValue(Double traceValue)
    {
        _traceValue = traceValue;
    }

    public String getyAxisLabel()
    {
        return _yAxisLabel;
    }

    public void setyAxisLabel(String yAxisLabel)
    {
        _yAxisLabel = yAxisLabel;
    }

    public Double getUpperBound()
    {
        return _upperBound;
    }

    public void setUpperBound(Double upperBound)
    {
        _upperBound = upperBound;
    }

    public Double getLowerBound()
    {
        return _lowerBound;
    }

    public void setLowerBound(Double lowerBound)
    {
        _lowerBound = lowerBound;
    }

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", _id);
        jsonObject.put("name", _name);
        jsonObject.put("queryName", _queryName);
        jsonObject.put("precursorScoped",  _precursorScoped);
        jsonObject.put("metricStatus", getStatus() == null ? QCMetricStatus.DEFAULT.toString() : getStatus().toString());
        if (_enabledQueryName != null) {
            jsonObject.put("enabledQueryName", _enabledQueryName);
        }
        if (_enabledQueryName != null) {
            jsonObject.put("enabledSchemaName", _enabledSchemaName);
        }
        if (_traceName != null) {
            jsonObject.put("traceName", _traceName);
        }
        if (_traceValue != null) {
            jsonObject.put("traceValue", _traceValue);
        }
        if (_minTimeValue != null) {
            jsonObject.put("minTimeValue", _minTimeValue);
        }
        if (_maxTimeValue != null) {
            jsonObject.put("maxTimeValue", _maxTimeValue);
        }
        if (_timeValueOption != null) {
            jsonObject.put("timeValueOption", _timeValueOption);
        }
        if (_yAxisLabel != null) {
            jsonObject.put("yAxisLabels", _yAxisLabel);
        }
        if (_lowerBound != null) {
            jsonObject.put("lowerBound", _lowerBound);
        }
        if (_upperBound != null) {
            jsonObject.put("upperBound", _upperBound);
        }

        return jsonObject;
    }

    @Override
    public int compareTo(@NotNull QCMetricConfiguration o)
    {
        return getName().compareToIgnoreCase(o.getName());
    }
}
