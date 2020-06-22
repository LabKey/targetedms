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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.targetedms.model.OutlierCounts;
import org.labkey.api.visualization.Stats;

import java.util.Date;

public class RawMetricDataSet
{
    String seriesLabel;
    Double metricValue;
    int metricId;
    int metricSeriesIndex;
    int guideSetId;
    int sampleFileId;

    Integer precursorId;
    Integer precursorChromInfoId;
    String dataType;
    Double mz;
    Date acquiredTime;
    boolean ignoreInQC;
    boolean inGuideSetTrainingRange;
    Double mR;
    Double cusumMP;
    Double cusumMN;
    Double cusumVP;
    Double cusumVN;

    String filePath;
    int replicateId;

    Double massMonoisotopic;
    Double massAverage;
    String precursorCharge;

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

    public int getMetricId()
    {
        return metricId;
    }

    public void setMetricId(int metricId)
    {
        this.metricId = metricId;
    }

    public int getMetricSeriesIndex()
    {
        return metricSeriesIndex;
    }

    public void setMetricSeriesIndex(int metricSeriesIndex)
    {
        this.metricSeriesIndex = metricSeriesIndex;
    }

    public GuideSetKey getGuideSetKey()
    {
        return new GuideSetKey(getMetricId(), getMetricSeriesIndex(), getGuideSetId(), getSeriesLabel());
    }

    public int getGuideSetId()
    {
        return guideSetId;
    }

    public void setGuideSetId(int guideSetId)
    {
        this.guideSetId = guideSetId;
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

    public int getSampleFileId()
    {
        return sampleFileId;
    }

    public void setSampleFileId(int sampleFileId)
    {
        this.sampleFileId = sampleFileId;
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
    public Double getCUSUMmP()
    {
        return cusumMP;
    }

    public void setCUSUMmP(Double d)
    {
        this.cusumMP = d;
    }

    @Nullable
    public Double getCUSUMmN()
    {
        return cusumMN;
    }

    public void setCUSUMmN(Double d)
    {
        this.cusumMN = d;
    }

    @Nullable
    public Double getCUSUMvP()
    {
        return cusumVP;
    }

    public void setCUSUMvP(Double d)
    {
        this.cusumVP = d;
    }

    @Nullable
    public Double getCUSUMvN()
    {
        return cusumVN;
    }

    public void setCUSUMvN(Double d)
    {
        this.cusumVN = d;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public int getReplicateId()
    {
        return replicateId;
    }

    public void setReplicateId(int replicateId)
    {
        this.replicateId = replicateId;
    }

    public Double getMassMonoisotopic()
    {
        return massMonoisotopic;
    }

    public void setMassMonoisotopic(Double massMonoisotopic)
    {
        this.massMonoisotopic = massMonoisotopic;
    }

    public Double getMassAverage()
    {
        return massAverage;
    }

    public void setMassAverage(Double massAverage)
    {
        this.massAverage = massAverage;
    }

    public String getPrecursorCharge()
    {
        return precursorCharge;
    }

    public void setPrecursorCharge(String precursorCharge)
    {
        this.precursorCharge = precursorCharge;
    }

    public void buildSeriesLabel()
    {
        StringBuilder modifiedSL = new StringBuilder();

        String sl = this.seriesLabel;
        // means seriesLabel and modifiedSequence were null
        if (sl.endsWith(","))
        {
            modifiedSL.append(sl);

            if (null != this.massMonoisotopic && null != this.massAverage)
            {
                double scale = Math.pow(10, 4);
                modifiedSL.append(" [");
                modifiedSL.append(Math.round(this.massMonoisotopic * scale) / scale);
                modifiedSL.append("/");
                modifiedSL.append(Math.round(this.massAverage * scale) / scale);
                modifiedSL.append("]");
            }

            if (null != this.precursorCharge)
            {
                modifiedSL.append(" ");
                modifiedSL.append(this.precursorCharge);
            }

            this.setSeriesLabel(modifiedSL.toString());
        }
    }

    public boolean isLeveyJenningsOutlier(GuideSetStats stat)
    {
        if (ignoreInQC || metricValue == null || stat == null)
        {
            return false;
        }

        double upperLimit = stat.getAverage() + stat.getStandardDeviation() * 3;
        double lowerLimit = stat.getAverage() - stat.getStandardDeviation() * 3;

        return metricValue > upperLimit || metricValue < lowerLimit || (Double.isNaN(stat.getStandardDeviation()) && metricValue != stat.getAverage());
    }

    public boolean isMovingRangeOutlier(GuideSetStats stat)
    {
        return !isIgnoreInQC() && stat != null && mR != null && mR > Stats.MOVING_RANGE_UPPER_LIMIT_WEIGHT * stat.getMovingRangeAverage();
    }

    private boolean isCUSUMOutlier(Double value)
    {
        return !isIgnoreInQC() && value != null && value > Stats.CUSUM_CONTROL_LIMIT;
    }

    public boolean isCUSUMvPOutlier()
    {
        return isCUSUMOutlier(cusumVP);
    }

    public boolean isCUSUMvNOutlier()
    {
        return isCUSUMOutlier(cusumVN);
    }

    public boolean isCUSUMmPOutlier()
    {
        return isCUSUMOutlier(cusumMP);
    }

    public boolean isCUSUMmNOutlier()
    {
        return isCUSUMOutlier(cusumMN);
    }

    public void increment(@NotNull OutlierCounts counts, @NotNull GuideSetStats stats)
    {
        counts.setTotalCount(counts.getTotalCount() + 1);
        if (isLeveyJenningsOutlier(stats))
        {
            counts.setLeveyJennings(counts.getLeveyJennings() + 1);
        }
        if (isMovingRangeOutlier(stats))
        {
            counts.setmR(counts.getmR() + 1);
        }
        if (isCUSUMmPOutlier())
        {
            counts.setCUSUMmP(counts.getCUSUMmP() + 1);
        }
        if (isCUSUMmNOutlier())
        {
            counts.setCUSUMmN(counts.getCUSUMmN() + 1);
        }
        if (isCUSUMvPOutlier())
        {
            counts.setCUSUMvP(counts.getCUSUMvP() + 1);
        }
        if (isCUSUMvNOutlier())
        {
            counts.setCUSUMvN(counts.getCUSUMvN() + 1);
        }
    }
}
