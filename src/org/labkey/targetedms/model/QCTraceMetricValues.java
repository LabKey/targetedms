/*
 * Copyright (c) 2021-2026 LabKey Corporation
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

public class QCTraceMetricValues
{
    private int _id;
    private int _metric;
    private float _value;
    private long _sampleFileId;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getMetric()
    {
        return _metric;
    }

    public void setMetric(int metric)
    {
        _metric = metric;
    }

    public float getValue()
    {
        return _value;
    }

    public void setValue(float value)
    {
        _value = value;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }
}
