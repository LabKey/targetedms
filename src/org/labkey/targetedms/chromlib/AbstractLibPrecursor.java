package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AbstractLibPrecursor<TransitionType extends AbstractLibTransition> extends AbstractLibEntity
{
    protected String _isotopeLabel;
    protected double _mz;
    protected int _charge;
    protected Double _collisionEnergy;
    protected Double _declusteringPotential;
    protected Double _totalArea;
    protected byte[] _chromatogram;
    protected int _uncompressedSize;
    protected int _chromatogramFormat;
    protected long _sampleFileId;

    protected Double _explicitIonMobility;
    protected Double _ccs;
    protected Double _ionMobilityMS1;
    protected Double _ionMobilityFragment;

    protected Double _ionMobilityWindow;
    protected String _ionMobilityType;

    protected Integer _numTransitions;
    protected Integer _numPoints;
    protected Double _averageMassErrorPPM;

    public AbstractLibPrecursor() {}

    public AbstractLibPrecursor(GeneralPrecursor<?> p, Map<Long, String> isotopeLabelMap,
                                PrecursorChromInfo bestChromInfo, TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        String isotopeLabel = isotopeLabelMap.get(p.getIsotopeLabelId());
        if(isotopeLabel == null)
        {
            throw new IllegalStateException("Isotope label name not found for Id "+p.getIsotopeLabelId());
        }
        setIsotopeLabel(isotopeLabel);
        setMz(p.getMz());
        setCharge(p.getCharge());
        setCollisionEnergy(p.getCollisionEnergy());
        setDeclusteringPotential(p.getDeclusteringPotential());

        if(bestChromInfo != null)
        {
            setTotalArea(bestChromInfo.getTotalArea() == null ? 0.0 : bestChromInfo.getTotalArea());
            setChromatogram(bestChromInfo.getChromatogramBytes(run));
            setUncompressedSize(bestChromInfo.getUncompressedSize());
            setChromatogramFormat(bestChromInfo.getChromatogramFormat());
            setNumTransitions(bestChromInfo.getNumTransitions());
            setNumPoints(bestChromInfo.getNumPoints());
            setAverageMassErrorPPM(bestChromInfo.getAverageMassErrorPPM());

            long sampleFileId = bestChromInfo.getSampleFileId();
            Integer libSampleFileId = sampleFileIdMap.get(sampleFileId);
            if(libSampleFileId == null)
            {
                throw new IllegalStateException("Could not find an Id in the library for sample file Id "+sampleFileId);
            }
            setSampleFileId(libSampleFileId.intValue());
        }
        else
        {
            setTotalArea(0.0);
            setNumTransitions(0);
            setNumPoints(0);
        }

    }

    protected List<LibPrecursorRetentionTime> _retentionTimes;

    protected List<TransitionType> _transitions;

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public double getMz()
    {
        return _mz;
    }

    public void setMz(double mz)
    {
        _mz = mz;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public Double getCollisionEnergy()
    {
        return _collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        _collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return _declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        _declusteringPotential = declusteringPotential;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        _totalArea = totalArea;
    }

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    public int getUncompressedSize()
    {
        return _uncompressedSize;
    }

    public void setUncompressedSize(int uncompressedSize)
    {
        _uncompressedSize = uncompressedSize;
    }

    public int getChromatogramFormat()
    {
        return _chromatogramFormat;
    }

    public void setChromatogramFormat(int chromatogramFormat)
    {
        _chromatogramFormat = chromatogramFormat;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }

    public Double getExplicitIonMobility()
    {
        return _explicitIonMobility;
    }

    public void setExplicitIonMobility(Double explicitIonMobility)
    {
        _explicitIonMobility = explicitIonMobility;
    }

    public Double getCcs()
    {
        return _ccs;
    }

    public void setCcs(Double ccs)
    {
        _ccs = ccs;
    }

    public Double getIonMobilityMS1()
    {
        return _ionMobilityMS1;
    }

    public void setIonMobilityMS1(Double ionMobilityMS1)
    {
        _ionMobilityMS1 = ionMobilityMS1;
    }

    public Double getIonMobilityFragment()
    {
        return _ionMobilityFragment;
    }

    public void setIonMobilityFragment(Double ionMobilityFragment)
    {
        _ionMobilityFragment = ionMobilityFragment;
    }

    public Double getIonMobilityWindow()
    {
        return _ionMobilityWindow;
    }

    public void setIonMobilityWindow(Double ionMobilityWindow)
    {
        _ionMobilityWindow = ionMobilityWindow;
    }

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setAverageMassErrorPPM(Double averageMassErrorPPM)
    {
        _averageMassErrorPPM = averageMassErrorPPM;
    }

    public Double getAverageMassErrorPPM()
    {
        return _averageMassErrorPPM;
    }

    public void setNumTransitions(Integer numTransitions)
    {
        _numTransitions = numTransitions;
    }

    public Integer getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumPoints(Integer numPoints)
    {
        _numPoints = numPoints;
    }

    public Integer getNumPoints()
    {
        return _numPoints;
    }



    public void addRetentionTime(LibPrecursorRetentionTime retentionTime)
    {
        if(_retentionTimes == null)
        {
            _retentionTimes = new ArrayList<>();
        }
        _retentionTimes.add(retentionTime);
    }

    public List<LibPrecursorRetentionTime> getRetentionTimes()
    {
        if(_retentionTimes == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_retentionTimes);
    }

    public void addTransition(TransitionType transition)
    {
        if(_transitions == null)
        {
            _transitions = new ArrayList<>();
        }
        _transitions.add(transition);
    }

    public List<TransitionType> getTransitions()
    {
        if(_transitions == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_transitions);
    }

    @Override
    public int getCacheSize()
    {
        return super.getCacheSize() +
                getTransitions().stream().mapToInt(AbstractLibEntity::getCacheSize).sum() +
                getRetentionTimes().stream().mapToInt(AbstractLibEntity::getCacheSize).sum();

    }
}