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
package org.labkey.targetedms.chromlib;

import java.util.Objects;

public class LibPredictor extends AbstractLibEntity
{
    private String _name;
    private Double _stepSize;
    private Integer _stepCount;

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public Double getStepSize()
    {
        return _stepSize;
    }

    public void setStepSize(Double stepSize)
    {
        _stepSize = stepSize;
    }

    public Integer getStepCount()
    {
        return _stepCount;
    }

    public void setStepCount(Integer stepCount)
    {
        _stepCount = stepCount;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibPredictor that = (LibPredictor) o;
        return Objects.equals(_name, that._name) && Objects.equals(_stepSize, that._stepSize) && Objects.equals(_stepCount, that._stepCount);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_name, _stepSize, _stepCount);
    }
}
