/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.panoramapremium.model;

public class UserSubscription
{
    int userId;
    boolean enabled;
    Integer samples;
    Integer outliers;

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Integer getSamples()
    {
        return samples;
    }

    public void setSamples(Integer sampleFiles)
    {
        this.samples = sampleFiles;
    }

    public Integer getOutliers()
    {
        return outliers;
    }

    public void setOutliers(Integer outliers)
    {
        this.outliers = outliers;
    }
}
