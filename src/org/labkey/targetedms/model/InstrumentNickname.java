package org.labkey.targetedms.model;

import org.labkey.api.data.Container;

public class InstrumentNickname
{
    private long _id;
    private String _nickname;
    private String _model;
    private String _serialNumber;
    private Container _container;

    public long getId()
    {
        return _id;
    }

    public void setId(long id)
    {
        _id = id;
    }

    public String getNickname()
    {
        return _nickname;
    }

    public void setNickname(String nickname)
    {
        _nickname = nickname;
    }

    public String getModel()
    {
        return _model;
    }

    public void setModel(String model)
    {
        _model = model;
    }

    public String getSerialNumber()
    {
        return _serialNumber;
    }

    public void setSerialNumber(String serialNumber)
    {
        _serialNumber = serialNumber;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }
}
