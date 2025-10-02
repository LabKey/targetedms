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
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class InstrumentSchedulingTest extends TargetedMSTest implements PostgresOnlyTest
{
    protected static final String SCHEDULER_USER_1 = "scheduler1@targetedms.test";
    protected static final String SCHEDULER_USER_2 = "scheduler2@targetedms.test";

    public static final String INSTRUMENT_1 = "Instrument1";
    public static final String INSTRUMENT_2 = "Instrument2";
    public static final String INACTIVE_INSTRUMENT = "InactiveInstrument";
    public static final String PROJECT_1 = "Project1";
    public static final String PROJECT_2 = "Project2";

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
                Map.of("Name", INSTRUMENT_1, "Active", true, "Color", "#ee0000"),
                Map.of("Name", INSTRUMENT_2, "Active", true, "Color", "#00ee00"),
                Map.of("Name", INACTIVE_INSTRUMENT, "Active", false, "Color", "#0000ee")
        ));
        List<Map<String, Object>> instruments = instrumentInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand projectInsert = new InsertRowsCommand("targetedms", "msProject");
        projectInsert.setRows(Arrays.asList(
                Map.of("Affiliation", "LabKey", "Title", PROJECT_1, "SubmitDate", "1/1/2025", "CollaborationWith", "Mike", "ScientificQuestion", "Why do I have to enter this?", "abstract", "b"),
                Map.of("Affiliation", "UW", "Title", PROJECT_2, "SubmitDate", "2/2/2025", "CollaborationWith", "Josh", "ScientificQuestion", "Why is the sky blue?", "abstract", "a")
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
    public void testSchedule()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Your project list"));
        assertTextPresent(PROJECT_1, PROJECT_2);
        clickAndWait(Locator.linkWithText(PROJECT_1));
        waitAndClickAndWait(Locator.linkWithText("Schedule instrument time"));

        String yearMonth = Calendar.getInstance().get(Calendar.YEAR) + "-";
        int month = (Calendar.getInstance().get(Calendar.MONTH) + 1);
        if (month < 10)
        {
            yearMonth += yearMonth;
        }
        yearMonth += month;

        scheduleInstrument(yearMonth + "-02");
        scheduleInstrument(yearMonth + "-03");
        scheduleInstrument(yearMonth + "-03", true);
        scheduleInstrument(yearMonth + "-03");

        assertProjectEventCounts(2, 0);

        sleep(1000);  // Wait for the dialog to clear out of the way
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.id("projectDropDown"), PROJECT_2));

        scheduleInstrument(yearMonth + "-04");
        assertProjectEventCounts(1, 2);

        scheduleInstrument(yearMonth + "-05");
        assertProjectEventCounts(2, 2);

        sleep(1000);  // Wait for the dialog to clear out of the way
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.id("instrumentDropDown"), INSTRUMENT_2));
        scheduleInstrument(yearMonth + "-06");
        assertProjectEventCounts(1, 0);

        goToDashboard();
        clickAndWait(Locator.linkWithText("All instrument calendar view"));
        assertTextPresent(INSTRUMENT_1, INSTRUMENT_2, INACTIVE_INSTRUMENT);

        waitForElementToBeVisible(Locator.tagWithClass("div", "activeProjectEvent"));
        assertProjectEventCounts(5, 0);

        selectOptionByText(Locator.id("projectFilter"), PROJECT_2);
        assertProjectEventCounts(3, 2);

        goToDashboard();
        clickAndWait(Locator.linkWithText("Instrument billing report"));
        assertTextPresent("$2,450.00", 4);
        assertTextPresent("$2,690.00", 1);

        // Future test cases:
        // Split payment across multiple methods
        // Schedule for hours within a day instead of 24-hour periods
        // Check billing for individual months, including reservations that span month boundaries with start/end dates
        // Ensure that overlapping reservations are rejected
        // Ensure that reservations cannot be made for inactive instruments
    }

    private void assertProjectEventCounts(int expectedActiveCount, int expectedOtherCount)
    {
        assertElementPresent(Locator.tagWithClass("div", "activeProjectEvent"), expectedActiveCount);
        assertElementPresent(Locator.tagWithClass("div", "otherProjectEvent"), expectedOtherCount);
    }

    private void scheduleInstrument(String yearMonthDay)
    {
        scheduleInstrument(yearMonthDay, false);
    }

    private void scheduleInstrument(String yearMonthDay, boolean delete)
    {
        waitAndClick(Locator.tagWithAttribute("td", "data-date", yearMonthDay));
        waitForText("Add Instrument Time");
        if (delete)
        {
            waitAndClick(Locator.button("Delete"));
        }
        else
        {
            waitForElementToBeVisible(Locator.id("event-name"));
            setFormElement(Locator.id("event-name").findElement(getDriver()), "A name!");
            setFormElement(Locator.id("event-notes").findElement(getDriver()), "A note!");
            waitAndClick(Locator.button("Save"));
            waitAndClick(Locator.button("Yes"));
        }
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
