package org.labkey.targetedms.parser;

public class OptimizationInfo
{
    private String _peptideModSeq;
    private int _charge;
    private String _fragmentIon;
    private int _productCharge;
    private double _value;
    private String _type;

    public String getPeptideModSeq()
    {
        return _peptideModSeq;
    }

    public void setPeptideModSeq(String peptideModSeq)
    {
        _peptideModSeq = peptideModSeq;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public String getFragmentIon()
    {
        return _fragmentIon;
    }

    public void setFragmentIon(String fragmentIon)
    {
        _fragmentIon = fragmentIon;
    }

    public int getProductCharge()
    {
        return _productCharge;
    }

    public void setProductCharge(int productCharge)
    {
        _productCharge = productCharge;
    }

    public double getValue()
    {
        return _value;
    }

    public void setValue(double value)
    {
        _value = value;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        _type = type;
    }
}
