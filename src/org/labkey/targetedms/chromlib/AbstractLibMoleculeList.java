package org.labkey.targetedms.chromlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractLibMoleculeList<ChildType extends AbstractLibMolecule> extends AbstractLibEntity
{
    private String _name;
    private String _description;

    private List<ChildType> _children = new ArrayList<>();

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public void addChild(ChildType child)
    {
        _children.add(child);
    }

    List<ChildType> getChildren()
    {
        return Collections.unmodifiableList(_children);
    }

    @Override
    public int getCacheSize()
    {
        return super.getCacheSize() + getChildren().stream().mapToInt(AbstractLibEntity::getCacheSize).sum();
    }
}
