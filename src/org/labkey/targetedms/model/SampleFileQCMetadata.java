/*
 * Copyright (c) 2022-2026 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.targetedms.parser.SampleFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SampleFileQCMetadata extends SampleFile
{
    boolean inGuideSetTrainingRange;
    private Set<Integer> _ignoredMetricIds = Collections.emptySet();
    private String _replicateName;

    // Use -1 to signify that an exclusion is for the whole sample (and therefore applies to all metrics)
    // See GROUP_CONCAT in SampleFileForQC.sql
    private static final int ALL_METRICS = -1;

    public boolean isIgnoreInQC(int metricId)
    {
        return _ignoredMetricIds.contains(metricId) || _ignoredMetricIds.contains(ALL_METRICS);
    }

    public String getExcludedMetricIds(String ignoredMetricIds)
    {
        return StringUtils.join(_ignoredMetricIds, ",");
    }

    public void setExcludedMetricIds(String excludedMetricIds)
    {
        if (excludedMetricIds == null)
        {
            _ignoredMetricIds = Collections.emptySet();
        }
        else
        {
            _ignoredMetricIds = Arrays.stream(excludedMetricIds.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        }
    }

    public boolean isInGuideSetTrainingRange()
    {
        return inGuideSetTrainingRange;
    }

    public void setInGuideSetTrainingRange(boolean inGuideSetTrainingRange)
    {
        this.inGuideSetTrainingRange = inGuideSetTrainingRange;
    }

    public String getReplicateName()
    {
        return _replicateName;
    }

    public void setReplicateName(String replicateName)
    {
        _replicateName = replicateName;
    }

    @Override
    public SampleFileInfo toSampleFileInfo()
    {
        SampleFileInfo result = super.toSampleFileInfo();
        result.setInGuideSetTrainingRange(isInGuideSetTrainingRange());
        result.setIgnoreForAllMetric(_ignoredMetricIds.contains(ALL_METRICS));
        result.setReplicateName(getReplicateName());
        return result;
    }
}
