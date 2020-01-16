/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:39 PM
 */
public class PrecursorChromInfo extends AbstractChromInfo
{
    private int _precursorId;
    private int _generalMoleculeChromInfoId;

    private Double _bestRetentionTime;
    private Double _minStartTime;
    private Double _maxEndTime;
    private Double _totalArea;
    private Double _totalAreaNormalized;
    private Double _totalBackground;
    private Double _maxFwhm;
    private Double _maxHeight;
    private Double _averageMassErrorPPM;
    private Double _peakCountRatio;
    private Integer _numTruncated;
    private String _identified;
    private Double _libraryDotP;
    private Double _isotopeDotP;
    private Integer _optimizationStep;
    private String _userSet;
    private String _note;
    private Double _qvalue;
    private Double _zscore;

    private byte[] _chromatogram;
    private int _numTransitions;

    private Double _ccs;
    private Double _driftTimeMs1;
    private Double _driftTimeFragment;
    private Double _driftTimeWindow;
    private Double _ionMobilityMs1;
    private Double _ionMobilityFragment;
    private Double _ionMobilityWindow;
    private String _ionMobilityType;


    public PrecursorChromInfo()
    {
    }

    public PrecursorChromInfo(Container c)
    {
        super(c);
    }

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }

    public int getGeneralMoleculeChromInfoId()
    {
        return _generalMoleculeChromInfoId;
    }

    public void setGeneralMoleculeChromInfoId(int generalmoleculechrominfoid)
    {
        _generalMoleculeChromInfoId = generalmoleculechrominfoid;
    }

    public Double getBestRetentionTime()
    {
        return _bestRetentionTime;
    }

    public void setBestRetentionTime(Double bestRetentionTime)
    {
        _bestRetentionTime = bestRetentionTime;
    }

    public Double getMinStartTime()
    {
        return _minStartTime;
    }

    public void setMinStartTime(Double minStartTime)
    {
        _minStartTime = minStartTime;
    }

    public Double getMaxEndTime()
    {
        return _maxEndTime;
    }

    public void setMaxEndTime(Double maxEndTime)
    {
        _maxEndTime = maxEndTime;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        _totalArea = totalArea;
    }

    public Double getTotalAreaNormalized()
    {
        return _totalAreaNormalized;
    }

    public void setTotalAreaNormalized(Double totalAreaNormalized)
    {
        _totalAreaNormalized = totalAreaNormalized;
    }

    public Double getTotalBackground()
    {
        return _totalBackground;
    }

    public void setTotalBackground(Double totalBackground)
    {
        _totalBackground = totalBackground;
    }

    public Double getMaxFwhm()
    {
        return _maxFwhm;
    }

    public void setMaxFwhm(Double maxFwhm)
    {
        _maxFwhm = maxFwhm;
    }

    public Double getMaxHeight()
    {
        return _maxHeight;
    }

    public void setMaxHeight(Double maxHeight)
    {
        _maxHeight = maxHeight;
    }

    public Double getAverageMassErrorPPM()
    {
        return _averageMassErrorPPM;
    }

    public void setAverageMassErrorPPM(Double averageMassErrorPPM)
    {
        _averageMassErrorPPM = averageMassErrorPPM;
    }

    public Double getPeakCountRatio()
    {
        return _peakCountRatio;
    }

    public void setPeakCountRatio(Double peakCountRatio)
    {
        _peakCountRatio = peakCountRatio;
    }

    public Integer getNumTruncated()
    {
        return _numTruncated;
    }

    public void setNumTruncated(Integer numTruncated)
    {
        _numTruncated = numTruncated;
    }

    public String getIdentified()
    {
        return _identified;
    }

    public void setIdentified(String identified)
    {
        _identified = identified;
    }

    public Double getLibraryDotP()
    {
        return _libraryDotP;
    }

    public void setLibraryDotP(Double libraryDotP)
    {
        _libraryDotP = libraryDotP;
    }

    public Double getIsotopeDotP()
    {
        return _isotopeDotP;
    }

    public void setIsotopeDotP(Double isotopeDotP)
    {
        _isotopeDotP = isotopeDotP;
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

    public Double getQvalue()
    {
        return _qvalue;
    }

    public void setQvalue(Double qvalue)
    {
        _qvalue = qvalue;
    }

    public Double getZscore()
    {
        return _zscore;
    }

    public void setZscore(Double zscore)
    {
        _zscore = zscore;
    }

    @Override @Nullable
    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    @Override
    public int getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumTransitions(int numTransitions)
    {
        _numTransitions = numTransitions;
    }

    @Override
    public Integer getUncompressedSize()
    {
        Integer result = super.getUncompressedSize();
        if (result == null)
        {
            result = (Integer.SIZE / 8) * getNumPoints() * (_numTransitions + 1);
        }

        return result;
    }

    /** Use the module property to decide whether to try fetching from disk*/
    @Nullable @Override
    public Chromatogram createChromatogram(TargetedMSRun run)
    {
        return createChromatogram(run, Boolean.parseBoolean(TargetedMSModule.PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.getEffectiveValue(getContainer())));
    }

    @Override
    public String toString()
    {
        return "PrecursorChromInfo" + _generalMoleculeChromInfoId;
    }
}
