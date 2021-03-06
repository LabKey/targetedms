/*
 * Copyright (c) 2012-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.parser;

import com.google.common.primitives.Floats;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Chromatogram data associated with a ChromGroupHeaderInfo.
 */
public class Chromatogram
{
    protected List<TimeIntensities> _transitionTimeIntensities;
    private final SourceStatus _status;
    private float[] _mergedTimes;
    private TimeIntensities[] _mergedTimeIntensities;

    public Chromatogram(List<TimeIntensities> transitionTimeIntensities, SourceStatus status)
    {
        _transitionTimeIntensities = transitionTimeIntensities;
        _status = status;
    }

    public int getTransitionsCount()
    {
        return getTransitionTimeIntensities().size();
    }

    public enum SourceStatus
    {
        dbOnly("DB contains bytes, but not offset so impossible to retrieve directly from file"),
        diskOnly("DB does not contain bytes, but does have indices and data was retrieved successfully from file"),
        noSkydResolved("DB contains bytes and offset, but we can't resolve the SKYD path, perhaps because the S3 bucket is no longer configured"),
        skydMissing("DB contains bytes and offset, but we can't find the SKYD file that's referenced"),
        mismatch("Both DB-based and file-based bytes available, but they didn't match"),
        match("Both DB-based and file-based bytes available, and they match");

        private final String _description;

        SourceStatus(String description)
        {
            _description = description;
        }

        public String getDescription()
        {
            return _description;
        }
    }

    public SourceStatus getStatus()
    {
        return _status;
    }

    /**
     * Returns a set of times which can be shared by all of the transitions in this chromatogram group.
     * If the transitions have different times than each other, then returns the union of all
     * possible time values.
     */
    public float[] getTimes()
    {
        if (_mergedTimes == null) {
            if (allSameTimes()) {
                _mergedTimes = getTransitionTimeIntensities().get(0).getTimes();
            } else {
                _mergedTimes = getAllTimesMerged();
            }
        }
        return _mergedTimes;
    }

    public float[] getIntensities(int index)
    {
        if (_mergedTimeIntensities == null) {
            _mergedTimeIntensities = new TimeIntensities[getTransitionTimeIntensities().size()];
        }
        TimeIntensities timeIntensities = _mergedTimeIntensities[index];
        if (timeIntensities == null) {
            _mergedTimeIntensities[index] = timeIntensities = getTransitionTimeIntensities().get(index).interpolate(getTimes());
        }
        return timeIntensities.getIntensities();
    }

    public List<TimeIntensities> getTransitionTimeIntensities()
    {
        return Collections.unmodifiableList(_transitionTimeIntensities);
    }

    /**
     * Returns true if all of the transition time intensities have exactly the same set of times.
     */
    public boolean allSameTimes()
    {
        float[] firstTimes = null;
        for (TimeIntensities timeIntensities : _transitionTimeIntensities) {
            if (firstTimes == null) {
                firstTimes = timeIntensities.getTimes();
            }
            if (firstTimes == timeIntensities.getTimes()) {
                continue;
            }
            if (!Arrays.equals(firstTimes, timeIntensities.getTimes())) {
                return false;
            }
        }
        return true;
    }

    private float[] getAllTimesMerged() {
        HashSet<Float> allTimes = new HashSet<>();
        for (TimeIntensities timeIntensities : _transitionTimeIntensities) {
            for (float time : timeIntensities.getTimes()) {
                allTimes.add(time);
            }
        }
        float[] times = Floats.toArray(allTimes);
        Arrays.sort(times);
        return times;
    }
}
