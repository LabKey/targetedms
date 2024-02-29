/*
 * Copyright (c) 2012-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.parser;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:04 PM
 */
public class TransitionChromInfo extends ChromInfo<TransitionChromInfoAnnotation>
{
    private long _transitionId;
    private long _precursorChromInfoId;

    private Double _retentionTime;
    private Double _startTime;
    private Double _endTime;
    private Double _height;
    private Double _area;
    private Double _background;
    private Double _massErrorPPM;
    private Double _fwhm;
    private Boolean _fwhmDegenerate;
    private Boolean _truncated;
    private Integer _peakRank;
    private String _identified;
    private Integer _optimizationStep;
    private String _userSet;
    private String _note;
    private int _chromatogramIndex;
    private Integer _pointsAcrossPeak;
    private Double _ccs;
    private Double _ionMobility;
    private Double _ionMobilityWindow;
    private String _ionMobilityType;
    private Integer _rank;
    private Integer _rankByLevel;
    private Boolean _forcedIntegration;
    private Double _skewness;
    private Double _kurtosis;
    private Double _stdDev;
    private Double _shapeCorrelation;

    public long getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(long transitionId)
    {
        _transitionId = transitionId;
    }

    public long getPrecursorChromInfoId()
    {
        return _precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(long precursorChromInfoId)
    {
        _precursorChromInfoId = precursorChromInfoId;
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

    public Double getHeight()
    {
        return _height;
    }

    public void setHeight(Double height)
    {
        _height = height;
    }

    public Double getArea()
    {
        return _area;
    }

    public void setArea(Double area)
    {
        _area = area;
    }

    public Double getBackground()
    {
        return _background;
    }

    public void setBackground(Double background)
    {
        _background = background;
    }

    public Double getMassErrorPPM()
    {
        return _massErrorPPM;
    }

    public void setMassErrorPPM(Double massErrorPPM)
    {
        _massErrorPPM = massErrorPPM;
    }

    public Double getFwhm()
    {
        return _fwhm;
    }

    public void setFwhm(Double fwhm)
    {
        _fwhm = fwhm;
    }

    public Boolean getFwhmDegenerate()
    {
        return _fwhmDegenerate;
    }

    public void setFwhmDegenerate(Boolean fwhmDegenerate)
    {
        _fwhmDegenerate = fwhmDegenerate;
    }

    public Boolean getTruncated()
    {
        return _truncated;
    }

    public void setTruncated(Boolean truncated)
    {
        _truncated = truncated;
    }

    public Integer getPeakRank()
    {
        return _peakRank;
    }

    public void setPeakRank(Integer peakRank)
    {
        _peakRank = peakRank;
    }

    public String getIdentified()
    {
        return _identified;
    }

    public void setIdentified(String identified)
    {
        _identified = identified;
    }

    public Integer getOptimizationStep()
    {
        return _optimizationStep;
    }

    public void setOptimizationStep(Integer optimizationStep)
    {
        _optimizationStep = optimizationStep;
    }

    public boolean isOptimizationPeak()
    {
        return _optimizationStep != null;
    }

    public String getUserSet()
    {
        return _userSet;
    }

    public void setUserSet(String userSet)
    {
        _userSet = userSet;
    }

    public String getNote()
    {
        return _note;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public void setChromatogramIndex(int chromatogramIndex)
    {
        _chromatogramIndex = chromatogramIndex;
    }

    public int getChromatogramIndex()
    {
        return _chromatogramIndex;
    }

    public Integer getPointsAcrossPeak()
    {
        return _pointsAcrossPeak;
    }

    public void setPointsAcrossPeak(Integer pointsAcrossPeak)
    {
        _pointsAcrossPeak = pointsAcrossPeak;
    }

    public Double getCcs()
    {
        return _ccs;
    }

    public void setCcs(Double ccs)
    {
        _ccs = ccs;
    }

    public Double getIonMobility()
    {
        return _ionMobility;
    }

    public void setIonMobility(Double ionMobility)
    {
        _ionMobility = ionMobility;
    }

    public Double getIonMobilityWindow()
    {
        return _ionMobilityWindow;
    }

    public void setIonMobilityWindow(Double ionMobilityWindow)
    {
        _ionMobilityWindow = ionMobilityWindow;
    }

    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    public Integer getRank()
    {
        return _rank;
    }

    public void setRank(Integer rank)
    {
        _rank = rank;
    }

    public Integer getRankByLevel()
    {
        return _rankByLevel;
    }

    public void setRankByLevel(Integer rankByLevel)
    {
        _rankByLevel = rankByLevel;
    }

    public Boolean getForcedIntegration()
    {
        return _forcedIntegration;
    }

    public void setForcedIntegration(Boolean forcedIntegration)
    {
        _forcedIntegration = forcedIntegration;
    }

    public Double getSkewness()
    {
        return _skewness;
    }

    public void setSkewness(Double skewness)
    {
        _skewness = skewness;
    }

    public Double getKurtosis()
    {
        return _kurtosis;
    }

    public void setKurtosis(Double kurtosis)
    {
        _kurtosis = kurtosis;
    }

    public Double getStdDev()
    {
        return _stdDev;
    }

    public void setStdDev(Double stdDev)
    {
        _stdDev = stdDev;
    }

    public Double getShapeCorrelation()
    {
        return _shapeCorrelation;
    }

    public void setShapeCorrelation(Double shapeCorrelation)
    {
        _shapeCorrelation = shapeCorrelation;
    }
}
