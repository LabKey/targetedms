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

import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.GuideSetAvgMR;
import org.labkey.targetedms.model.RawGuideSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MovingRangeOutliers
{
    public List<GuideSetAvgMR> getGuideSetAvgMRs(List<RawGuideSet> rawGuideSet)
    {
        Map<GuideSetAvgMR, List<Double>> guideSetDataMap = new LinkedHashMap<>();
        List<GuideSetAvgMR> movingRangeMap = new ArrayList<>();

        for (RawGuideSet row : rawGuideSet)
        {
            GuideSetAvgMR guideSetAvgMR = new GuideSetAvgMR(row.getGuideSetId(), row.getSeriesLabel(), row.getSeriesType());

            if (guideSetDataMap.get(guideSetAvgMR) == null)
            {
                List<Double> metValues = new ArrayList<>();
                if (row.getMetricValue() != null)
                {
                    rawGuideSet.forEach(gs -> {
                        String label = gs.getSeriesLabel();
                        int guideSetId = gs.getGuideSetId();
                        if (guideSetAvgMR.getSeriesLabel().equals(label) && guideSetAvgMR.getGuideSetid() == guideSetId)
                        {
                            if (gs.getMetricValue() != null)
                                metValues.add(gs.getMetricValue());
                        }
                    });
                }
                guideSetDataMap.put(guideSetAvgMR, metValues);
            }
        }

        guideSetDataMap.forEach((guideSetAvgMR, metricVals) -> {
            if (metricVals == null || metricVals.size() == 0)
                return;

            Double[] mVals = metricVals.toArray(new Double[0]);
            Double[] metricMovingRanges = Stats.getMovingRanges(mVals, false, null);

            double mean = Stats.getMean(metricMovingRanges);
            double standardDev = Stats.getStdDev(metricMovingRanges);

            guideSetAvgMR.setAverage(mean);
            guideSetAvgMR.setStandardDev(standardDev);

            movingRangeMap.add(guideSetAvgMR);

        });

        return movingRangeMap;
    }
}
