package org.labkey.targetedms.parser.list;

import org.labkey.targetedms.parser.SkylineEntity;

public class ListDefinition extends SkylineEntity
{
    private int _runId;
    private String _name;
    private Integer _pkColumnIndex;
    private Integer _displayColumnIndex;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public Integer getPkColumnIndex()
    {
        return _pkColumnIndex;
    }

    public void setPkColumnIndex(Integer pkColumnIndex)
    {
        _pkColumnIndex = pkColumnIndex;
    }

    public Integer getDisplayColumnIndex()
    {
        return _displayColumnIndex;
    }

    public void setDisplayColumnIndex(Integer displayColumnIndex)
    {
        _displayColumnIndex = displayColumnIndex;
    }

    public String getUserSchemaTableName()
    {
        return getRunId() + "-" + getName();
    }
}
