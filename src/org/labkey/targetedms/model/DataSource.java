package org.labkey.targetedms.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.targetedms.parser.SkylineEntity;
import java.util.Date;

public class DataSource extends SkylineEntity
{
    private Container _container;
    private Date _created;
    private int _createdBy;
    private int _dataId; // FK to exp.data's RowId column
    private long _size;
    private String _instrumentType;

    public DataSource() {}

    public DataSource(@NotNull Container container, int dataId, long size, @Nullable String instrumentType)
    {
        _container = container;
        _dataId = dataId;
        _size = size;
        _instrumentType = instrumentType;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public int getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(int createdBy)
    {
        _createdBy = createdBy;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public long getSize()
    {
        return _size;
    }

    public void setSize(long size)
    {
        _size = size;
    }

    public String getInstrumentType()
    {
        return _instrumentType;
    }

    public void setInstrumentType(String instrumentType)
    {
        _instrumentType = instrumentType;
    }
}
