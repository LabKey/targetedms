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
package org.labkey.targetedms.outliers;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.MathUtils;
import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.targetedms.model.RawMetricDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MovingRangeOutliers
{
    public Map<GuideSetStats.Key, GuideSetStats> getGuideSetAvgMRs(List<RawMetricDataSet> rawGuideSet)
    {
        Map<GuideSetStats.Key, List<Double>> guideSetDataMap = new LinkedHashMap<>();

        // Bucket the values based on guide set, series, and metric type
        for (RawMetricDataSet row : rawGuideSet)
        {
            GuideSetStats.Key key = new GuideSetStats.Key(row.getGuideSetId(), row.getSeriesLabel(), row.getSeriesType());

            List<Double> metricValues = guideSetDataMap.computeIfAbsent(key, x -> new ArrayList<>());
            if (row.getMetricValue() != null)
            {
                metricValues.add(row.getMetricValue());
            }
        }

        Map<GuideSetStats.Key, GuideSetStats> result = new HashMap<>();

        guideSetDataMap.forEach((key, metricVals) -> {
            if (metricVals == null || metricVals.size() == 0)
                return;

            Double[] values = metricVals.toArray(new Double[0]);

            GuideSetStats stats = new GuideSetStats(key, values);
            result.put(key, stats);
        });

        return result;
    }
}
