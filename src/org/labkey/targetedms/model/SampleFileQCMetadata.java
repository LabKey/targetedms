package org.labkey.targetedms.model;

import org.jetbrains.annotations.Nullable;
import org.labkey.targetedms.parser.SampleFile;

public class SampleFileQCMetadata extends SampleFile
{
    boolean ignoreInQC;
    boolean inGuideSetTrainingRange;

    @Nullable
    public boolean isIgnoreInQC()
    {
        return ignoreInQC;
    }

    public void setIgnoreInQC(boolean ignoreInQC)
    {
        this.ignoreInQC = ignoreInQC;
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
