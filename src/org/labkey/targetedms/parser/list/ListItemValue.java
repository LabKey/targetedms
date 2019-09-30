package org.labkey.targetedms.parser.list;

import org.labkey.targetedms.parser.SkylineEntity;

public class ListItemValue extends SkylineEntity
{
    private int _listItemId;
    private int _columnIndex;
    private String _textValue;
    private Double _numericValue;

    public int getListItemId()
    {
        return _listItemId;
    }

    public void setListItemId(int listItemId)
    {
        _listItemId = listItemId;
    }

    public int getColumnIndex()
    {
        return _columnIndex;
    }

    public void setColumnIndex(int columnIndex)
    {
        _columnIndex = columnIndex;
    }

    public String getTextValue()
    {
        return _textValue;
    }

    public void setTextValue(String textValue)
    {
        _textValue = textValue;
    }

    public Double getNumericValue()
    {
        return _numericValue;
    }

    public void setNumericValue(Double numericValue)
    {
        _numericValue = numericValue;
    }

    public void setValue(Object value)
    {
        if (value == null)
        {
            setTextValue(null);
            setNumericValue(null);
            return;
        }
        if (value instanceof String)
        {
            setTextValue((String) value);
            return;
        }
        if (value instanceof Number)
        {
            setNumericValue(((Number)value).doubleValue());
            return;
        }
        if (value instanceof Boolean)
        {
            setNumericValue(((Boolean) value)?1.0:0.0);
            return;
        }
        throw new IllegalArgumentException();
    }
}
