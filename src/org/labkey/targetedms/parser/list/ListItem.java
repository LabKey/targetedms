package org.labkey.targetedms.parser.list;

import org.labkey.targetedms.parser.SkylineEntity;

public class ListItem extends SkylineEntity
{
    private int _listDefinitionId;

    public int getListDefinitionId()
    {
        return _listDefinitionId;
    }

    public void setListDefinitionId(int listId)
    {
        _listDefinitionId = listId;
    }
}
