/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
package org.labkey.api.targetedms;

import org.labkey.api.data.Container;

import java.util.Date;

/**
 * User: vsharma
 * Date: 8/26/2015
 * Time: 2:13 PM
 */
public interface ITargetedMSRun
{
    public Container getContainer();
    public String getBaseName();
    public String getFileName();
    public String getDescription();
    public Date getCreated();
    public long getId();
    public Integer getDataId();
    public Integer getSkydDataId();
    public String getSoftwareVersion();
}
