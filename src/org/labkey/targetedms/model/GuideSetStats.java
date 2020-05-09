/*
 * Copyright (c) 2019 LabKey Corporation
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

import org.labkey.api.visualization.Stats;

import java.util.ArrayList;
import java.util.List;

public class GuideSetStats
{
    private final GuideSetKey _key;
    private final GuideSet _guideSet;

    /** Rows that define the normal range for this guide set */
    private final List<RawMetricDataSet> _trainingRows = new ArrayList<>();
    /** Rows that reference the training values */
    private final List<RawMetricDataSet> _referenceRows = new ArrayList<>();

    public GuideSetKey getKey()
    {
        return _key;
    }

    private double _standardDeviation;
    private double _average;

    private double _movingRangeAverage;

    private boolean _locked = false;

    public GuideSetStats(GuideSetKey key, GuideSet guideSet)
    {
        _key = key;
        _guideSet = guideSet;
    }

    public double getStandardDeviation()
    {
        assertLocked();
        return _standardDeviation;
    }

    private void assertLocked()
    {
        if (!_locked)
        {
            throw new IllegalStateException("Stats have not yet been calculated");
        }
    }

    public double getAverage()
    {
        assertLocked();
        return _average;
    }

    public double getMovingRangeAverage()
    {
        assertLocked();
        return _movingRangeAverage;
    }

    public void addRow(RawMetricDataSet row)
    {
        if (_locked)
        {
            throw new IllegalStateException("Stats have already been locked");
        }
        if (!row.isIgnoreInQC() &&
                _guideSet.getTrainingStart().compareTo(row.getAcquiredTime()) <= 0 &&
                (_guideSet.getTrainingEnd() == null || _guideSet.getTrainingEnd().compareTo(row.getAcquiredTime()) >= 0))
        {
            _trainingRows.add(row);
        }
        else
        {
            _referenceRows.add(row);
        }
    }

    private Double[] getValues(List<RawMetricDataSet> rows, boolean transformNullsToZero, boolean roundValues)
    {
        List<Double> result = new ArrayList<>();
        for (RawMetricDataSet row : rows)
        {
            Double value = row.getMetricValue();
            if (value == null)
            {
                if (transformNullsToZero)
                {
                    result.add(0.0d);
                }
            }
            else if (roundValues)
            {
                result.add((double) Math.round(value * 10000.0d) / 10000.0d);
            }
            else
            {
                result.add(value.doubleValue());
            }
        }

        return result.toArray(new Double[0]);
    }

    public void calculateStats()
    {
        _locked = true;

        Double[] trainingValues = getValues(_trainingRows, false, false);

        _average = Stats.getMean(trainingValues);
        _standardDeviation = Stats.getStdDev(trainingValues, true);

        Double[] movingRanges = Stats.getMovingRanges(trainingValues, false, null);
        _movingRangeAverage = Stats.getMean(movingRanges);

        List<RawMetricDataSet> allRows = new ArrayList<>(_trainingRows.size() + _referenceRows.size());
        allRows.addAll(_trainingRows);
        allRows.addAll(_referenceRows);

        Double[] metricVals = getValues(allRows, true, true);

        Double[] mRs = Stats.getMovingRanges(metricVals, false, null);

        double[] positiveCUSUMm = Stats.getCUSUMS(metricVals, false, false, false, null);
        double[] negativeCUSUMm = Stats.getCUSUMS(metricVals, true, false, false, null);

        double[] positiveCUSUMv = Stats.getCUSUMS(metricVals, false, true, false, null);
        double[] negativeCUSUMv = Stats.getCUSUMS(metricVals, true, true, false, null);

        for (int i = 0; i < allRows.size(); i++)
        {
            RawMetricDataSet row = allRows.get(i);
            row.setmR(mRs[i]);
            row.setCUSUMmP(positiveCUSUMm[i]);
            row.setCUSUMmN(negativeCUSUMm[i]);
            row.setCUSUMvP(positiveCUSUMv[i]);
            row.setCUSUMvN(negativeCUSUMv[i]);
        }
    }
}
