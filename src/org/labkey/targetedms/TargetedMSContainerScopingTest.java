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
package org.labkey.targetedms;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.permissions.AbstractContainerScopingTest;
import org.labkey.api.util.GUID;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.parser.skyaudit.UnitTestUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Container-scoping integration tests for {@link TargetedMSController} actions that resolve an object by a
 * global id. Each test sets up the same object in one folder and confirms the action rejects a request that addresses
 * via the wrong container
 */
public class TargetedMSContainerScopingTest extends AbstractContainerScopingTest
{
    private static final Logger LOG = LogHelper.getLogger(TargetedMSContainerScopingTest.class, "TargetedMS container scoping tests");
    private static final GUID DOC_GUID = new GUID("8f1c2a44-3c5e-4f0a-9d2b-6e7a1b3c5d9e");

    @Before
    @After
    public void cleanupAuditLog()
    {
        UnitTestUtil.cleanupDatabase(DOC_GUID);
    }

    /**
     * RemoveLinkVersionAction relinks a Skyline document-version chain by run rowId. It must reject a run that lives in
     * a different container (mirroring its sibling SaveLinkVersionsAction), otherwise an editor in one folder could
     * rewrite the version chain of documents they can't see.
     */
    @Test
    public void removeLinkVersionIsContainerScoped() throws Exception
    {
        Container owner = createContainer("LinkOwner");
        Container other = createContainer("LinkOther");

        // Negative: a run that lives in 'owner' cannot be relinked through the 'other' folder
        ExpRun foreignRun = createRun(owner, "foreign-version");
        ActionURL foreign = new ActionURL(TargetedMSController.RemoveLinkVersionAction.class, other)
                .addParameter("rowId", foreignRun.getRowId());
        assertStatus(HttpServletResponse.SC_BAD_REQUEST, post(foreign, getAdmin()));

        // Positive: a run that lives in the acting folder is accepted
        ExpRun localRun = createRun(other, "local-version");
        ActionURL local = new ActionURL(TargetedMSController.RemoveLinkVersionAction.class, other)
                .addParameter("rowId", localRun.getRowId());
        assertStatus(HttpServletResponse.SC_OK, post(local, getAdmin()));
    }

    /**
     * ShowSkylineAuditLogExtraInfoAJAXAction renders the full Skyline audit detail (user, timestamp, extra info) for an
     * audit entry resolved by global EntryId. It must only render entries whose owning run is in the requested folder,
     * otherwise any reader could read another folder's document audit history by guessing entry ids.
     */
    @Test
    public void auditLogExtraInfoIsContainerScoped() throws Exception
    {
        Container owner = createContainer("AuditOwner");
        Container other = createContainer("AuditOther");

        int entryId = importAuditLogEntry(owner);

        // Negative: the entry's run lives in 'owner', so requesting it via 'other' must 404 rather than leak detail
        ActionURL foreign = new ActionURL(TargetedMSController.ShowSkylineAuditLogExtraInfoAJAXAction.class, other)
                .addParameter("entryId", entryId);
        assertStatus(HttpServletResponse.SC_NOT_FOUND, get(foreign, getAdmin()));

        // Positive: requesting the entry through its owning folder renders normally
        ActionURL owned = new ActionURL(TargetedMSController.ShowSkylineAuditLogExtraInfoAJAXAction.class, owner)
                .addParameter("entryId", entryId);
        assertStatus(HttpServletResponse.SC_OK, get(owned, getAdmin()));
    }

    /** Persist a minimal, saved {@link ExpRun} in the given container so the controller can resolve it by rowId. */
    private ExpRun createRun(Container c, String name) throws Exception
    {
        ExperimentService svc = ExperimentService.get();
        ExpRun run = svc.createExperimentRun(c, name);
        PipeRoot pipeRoot = PipelineService.get().findPipelineRoot(c);
        assertNotNull("No pipeline root for " + c.getPath(), pipeRoot);
        run.setFilePathRoot(pipeRoot.getRootPath());
        run.setProtocol(svc.ensureSampleDerivationProtocol(getAdmin()));
        ViewBackgroundInfo info = new ViewBackgroundInfo(c, getAdmin(), null);
        return svc.saveSimpleExperimentRun(run, Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), info, null, false);
    }

    /** Insert a TargetedMS run in the given container, import a sample Skyline audit log for it, and return an EntryId. */
    private int importAuditLogEntry(Container c) throws Exception
    {
        TargetedMSRun run = new TargetedMSRun();
        run.setContainer(c);
        run.setDocumentGUID(DOC_GUID);
        Table.insert(getAdmin(), TargetedMSManager.getTableInfoRuns(), run);

        File zip = UnitTestUtil.getSampleDataFile("AuditLogFiles/MethodEdit_v1.zip");
        File logFile = UnitTestUtil.extractLogFromZip(zip, LOG);
        new SkylineAuditLogManager(c, null).importAuditLogFile(getAdmin(), logFile, DOC_GUID, run);

        List<Integer> entryIds = new TableSelector(
                TargetedMSManager.getTableInfoSkylineRunAuditLogEntry(),
                Collections.singleton("AuditLogEntryId"),
                new SimpleFilter(FieldKey.fromParts("VersionId"), run.getId()), null)
                .getArrayList(Integer.class);
        assertFalse("No audit log entries were imported", entryIds.isEmpty());
        return entryIds.get(0);
    }
}
