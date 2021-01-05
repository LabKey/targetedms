package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.TransitionChromInfo;

public abstract class AbstractLibTransition extends AbstractLibEntity
{
    protected Double _mz;
    protected Integer _charge;
    protected String _fragmentType;
    protected Integer _fragmentOrdinal;
    protected Integer _massIndex;
    protected Double _area;
    protected Double _height;
    protected Double _fwhm;
    protected Integer _chromatogramIndex;
    protected Double _massErrorPPM;

    private Double _optimizationValue;
    private String _optimizationType;

    public AbstractLibTransition() {}

    public AbstractLibTransition(GeneralTransition transition, TransitionChromInfo tci)
    {
        setMz(transition.getMz());
        setCharge(transition.getCharge());
        setFragmentType(transition.getFragmentType());
        setMassIndex(transition.getMassIndex());

        if(tci != null)
        {
            setArea(tci.getArea() == null ? 0.0 : tci.getArea());
            setHeight(tci.getHeight() == null ? 0.0 : tci.getHeight());
            setFwhm(tci.getFwhm() == null ? 0.0 : tci.getFwhm());
            setChromatogramIndex(tci.getChromatogramIndex());
            setMassErrorPPM(tci.getMassErrorPPM());
        }
        else
        {
            setArea(0.0);
            setHeight(0.0);
            setFwhm(0.0);
        }

        if (transition.getCollisionEnergy() != null)
        {
            setOptimizationValue(transition.getCollisionEnergy());
            setOptimizationType("ce");
        }
        if (transition.getDeclusteringPotential() != null)
        {
            setOptimizationValue(transition.getDeclusteringPotential());
            setOptimizationType("dp");
        }

    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        _mz = mz;
    }

    public Integer getCharge()
    {
        return _charge;
    }

    public void setCharge(Integer charge)
    {
        _charge = charge;
    }

    public String getFragmentType()
    {
        return _fragmentType;
    }

    public void setFragmentType(String fragmentType)
    {
        _fragmentType = fragmentType;
    }

    public Integer getMassIndex()
    {
        return _massIndex;
    }

    public void setMassIndex(Integer massIndex)
    {
        _massIndex = massIndex;
    }

    public Double getArea()
    {
        return _area;
    }

    public void setArea(Double area)
    {
        _area = area;
    }

    public Double getHeight()
    {
        return _height;
    }

    public void setHeight(Double height)
    {
        _height = height;
    }

    public Double getFwhm()
    {
        return _fwhm;
    }

    public void setFwhm(Double fwhm)
    {
        _fwhm = fwhm;
    }

    public Integer getChromatogramIndex()
    {
        return _chromatogramIndex;
    }

    public void setChromatogramIndex(Integer chromatogramIndex)
    {
        _chromatogramIndex = chromatogramIndex;
    }

    public void setMassErrorPPM(Double massErrorPPM)
    {
        _massErrorPPM = massErrorPPM;
    }

    public Double getMassErrorPPM()
    {
        return _massErrorPPM;
    }

    public Double getOptimizationValue()
    {
        return _optimizationValue;
    }

    public void setOptimizationValue(Double optimizationValue)
    {
        _optimizationValue = optimizationValue;
    }

    public String getOptimizationType()
    {
        return _optimizationType;
    }

    public void setOptimizationType(String optimizationType)
    {
        _optimizationType = optimizationType;
    }
}
