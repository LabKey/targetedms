/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.targetedms.parser;

import org.labkey.api.targetedms.model.SampleFileInfo;

import java.util.Date;
import java.util.List;

/**
 * User: jeckels
 * Date: Apr 18, 2012
 */
public class SampleFile extends SkylineEntity
{
    private long _replicateId;
    private Integer _instrumentId;
    private String _filePath;
    /** User-specified in Skyline */
    private String _sampleName;
    private String _skylineId;
    private Date _acquiredTime;
    private Date _modifiedTime;
    private Double _ticArea;
    private String _instrumentSerialNumber;
    /** Extracted by Skyline from raw file */
    private String _sampleId;
    private Double _explicitGlobalStandardArea;
    private String _ionMobilityType;

    // Calculated values loaded via TargetedMSManager.getSampleFiles()
    private Integer _guideSetId;
    private boolean _ignoreForAllMetric;

    private List<Instrument> _instrumentInfoList;

    public long getReplicateId()
    {
        return _replicateId;
    }

    public void setReplicateId(long replicateId)
    {
        _replicateId = replicateId;
    }

    public String getFilePath()
    {
        return _filePath;
    }

    public void setFilePath(String filePath)
    {
        _filePath = filePath;
    }

    public String getSampleName()
    {
        return _sampleName;
    }

    public void setSampleName(String sampleName)
    {
        _sampleName = sampleName;
    }

    public Date getAcquiredTime()
    {
        return _acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        _acquiredTime = acquiredTime;
    }

    public Date getModifiedTime()
    {
        return _modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime)
    {
        _modifiedTime = modifiedTime;
    }

    public Integer getInstrumentId()
    {
        return _instrumentId;
    }

    public void setInstrumentId(Integer instrumentId)
    {
        _instrumentId = instrumentId;
    }

    public String getSkylineId()
    {
        return _skylineId;
    }

    public void setSkylineId(String skylineId)
    {
        _skylineId = skylineId;
    }

    public List<Instrument> getInstrumentInfoList()
    {
        return _instrumentInfoList;
    }

    public void setInstrumentInfoList(List<Instrument> instrumentInfoList)
    {
        _instrumentInfoList = instrumentInfoList;
    }

    public Double getTicArea()
    {
        return _ticArea;
    }

    public void setTicArea(Double ticArea)
    {
        _ticArea = ticArea;
    }

    public String getInstrumentSerialNumber()
    {
        return _instrumentSerialNumber;
    }

    public void setInstrumentSerialNumber(String instrumentSerialNumber)
    {
        _instrumentSerialNumber = instrumentSerialNumber;
    }

    public String getSampleId()
    {
        return _sampleId;
    }

    public void setSampleId(String sampleId)
    {
        _sampleId = sampleId;
    }

    public Double getExplicitGlobalStandardArea()
    {
        return _explicitGlobalStandardArea;
    }

    public void setExplicitGlobalStandardArea(Double explicitGlobalStandardArea)
    {
        _explicitGlobalStandardArea = explicitGlobalStandardArea;
    }

    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    public Integer getGuideSetId()
    {
        return _guideSetId;
    }

    public void setGuideSetId(Integer guideSetId)
    {
        _guideSetId = guideSetId;
    }

    public boolean isIgnoreForAllMetric()
    {
        return _ignoreForAllMetric;
    }

    public void setIgnoreForAllMetric(boolean ignoreForAllMetric)
    {
        _ignoreForAllMetric = ignoreForAllMetric;
    }

    public SampleFileInfo toSampleFileInfo()
    {
        return new SampleFileInfo(getId(), getAcquiredTime(), getSampleName(), _guideSetId, _ignoreForAllMetric, getFilePath(), getReplicateId());
    }
}
