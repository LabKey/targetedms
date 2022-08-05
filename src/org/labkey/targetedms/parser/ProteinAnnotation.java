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
public class ProteinAnnotation extends AbstractAnnotation
{
    private long _proteinId;

    public long getProteinId()
    {
        return _proteinId;
    }

    public void setProteinId(long proteinId)
    {
        _proteinId = proteinId;
    }
}
