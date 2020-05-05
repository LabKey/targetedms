/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.model;

import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class RawMetricDataSet
{
    String seriesLabel;
    Double metricValue;
    String metricType;
    String seriesType;
    int guideSetId;

    String sampleFile;
    Integer precursorId;
    Integer precursorChromInfoId;
    String dataType;
    Double mz;
    Date acquiredTime;
    String filePath;
    Integer replicateId;
    boolean ignoreInQC;
    boolean inGuideSetTrainingRange;
    Double mR;
    Double cUSUMmP;
    Double cUSUMmN;
    Double CUSUMvP;
    Double CUSUMvN;

    @Nullable
    public String getSeriesLabel()
    {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel)
    {
        this.seriesLabel = seriesLabel;
    }

    @Nullable
    public Double getMetricValue()
    {
        return metricValue;
    }

    public void setMetricValue(Double metricValue)
    {
        this.metricValue = metricValue;
    }

    @Nullable
    public String getMetricType()
    {
        return metricType;
    }

    public void setMetricType(String metricType)
    {
        this.metricType = metricType;
    }

    @Nullable
    public String getSeriesType()
    {
        return seriesType;
    }

    public void setSeriesType(String seriesType)
    {
        this.seriesType = seriesType;
    }

    public int getGuideSetId()
    {
        return guideSetId;
    }

    public void setGuideSetId(int guideSetId)
    {
        this.guideSetId = guideSetId;
    }

    public String getSampleFile()
    {
        return sampleFile;
    }

    public void setSampleFile(String sampleFile)
    {
        this.sampleFile = sampleFile;
    }

    @Nullable
    public Integer getPrecursorId()
    {
        return precursorId;
    }

    public void setPrecursorId(Integer precursorId)
    {
        this.precursorId = precursorId;
    }

    @Nullable
    public Integer getPrecursorChromInfoId()
    {
        return precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(Integer precursorChromInfoId)
    {
        this.precursorChromInfoId = precursorChromInfoId;
    }

    public String getDataType()
    {
        return dataType;
    }

    public void setDataType(String dataType)
    {
        this.dataType = dataType;
    }

    @Nullable
    public Double getMz()
    {
        return mz;
    }

    public void setMz(Double mz)
    {
        this.mz = mz;
    }

    @Nullable
    public Date getAcquiredTime()
    {
        return acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        this.acquiredTime = acquiredTime;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    @Nullable
    public Integer getReplicateId()
    {
        return replicateId;
    }

    public void setReplicateId(Integer replicateId)
    {
        this.replicateId = replicateId;
    }

    public boolean isIgnoreInQC()
    {
        return ignoreInQC;
    }

    public void setIgnoreInQC(boolean ignoreInQC)
    {
        this.ignoreInQC = ignoreInQC;
    }

    public boolean isInGuideSetTrainingRange()
    {
        return inGuideSetTrainingRange;
    }

    public void setInGuideSetTrainingRange(boolean inGuideSetTrainingRange)
    {
        this.inGuideSetTrainingRange = inGuideSetTrainingRange;
    }

    @Nullable
    public Double getmR()
    {
        return mR;
    }

    public void setmR(Double mR)
    {
        this.mR = mR;
    }

    @Nullable
    public Double getcUSUMmP()
    {
        return cUSUMmP;
    }

    public void setcUSUMmP(Double cUSUMmP)
    {
        this.cUSUMmP = cUSUMmP;
    }

    @Nullable
    public Double getcUSUMmN()
    {
        return cUSUMmN;
    }

    public void setcUSUMmN(Double cUSUMmN)
    {
        this.cUSUMmN = cUSUMmN;
    }

    @Nullable
    public Double getCUSUMvP()
    {
        return CUSUMvP;
    }

    public void setCUSUMvP(Double CUSUMvP)
    {
        this.CUSUMvP = CUSUMvP;
    }

    @Nullable
    public Double getCUSUMvN()
    {
        return CUSUMvN;
    }

    public void setCUSUMvN(Double CUSUMvN)
    {
        this.CUSUMvN = CUSUMvN;
    }
}
