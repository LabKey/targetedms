package org.labkey.targetedms.parser.list;

import org.labkey.targetedms.parser.DataSettings;
import org.labkey.targetedms.parser.SkylineEntity;

public class ListColumn extends SkylineEntity
{
    private int _listDefinitionId;
    private int _columnIndex;
    private String _name;
    private String _lookup;
    private String _annotationType;

    public int getListDefinitionId()
    {
        return _listDefinitionId;
    }

    public void setListDefinitionId(int listId)
    {
        _listDefinitionId = listId;
    }

    public int getColumnIndex()
    {
        return _columnIndex;
    }

    public void setColumnIndex(int columnIndex)
    {
        _columnIndex = columnIndex;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getLookup()
    {
        return _lookup;
    }

    public void setLookup(String lookup)
    {
        _lookup = lookup;
    }

    public String getAnnotationType()
    {
        return _annotationType;
    }

    public void setAnnotationType(String annotationType)
    {
        _annotationType = annotationType;
    }

    public DataSettings.AnnotationType getAnnotationTypeEnum() {
        return DataSettings.AnnotationType.fromString(getAnnotationType());
    }
}
