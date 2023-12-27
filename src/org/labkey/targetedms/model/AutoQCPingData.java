package org.labkey.targetedms.model;

import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;

import java.util.Date;
import java.util.Map;

public class AutoQCPingData
{
    private Container _container;
    private Date _created;
    private int _createdBy;
    private Date _modified;
    private int _modifiedBy;
    private String _softwareVersion;

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public String getSoftwareVersion()
    {
        return _softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion)
    {
        _softwareVersion = softwareVersion;
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

    public Date getModified()
    {
        return _modified;
    }

    public void setModified(Date modified)
    {
        _modified = modified;
    }

    public int getModifiedBy()
    {
        return _modifiedBy;
    }

    public void setModifiedBy(int modifiedBy)
    {
        _modifiedBy = modifiedBy;
    }

    public Map<String, Object> toMap()
    {
        Map<String, Object> map = new CaseInsensitiveHashMap<>();
        map.put("container", getContainer().getId());
        map.put("softwareVersion", getSoftwareVersion());
        map.put("modified", getModified());
        map.put("modifiedBy", getModifiedBy());
        map.put("created", getCreated());
        map.put("createdBy", getCreatedBy());

        return map;
    }
}
