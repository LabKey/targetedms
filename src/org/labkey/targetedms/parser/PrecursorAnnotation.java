/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.targetedms.parser;

/**
 * User: jeckels
 * Date: Jun 4, 2012
 */
public class PrecursorAnnotation extends AbstractAnnotation
{
    private long _precursorId;
    private long _generalPrecursorId;
    public long getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(long precursorId)
    {
        _precursorId = precursorId;
    }

    public long getGeneralPrecursorId()
    {
        return _generalPrecursorId;
    }

    public void setGeneralPrecursorId(long generalPrecursorId)
    {
        _generalPrecursorId = generalPrecursorId;
    }
}
