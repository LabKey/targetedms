/*
 * Copyright (c) 2020-2026 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.targetedms.model.QCMetricConfiguration;

import java.util.Objects;

public class GuideSetKey
{
    private final QCMetricConfiguration _metric;

    private final int _guideSetId;
    private final String _seriesLabel;
    private final int _hashCode;

    public GuideSetKey(QCMetricConfiguration metric, int guideSetId, String seriesLabel)
    {
        _metric = metric;
        _guideSetId = guideSetId;
        _seriesLabel = seriesLabel;
        _hashCode = Objects.hash(_metric.getId(), _guideSetId, _seriesLabel);
    }

    public int getMetricId()
    {
        return _metric.getId();
    }

    @NotNull
    public QCMetricConfiguration getMetric()
    {
        return _metric;
    }

    public int getGuideSetId()
    {
        return _guideSetId;
    }

    public String getSeriesLabel()
    {
        return _seriesLabel;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuideSetKey that = (GuideSetKey) o;
        return _metric.getId() == that._metric.getId() &&
                _guideSetId == that._guideSetId &&
                Objects.equals(_seriesLabel, that._seriesLabel);
    }

    @Override
    public int hashCode()
    {
        return _hashCode;
    }

    @Override
    public String toString()
    {
        return "GuideSet: " + getGuideSetId() + ", Metric: " + _metric.getId() + ", Series: " + _seriesLabel;
    }
}
