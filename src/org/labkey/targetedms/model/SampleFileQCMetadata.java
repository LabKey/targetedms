package org.labkey.targetedms.model;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.targetedms.parser.SampleFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SampleFileQCMetadata extends SampleFile
{
    boolean inGuideSetTrainingRange;
    private Set<Integer> _ignoredMetricIds = Collections.emptySet();

    @Nullable
    public boolean isIgnoreInQC(int metricId)
    {
        return _ignoredMetricIds.contains(metricId) || _ignoredMetricIds.contains(-1);
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
}
