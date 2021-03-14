package org.labkey.targetedms.model;

public class QCTraceMetricValues
{
    private int _id;
    private int _metric;
    private float _value;
    private int _sampleFile;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getMetric()
    {
        return _metric;
    }

    public void setMetric(int metric)
    {
        _metric = metric;
    }

    public float getValue()
    {
        return _value;
    }

    public void setValue(float value)
    {
        _value = value;
    }

    public int getSampleFile()
    {
        return _sampleFile;
    }

    public void setSampleFile(int sampleFile)
    {
        _sampleFile = sampleFile;
    }
}
