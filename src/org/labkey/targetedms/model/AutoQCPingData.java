/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
