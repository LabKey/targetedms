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
 * User: vsharma
 * Date: 7/24/12
 * Time: 3:00 PM
 */
public class PrecursorAreaRatio extends AreaRatio
{
    private long _precursorChromInfoId;
    private long _precursorChromInfoStdId;

    public long getPrecursorChromInfoId()
    {
        return _precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(long precursorChromInfoId)
    {
        _precursorChromInfoId = precursorChromInfoId;
    }

    public long getPrecursorChromInfoStdId()
    {
        return _precursorChromInfoStdId;
    }

    public void setPrecursorChromInfoStdId(long precursorChromInfoStdId)
    {
        _precursorChromInfoStdId = precursorChromInfoStdId;
    }
}
