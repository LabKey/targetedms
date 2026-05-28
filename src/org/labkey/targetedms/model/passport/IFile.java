/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.targetedms.model.passport;

import org.labkey.api.data.Container;

import java.util.Date;

public class IFile
{
    String fileName;

    public Container getContainer()
    {
        return container;
    }

    public void setContainer(Container container)
    {
        this.container = container;
    }

    Container container;

    public long getRunId()
    {
        return runId;
    }

    public void setRunId(long runId)
    {
        this.runId = runId;
    }

    private long runId;

    public int getPepGroupId()
    {
        return pepGroupId;
    }

    public void setPepGroupId(int pepGroupId)
    {
        this.pepGroupId = pepGroupId;
    }

    private int pepGroupId;

    public Date getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate)
    {
        this.createdDate = createdDate;
    }

    Date createdDate;

    public Date getModifiedDate()
    {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate)
    {
        this.modifiedDate = modifiedDate;
    }

    Date modifiedDate;

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getSoftwareVersion()
    {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion)
    {
        this.softwareVersion = softwareVersion;
    }



    String softwareVersion;
}
