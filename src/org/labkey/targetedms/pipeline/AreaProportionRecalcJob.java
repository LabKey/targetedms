package org.labkey.targetedms.pipeline;

import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.TableSelector;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;

import java.io.File;

public class AreaProportionRecalcJob extends PipelineJob
{
    @SuppressWarnings("unused")  // for serialization
    protected AreaProportionRecalcJob()
    {

    }

    public AreaProportionRecalcJob(ViewBackgroundInfo info, @NotNull PipeRoot root)
    {
        super(TargetedMSPipelineProvider.name, info, root);
        setLogFile(new File(root.getRootPath(), FileUtil.makeFileNameWithTimestamp("AreaProportionRecalcJob", "log")));
    }

    @Override
    public void run()
    {
        setStatus(TaskStatus.running);
        TableSelector selector = new TableSelector(TargetedMSManager.getTableInfoRuns());
        long totalRuns = selector.getRowCount();

        getLogger().info("Starting to recalculate area proportions for " + totalRuns + " Skyline documents");

        MutableLong count = new MutableLong(0);

        selector.forEach(TargetedMSRun.class, run ->
        {
            if (count.incrementAndGet() % 100 == 0)
            {
                getLogger().info("Updating Skyline document " + count);
            }
            TargetedMSManager.updateModifiedAreaProportions(null, run);
        });

        getLogger().info("All done!");
        setStatus(TaskStatus.complete);
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Recalculating area proportions";
    }
}
