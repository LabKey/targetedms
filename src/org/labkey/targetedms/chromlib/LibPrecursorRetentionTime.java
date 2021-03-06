/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;


/**
 * Does double-duty for both proteomics and small molecule since it's the same fields
 * User: vsharma
 * Date: 12/31/12
 */
public class LibPrecursorRetentionTime extends AbstractLibEntity
{
    private long _precursorId;
    private long _sampleFileId;
    private Double _retentionTime;
    private Double _startTime;
    private Double _endTime;
    private Integer _optimizationStep;

    public long getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(long precursorId)
    {
        _precursorId = precursorId;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getStartTime()
    {
        return _startTime;
    }

    public void setStartTime(Double startTime)
    {
        _startTime = startTime;
    }

    public Double getEndTime()
    {
        return _endTime;
    }

    public void setEndTime(Double endTime)
    {
        _endTime = endTime;
    }

    public Integer getOptimizationStep()
    {
        return _optimizationStep;
    }

    public void setOptimizationStep(Integer optimizationStep)
    {
        _optimizationStep = optimizationStep;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibPrecursorRetentionTime)) return false;

        LibPrecursorRetentionTime that = (LibPrecursorRetentionTime) o;

        if (_precursorId != that._precursorId) return false;
        if (_sampleFileId != that._sampleFileId) return false;
        if (_endTime != null ? !_endTime.equals(that._endTime) : that._endTime != null) return false;
        if (_retentionTime != null ? !_retentionTime.equals(that._retentionTime) : that._retentionTime != null)
            return false;
        if (_startTime != null ? !_startTime.equals(that._startTime) : that._startTime != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) _precursorId;
        result = (int) (31 * result + _sampleFileId);
        result = 31 * result + (_retentionTime != null ? _retentionTime.hashCode() : 0);
        result = 31 * result + (_startTime != null ? _startTime.hashCode() : 0);
        result = 31 * result + (_endTime != null ? _endTime.hashCode() : 0);
        return result;
    }
}
