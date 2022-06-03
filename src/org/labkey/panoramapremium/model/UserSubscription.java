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
