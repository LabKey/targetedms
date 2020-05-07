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

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.labkey.api.visualization.Stats;

import java.util.Objects;

public class GuideSetStats
{
    private final Key _key;

    public Key getKey()
    {
        return _key;
    }

    public static class Key
    {
        private final int guideSetId;
        private final String seriesLabel;
        private final String seriesType;

        public Key(int guideSetId, String seriesLabel, String seriesType)
        {
            this.guideSetId = guideSetId;
            this.seriesLabel = seriesLabel;
            this.seriesType = seriesType;
        }

        public int getGuideSetId()
        {
            return guideSetId;
        }

        public String getSeriesLabel()
        {
            return seriesLabel;
        }

        public String getSeriesType()
        {
            return seriesType;
        }

        @Override
        public String toString()
        {
            return getGuideSetId() + ": " + seriesType + ": " + seriesLabel;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return guideSetId == key.guideSetId &&
                    Objects.equals(seriesLabel, key.seriesLabel) &&
                    Objects.equals(seriesType, key.seriesType);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(guideSetId, seriesLabel, seriesType);
        }
    }

    private final double _standardDeviation;
    private final double _average;

    private final double _movingRangeAverage;
    private final double _movingRangeSD;

    public GuideSetStats(Key key, Double[] values)
    {
        _key = key;

        _average = Stats.getMean(values);
        _standardDeviation = Stats.getStdDev(values, true);

        Double[] movingRanges = Stats.getMovingRanges(values, false, null);
        _movingRangeAverage = Stats.getMean(movingRanges);
        _movingRangeSD = Stats.getStdDev(movingRanges, true);
    }

    public double getStandardDeviation()
    {
        return _standardDeviation;
    }

    public double getAverage()
    {
        return _average;
    }

    public double getMovingRangeAverage()
    {
        return _movingRangeAverage;
    }

    public double getMovingRangeSD()
    {
        return _movingRangeSD;
    }
}
