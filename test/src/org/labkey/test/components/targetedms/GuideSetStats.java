/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.components.targetedms;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cnathe on 4/29/15.
 */
public class GuideSetStats
{
    private String _queryName;
    private int _numRecords;
    private String _precursor;
    private Double _mean;
    private Double _stdDev;

    public GuideSetStats(String queryName, int numRecords)
    {
        _queryName = queryName;
        _numRecords = numRecords;
    }

    public GuideSetStats(String queryName, int numRecords, String precursor, Double mean, Double stdDev)
    {
        this(queryName, numRecords);
        _precursor = precursor;
        _mean = mean;
        _stdDev = stdDev;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    public int getNumRecords()
    {
        return _numRecords;
    }

    public String getPrecursor()
    {
        return _precursor;
    }

    public Double getMean()
    {
        return _mean;
    }

    public Double getStdDev()
    {
        return _stdDev;
    }
}
