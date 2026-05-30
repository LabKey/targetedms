/*
 * Copyright (c) 2013-2026 LabKey Corporation
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
package org.labkey.targetedms;

import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.targetedms.query.QCAnnotationTypeTable;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * User: jeckels
 * Date: 5/13/13
 */
public class TargetedMSUpgradeCode implements UpgradeCode
{
    private static final Logger LOG = LogHelper.getLogger(TargetedMSUpgradeCode.class, "TargetedMS schema upgrade operations");

    // called at every bootstrap to initialize annotation types
    @SuppressWarnings({"UnusedDeclaration"})
    public void populateDefaultAnnotationTypes(final ModuleContext moduleContext)
    {
        if (ModuleLoader.getInstance().shouldInsertData())
        {
            insertAnnotationType("Instrumentation Change", "FF0000", moduleContext.getUpgradeUser());
            insertAnnotationType("Reagent Change", "00FF00", moduleContext.getUpgradeUser());
            insertAnnotationType("Technician Change", "0000FF", moduleContext.getUpgradeUser());
            insertAnnotationType(QCAnnotationTypeTable.INSTRUMENT_DOWNTIME, "CCCC00", moduleContext.getUpgradeUser());

            // Enable the module in the /Shared container so that it can be resolved
            Set<Module> activeModules = new HashSet<>(ContainerManager.getSharedContainer().getActiveModules());
            activeModules.add(ModuleLoader.getInstance().getModule(TargetedMSModule.class));
            ContainerManager.getSharedContainer().setActiveModules(activeModules);
        }
    }

    private void insertAnnotationType(String name, String color, User user)
    {
        SQLFragment sql = new SQLFragment("INSERT INTO ");
        sql.append(TargetedMSManager.getTableInfoQCAnnotationType());
        sql.append(" (Container, Created, Modified, CreatedBy, ModifiedBy, Name, Color) VALUES (?, ?, ?, ?, ?, ?, ?)");
        sql.add(ContainerManager.getSharedContainer());
        Date now = new Date();
        sql.add(now);
        sql.add(now);
        sql.add(user.getUserId());
        sql.add(user.getUserId());
        sql.add(name);
        sql.add(color);
        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    /** Populate PTMPercentsGroupedPrepivotCache for all runs in existing ExperimentMAM folders */
    @SuppressWarnings("UnusedDeclaration")
    @DeferredUpgrade
    public void populatePTMPercentsGroupedPrepivotCache(final ModuleContext moduleContext)
    {
        if (moduleContext.isNewInstall())
        {
            return;
        }

        User user = moduleContext.getUpgradeUser();
        LOG.info("Populating PTMPercentsGroupedPrepivotCache for existing ExperimentMAM folders");

        for (TargetedMSRun run : TargetedMSManager.getAllNonDeletedRuns())
        {
            Container container = run.getContainer();
            if (container != null && TargetedMSManager.getFolderType(container) == TargetedMSService.FolderType.ExperimentMAM)
            {
                try
                {
                    TargetedMSManager.populatePTMPercentsGroupedPrepivotCache(run, user, container);
                }
                catch (Exception e)
                {
                    LOG.error("Error populating PTMPercentsGroupedPrepivotCache for run {} in {}", run.getId(), container.getPath(), e);
                }
            }
        }

        LOG.info("Finished populating PTMPercentsGroupedPrepivotCache for existing ExperimentMAM folders");
    }

    @SuppressWarnings("UnusedDeclaration")
    public void reparentOrphanedTargetedMSData(final ModuleContext moduleContext)
    {
        if (moduleContext.isNewInstall())
        {
            return;
        }

        Container sharedContainer = ContainerManager.getSharedContainer();

        SqlExecutor executor = new SqlExecutor(TargetedMSManager.getSchema());

        String[] tablesToDeleteOrphans = {"QCAnnotation", "GuideSet", "AutoQCPing"};
        for (String tableName : tablesToDeleteOrphans)
        {
            SQLFragment deleteSql = new SQLFragment("DELETE FROM targetedms.").append(tableName)
                    .append(" WHERE Container NOT IN (SELECT EntityId FROM core.Containers)");
            int deletedCount = executor.execute(deleteSql);
            if (deletedCount > 0)
            {
                LOG.info("Deleted {} orphaned rows from targetedms.{}", deletedCount, tableName);
            }
        }

        String[] tablesToReparentOrphans = {"Runs", "PrecursorChromInfo", "SampleFileChromInfo"};
        for (String tableName : tablesToReparentOrphans)
        {
            SQLFragment updateSql = new SQLFragment("UPDATE targetedms.").append(tableName)
                    .append(" SET Container = ? WHERE Container NOT IN (SELECT EntityId FROM core.Containers)")
                    .add(sharedContainer.getEntityId());
            int updatedCount = executor.execute(updateSql);
            if (updatedCount > 0)
            {
                LOG.info("Reparented {} orphaned rows from targetedms.{} to /Shared", updatedCount, tableName);
            }
        }
    }
}
