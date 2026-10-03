/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.targetedms.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.CancelledException;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/** Backfills PTMPercentsGroupedPrepivotCache for existing ExperimentMAM runs, skipping runs that already have cache rows. */
public class PTMPercentsCachePopulationJob extends PipelineJob
{
    @JsonCreator
    protected PTMPercentsCachePopulationJob()
    {
    }

    public PTMPercentsCachePopulationJob(ViewBackgroundInfo info, PipeRoot root)
    {
        super(TargetedMSPipelineProvider.name, info, root);
        setLogFile(root.resolvePathToFileLike(FileUtil.makeFileNameWithTimestamp("PTMPercentsCachePopulation", "log")));
    }

    @Override
    public void run()
    {
        setStatus(TaskStatus.running);
        try
        {
            populateAll(getLogger(), (done, total) -> setStatus(TaskStatus.running, done + "/" + total + " runs"));
            setStatus(TaskStatus.complete);
        }
        catch (CancelledException e)
        {
            setActiveTaskStatus(TaskStatus.cancelled);
        }
        catch (Exception e)
        {
            error("Unexpected error populating PTMPercentsGroupedPrepivotCache", e);
            setStatus(TaskStatus.error);
        }
    }

    /** Safe to rerun: runs that already have cache rows are skipped. */
    public static void populateAll(Logger log, @Nullable BiConsumer<Integer, Integer> progress)
    {
        // Not getUser(): a deserialized (retried) job would resolve the upgrade's search user to Guest and read nothing
        User user = User.getSearchUser();
        List<TargetedMSRun> runs = Arrays.stream(TargetedMSManager.getAllNonDeletedRuns())
                .filter(run -> run.getContainer() != null && TargetedMSManager.getFolderType(run.getContainer()) == TargetedMSService.FolderType.ExperimentMAM)
                .toList();
        log.info("Populating PTMPercentsGroupedPrepivotCache for {} runs in ExperimentMAM folders", runs.size());

        long start = System.currentTimeMillis();
        long totalRows = 0;
        int failures = 0;
        for (int i = 0; i < runs.size(); i++)
        {
            TargetedMSRun run = runs.get(i);
            Container container = run.getContainer();
            int done = i + 1;
            long runStart = System.currentTimeMillis();
            try
            {
                int rows = TargetedMSManager.populatePTMPercentsGroupedPrepivotCache(run, user, container, false);
                totalRows += rows;
                long now = System.currentTimeMillis();
                long elapsed = now - start;
                long eta = elapsed / done * (runs.size() - done);
                log.info("PTMPercentsGroupedPrepivotCache {}/{} ({}%): run {} in {}, {} rows in {}. Elapsed {}, ETA {}",
                        done, runs.size(), done * 100 / runs.size(), run.getId(), container.getPath(), rows,
                        DateUtil.formatDuration(now - runStart), DateUtil.formatDuration(elapsed), DateUtil.formatDuration(eta));
            }
            catch (CancelledException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                failures++;
                log.error("Error populating PTMPercentsGroupedPrepivotCache for run {} in {} ({}/{})", run.getId(), container.getPath(), done, runs.size(), e);
            }
            if (progress != null)
                progress.accept(done, runs.size());
        }

        log.info("Finished populating PTMPercentsGroupedPrepivotCache: {} runs, {} rows, {} failures, {}",
                runs.size(), totalRows, failures, DateUtil.formatDuration(System.currentTimeMillis() - start));
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Populate PTM percents cache for existing ExperimentMAM runs";
    }
}
