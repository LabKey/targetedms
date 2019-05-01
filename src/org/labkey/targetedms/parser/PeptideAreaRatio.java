/*
 * Copyright (c) 2012 LabKey Corporation
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
public class PeptideAreaRatio extends AreaRatio
{
    private int _peptideChromInfoId;
    private int _peptideChromInfoStdId;

    public int getPeptideChromInfoId()
    {
        return _peptideChromInfoId;
    }

    public void setPeptideChromInfoId(int peptideChromInfoId)
    {
        _peptideChromInfoId = peptideChromInfoId;
    }

    public int getPeptideChromInfoStdId()
    {
        return _peptideChromInfoStdId;
    }

    public void setPeptideChromInfoStdId(int peptideChromInfoStdId)
    {
        _peptideChromInfoStdId = peptideChromInfoStdId;
    }
}
