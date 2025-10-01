/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class InstrumentSchedulingTest extends TargetedMSTest
{
    protected static final String SCHEDULER_USER_1 = "scheduler1@targetedms.test";
    protected static final String SCHEDULER_USER_2 = "scheduler2@targetedms.test";

    @BeforeClass
    public static void initProject() throws IOException, CommandException
    {
        InstrumentSchedulingTest init = getCurrentTest();
        init.doInit();
    }

    private void doInit() throws IOException, CommandException
    {
        setupFolder(FolderType.Experiment);
        new PortalHelper(this).addWebPart("Instrument Scheduling Admin");

        int schedulerUser1Id = _userHelper.createUser(SCHEDULER_USER_1).getUserId();
        int schedulerUser2Id = _userHelper.createUser(SCHEDULER_USER_2).getUserId();
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(SCHEDULER_USER_1, "Editor", PermissionsHelper.MemberType.user);
        apiPermissionsHelper.addMemberToRole(SCHEDULER_USER_2, "Editor", PermissionsHelper.MemberType.user);

        InsertRowsCommand instrumentInsert = new InsertRowsCommand("targetedms", "msInstrument");
        instrumentInsert.setRows(Arrays.asList(
                Map.of("Name", "Instrument1", "Active", true, "Color", "#ee0000"),
                Map.of("Name", "Instrument2", "Active", true, "Color", "#00ee00"),
                Map.of("Name", "InactiveInstrument", "Active", false, "Color", "#0000ee")
        ));
        List<Map<String, Object>> instruments = instrumentInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand projectInsert = new InsertRowsCommand("targetedms", "msProject");
        projectInsert.setRows(Arrays.asList(
                Map.of("Affiliation", "LabKey", "Title", "Project1", "SubmitDate", "1/1/2025", "CollaborationWith", "Mike", "ScientificQuestion", "Why do I have to enter this?", "abstract", "b"),
                Map.of("Affiliation", "UW", "Title", "Project2", "SubmitDate", "2/2/2025", "CollaborationWith", "Josh", "ScientificQuestion", "Why is the sky blue?", "abstract", "a")
        ));
        List<Map<String, Object>> projects = projectInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand rateTypeInsert = new InsertRowsCommand("targetedms", "rateType");
        rateTypeInsert.setRows(Arrays.asList(
                Map.of("Name", "DefaultRate", "SetupFee", 50),
                Map.of("Name", "BigSpenderRate", "SetupFee", 50)
        ));
        List<Map<String, Object>> rateTypes = rateTypeInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand paymentMethodInsert = new InsertRowsCommand("targetedms", "paymentMethod");
        paymentMethodInsert.setRows(Arrays.asList(
                Map.of("UWBudgetNumber", "1111", "Name", "PaymentMethod1"),
                Map.of("UWBudgetNumber", "2222", "Name", "PaymentMethod2")
        ));
        List<Map<String, Object>> paymentMethods = paymentMethodInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand projectPaymentMethodInsert = new InsertRowsCommand("targetedms", "projectPaymentMethod");
        projectPaymentMethodInsert.setRows(Arrays.asList(
                Map.of("PaymentMethod", paymentMethods.get(0).get("Id"), "Project", projects.get(0).get("Id")),
                Map.of("PaymentMethod", paymentMethods.get(1).get("Id"), "Project", projects.get(1).get("Id"))
        ));
        List<Map<String, Object>> projectPaymentMethods = projectPaymentMethodInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand projectResearcherInsert = new InsertRowsCommand("targetedms", "projectResearcher");
        projectResearcherInsert.setRows(Arrays.asList(
                Map.of("Project", projects.get(0).get("Id"), "Researcher", schedulerUser1Id),
                Map.of("Project", projects.get(1).get("Id"), "Researcher", schedulerUser1Id),
                Map.of("Project", projects.get(1).get("Id"), "Researcher", schedulerUser2Id)
        ));
        List<Map<String, Object>> projectResearchers = projectResearcherInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand instrumentRateInsert = new InsertRowsCommand("targetedms", "instrumentRate");
        instrumentRateInsert.setRows(Arrays.asList(
                Map.of("Instrument", instruments.get(0).get("Id"), "rateType", rateTypes.get(0).get("Id"), "fee", 100),
                Map.of("Instrument", instruments.get(1).get("Id"), "rateType", rateTypes.get(1).get("Id"), "fee", 110)
        ));
        List<Map<String, Object>> instrumentRates = instrumentRateInsert.execute(createDefaultConnection(), getProjectName()).getRows();
    }

    @Test
    public void doSomeStuff()
    {

    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // these tests use the UIContainerHelper for project creation, but we can use the APIContainerHelper for deletion
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(getProjectName(), afterTest);
        
        _userHelper.deleteUsers(false, SCHEDULER_USER_1);
        _userHelper.deleteUsers(false, SCHEDULER_USER_2);
    }
}
