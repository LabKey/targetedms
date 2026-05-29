/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.labkey.api.data.Container;

public class InstrumentNickname
{
    private long _id;
    private String _nickname;
    private String _model;
    private String _serialNumber;
    private Container _container;

    public long getId()
    {
        return _id;
    }

    public void setId(long id)
    {
        _id = id;
    }

    public String getNickname()
    {
        return _nickname;
    }

    public void setNickname(String nickname)
    {
        _nickname = nickname;
    }

    public String getModel()
    {
        return _model;
    }

    public void setModel(String model)
    {
        _model = model;
    }

    public String getSerialNumber()
    {
        return _serialNumber;
    }

    public void setSerialNumber(String serialNumber)
    {
        _serialNumber = serialNumber;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }
}
