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

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // Don't insert additional projects until after testSchedule() has run
public class InstrumentSchedulingTest extends TargetedMSTest implements PostgresOnlyTest
{
    public static final String INSTRUMENT_1 = "Instrument1";
    public static final String INSTRUMENT_2 = "Instrument2";
    public static final String INACTIVE_INSTRUMENT = "InactiveInstrument";
    public static final String PROJECT_1 = "Project1";
    public static final String PROJECT_2 = "Project2";
    public static final String PAYMENT_METHOD_1 = "PaymentMethod1";
    public static final String PAYMENT_METHOD_2 = "PaymentMethod2";
    public static final String PAYMENT_METHOD_3 = "PaymentMethod3";
    protected static final String LAB_MEMBER_USER = "labmember@targetedms.test";
    protected static final String EXTERNAL_COLLABORATOR_USER = "collaborator@targetedms.test";

    public static final Locator.IdLocator EVENT_NAME_FIELD = Locator.id("event-name");
    public static final Locator.IdLocator EVENT_NOTE_FIELD = Locator.id("event-notes");

    public static final Locator.IdLocator START_DATE_TIME_FIELD = Locator.id("event-start-date");
    public static final Locator.IdLocator END_DATE_TIME_FIELD = Locator.id("event-end-date");
    public static final Locator.IdLocator INSTRUMENT_DROP_DOWN = Locator.id("instrumentDropDown");
    public static final Locator.IdLocator PROJECT_DROP_DOWN = Locator.id("projectDropDown");
    public static final Locator PAYMENT_METHOD_DROP_DOWNS = Locator.tagWithClass("select", "paymentMethodDropDown");
    public static final Locator PAYMENT_METHOD_PERCENTS = Locator.tagWithClass("input", "paymentMethodPercent");


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

        int labMemberUserId = _userHelper.createUser(LAB_MEMBER_USER).getUserId();
        int collaboratorUserId = _userHelper.createUser(EXTERNAL_COLLABORATOR_USER).getUserId();
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(LAB_MEMBER_USER, PermissionsHelper.EDITOR_ROLE, PermissionsHelper.MemberType.user);
        apiPermissionsHelper.addMemberToRole(EXTERNAL_COLLABORATOR_USER, PermissionsHelper.SUBMITTER_ROLE, PermissionsHelper.MemberType.user);
        apiPermissionsHelper.addMemberToRole(EXTERNAL_COLLABORATOR_USER, PermissionsHelper.READER_ROLE, PermissionsHelper.MemberType.user);

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
                Map.of("Name", "BigSpenderRate", "SetupFee", 66)
        ));
        List<Map<String, Object>> rateTypes = rateTypeInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand paymentMethodInsert = new InsertRowsCommand("targetedms", "paymentMethod");
        paymentMethodInsert.setRows(Arrays.asList(
                Map.of("UWBudgetNumber", "1111", "Name", PAYMENT_METHOD_1, "RateType", rateTypes.get(0).get("Id")),
                Map.of("UWBudgetNumber", "2222", "Name", PAYMENT_METHOD_2, "RateType", rateTypes.get(1).get("Id")),
                Map.of("UWBudgetNumber", "3333", "Name", PAYMENT_METHOD_3, "RateType", rateTypes.get(0).get("Id")) // Intentionally not associated with a project
        ));
        List<Map<String, Object>> paymentMethods = paymentMethodInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand projectPaymentMethodInsert = new InsertRowsCommand("targetedms", "projectPaymentMethod");
        projectPaymentMethodInsert.setRows(Arrays.asList(
                Map.of("PaymentMethod", paymentMethods.get(0).get("Id"), "Project", projects.get(0).get("Id")),
                Map.of("PaymentMethod", paymentMethods.get(0).get("Id"), "Project", projects.get(1).get("Id")),
                Map.of("PaymentMethod", paymentMethods.get(1).get("Id"), "Project", projects.get(1).get("Id"))
        ));
        List<Map<String, Object>> projectPaymentMethods = projectPaymentMethodInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand projectResearcherInsert = new InsertRowsCommand("targetedms", "projectResearcher");
        projectResearcherInsert.setRows(Arrays.asList(
                Map.of("Project", projects.get(0).get("Id"), "Researcher", labMemberUserId),
                Map.of("Project", projects.get(1).get("Id"), "Researcher", labMemberUserId),
                Map.of("Project", projects.get(1).get("Id"), "Researcher", collaboratorUserId)
        ));
        List<Map<String, Object>> projectResearchers = projectResearcherInsert.execute(createDefaultConnection(), getProjectName()).getRows();

        InsertRowsCommand instrumentRateInsert = new InsertRowsCommand("targetedms", "instrumentRate");
        instrumentRateInsert.setRows(Arrays.asList(
                Map.of("Instrument", instruments.get(0).get("Id"), "rateType", rateTypes.get(0).get("Id"), "fee", 100),
                Map.of("Instrument", instruments.get(0).get("Id"), "rateType", rateTypes.get(1).get("Id"), "fee", 211),
                Map.of("Instrument", instruments.get(1).get("Id"), "rateType", rateTypes.get(0).get("Id"), "fee", 100),
                Map.of("Instrument", instruments.get(1).get("Id"), "rateType", rateTypes.get(1).get("Id"), "fee", 330)
        ));
        List<Map<String, Object>> instrumentRates = instrumentRateInsert.execute(createDefaultConnection(), getProjectName()).getRows();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // these tests use the UIContainerHelper for project creation, but we can use the APIContainerHelper for deletion
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(getProjectName(), afterTest);

        _userHelper.deleteUsers(false, LAB_MEMBER_USER);
        _userHelper.deleteUsers(false, EXTERNAL_COLLABORATOR_USER);
    }

    @Test
    public void testSchedule() throws IOException, CommandException
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Your project list"));
        waitForText(PROJECT_1, PROJECT_2);
        clickAndWait(Locator.linkWithText(PROJECT_1));
        waitAndClickAndWait(Locator.linkWithText("Schedule instrument time"));

        assertTrue("Wrong instrument list", waitFor(() -> Arrays.asList(INSTRUMENT_1, INSTRUMENT_2).equals(getSelectOptions(INSTRUMENT_DROP_DOWN)), 5_000));
        assertTrue("Wrong payment method list", waitFor(() -> Arrays.asList(PAYMENT_METHOD_1).equals(getSelectOptions(PAYMENT_METHOD_DROP_DOWNS)), 5_000));

        int month = (Calendar.getInstance().get(Calendar.MONTH) + 1);
        String yearMonth = Calendar.getInstance().get(Calendar.YEAR) + "-" + (month < 10 ? "0" + month : "" + month);

        scheduleInstrument(yearMonth + "-02", false);
        scheduleInstrument(yearMonth + "-03", false, () ->
        {
            String originalStart = getFormElement(START_DATE_TIME_FIELD.findElement(getDriver()));
            String originalEnd = getFormElement(END_DATE_TIME_FIELD.findElement(getDriver()));
            // Try scheduling over the first reservation and verify it is blocked
            setFormElement(START_DATE_TIME_FIELD.findElement(getDriver()), originalStart.replace("-03T", "-02T"));
            setFormElement(END_DATE_TIME_FIELD.findElement(getDriver()), originalEnd.replace("-03T", "-02T"));
            waitAndClick(Locator.button("Save"));
            waitForText("Error saving. Instrument schedule overlaps with an existing reservation for this instrument");
            setFormElement(START_DATE_TIME_FIELD.findElement(getDriver()), originalStart);
            waitForText("End date must be after start date.");
            setFormElement(END_DATE_TIME_FIELD.findElement(getDriver()), originalEnd);
        });
        scheduleInstrument(yearMonth + "-03", true);
        scheduleInstrument(yearMonth + "-03", false);

        assertProjectEventCounts(2, 0);

        doAndWaitForPageToLoad(() -> selectOptionByText(PROJECT_DROP_DOWN, PROJECT_2));

        scheduleInstrument(yearMonth + "-04", false);
        assertProjectEventCounts(1, 2);

        scheduleInstrument(yearMonth + "-05", false);
        assertProjectEventCounts(2, 2);

        doAndWaitForPageToLoad(() -> selectOptionByText(INSTRUMENT_DROP_DOWN, INSTRUMENT_2));
        sleep(1000);
        click(Locator.id("addPaymentMethod"));
        List<WebElement> percentInputs = getDriver().findElements(PAYMENT_METHOD_PERCENTS);
        assertEquals("Wrong number of payment method percents", 2, percentInputs.size());
        List<WebElement> methodInputs = getDriver().findElements(PAYMENT_METHOD_DROP_DOWNS);
        assertEquals("Wrong number of payment method dropdowns", 2, methodInputs.size());

        // Duplicate payment methods
        selectOptionByText(methodInputs.get(0), PAYMENT_METHOD_2);
        assertTextPresent("The same payment method cannot be selected more than once.");
        selectOptionByText(methodInputs.get(0), PAYMENT_METHOD_1);

        // Bogus payment percentages
        setFormElement(percentInputs.get(1), "0");
        assertTextPresent("Each payment percentage must be a number between 0 and 100.");
        setFormElement(percentInputs.get(1), "60");
        assertTextPresent("When multiple payment methods are used, the percentages must add up to 100% (current total: 110%).");
        setFormElement(percentInputs.get(0), "40");

        AtomicBoolean twoDayReservationOverWeekend = new AtomicBoolean(false);
        scheduleInstrument(yearMonth + "-06", false, () -> {
            // Make it a two-day reservation
            String originalEnd = getFormElement(END_DATE_TIME_FIELD.findElement(getDriver()));
            try {
                Calendar c = Calendar.getInstance();
                c.setTime(new SimpleDateFormat(getCurrentDateFormatString()).parse(originalEnd));
                twoDayReservationOverWeekend.set(c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY);
            } catch (ParseException ignore) {}
            setFormElement(END_DATE_TIME_FIELD.findElement(getDriver()), originalEnd.replace("-06T", "-07T"));
        });
        // If an event spans a weekend, it will be represented by two separate event elements
        assertProjectEventCounts(twoDayReservationOverWeekend.get() ? 2 : 1, 0);

        impersonate(LAB_MEMBER_USER);
        assertEquals("Wrong number of projects for " + LAB_MEMBER_USER,
                2,
                new SelectRowsCommand("targetedms", "msProject").execute(createDefaultConnection(), getProjectName()).getRows().size());

        stopImpersonating();
        impersonate(EXTERNAL_COLLABORATOR_USER);
        List<Map<String, Object>> projects = new SelectRowsCommand("targetedms", "msProject").execute(createDefaultConnection(), getProjectName()).getRows();
        assertEquals("Wrong number of projects for " + EXTERNAL_COLLABORATOR_USER, 1, projects.size());
        int project2Id = (Integer) projects.get(0).get("Id");
        SelectRowsCommand instrumentSelect = new SelectRowsCommand("targetedms", "msInstrument");
        instrumentSelect.setFilters(Arrays.asList(new Filter("Name", INSTRUMENT_1)));
        List<Map<String, Object>> instruments = instrumentSelect.execute(createDefaultConnection(), getProjectName()).getRows();
        assertEquals("Wrong number of instruments", 1, instruments.size());
        int instrument1Id = (Integer) instruments.get(0).get("Id");

        SelectRowsCommand paymentMethodSelect = new SelectRowsCommand("targetedms", "paymentMethod");
        paymentMethodSelect.setFilters(Arrays.asList(new Filter("Name", PAYMENT_METHOD_1)));
        List<Map<String, Object>> paymentMethods = paymentMethodSelect.execute(createDefaultConnection(), getProjectName()).getRows();
        assertEquals("Wrong number of paymentMethods", 1, paymentMethods.size());
        int paymentMethod1Id = (Integer) paymentMethods.get(0).get("Id");

        int project1Id = project2Id - 1;  // Assume sequential auto-incrementing ids
        int inactiveInstrumentId = instrument1Id + 2;
        int paymentMethod3Id = paymentMethod1Id + 2;

        attemptScheduleInsertExpectingFailure(
                Map.of("Project", project1Id, "Instrument", instrument1Id),
                "User is not a member of the project");
        attemptScheduleInsertExpectingFailure(
                Map.of("Project", project2Id, "Instrument", instrument1Id),
                "StartTime and EndTime are required");
        Calendar start = Calendar.getInstance();
        start.add(Calendar.MONTH, 1);
        Calendar end = Calendar.getInstance();
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.DATE, 1);
        Date startDate = new Date(start.getTimeInMillis());
        Date endDate = new Date(end.getTimeInMillis());
        attemptScheduleInsertExpectingFailure(
                Map.of("Project", project2Id, "Instrument", instrument1Id, "StartTime", endDate, "EndTime", startDate),
                "StartTime must be before EndTime");
        attemptScheduleInsertExpectingFailure(
                Map.of("Project", project2Id, "Instrument", instrument1Id, "StartTime", startDate, "EndTime", endDate),
                "Instrument usage payments do not add up to 100%");
        attemptScheduleInsertExpectingFailure(
                Map.of("Project", project2Id, "Instrument", instrument1Id, "StartTime", startDate, "EndTime", endDate, "UsagePayments", Arrays.asList(Map.of("PaymentMethod", paymentMethod3Id, "PercentPayment", 100))),
                "Instrument usage payments are not using a payment method that is configured for the project.");
        attemptScheduleInsertExpectingFailure(
                Map.of("Project", project2Id, "Instrument", inactiveInstrumentId, "StartTime", startDate, "EndTime", endDate, "UsagePayments", Arrays.asList(Map.of("PaymentMethod", paymentMethod1Id, "PercentPayment", 100))),
                "Instrument does not exist or is not active");

        stopImpersonating();

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText("All instrument calendar view"));
        waitForText(INSTRUMENT_1, INSTRUMENT_2, INACTIVE_INSTRUMENT);

        assertProjectEventCounts(twoDayReservationOverWeekend.get() ? 6 : 5, 0);

        selectOptionByText(Locator.id("projectFilter"), PROJECT_2);
        assertProjectEventCounts(twoDayReservationOverWeekend.get() ? 4 : 3, 2);

        goToDashboard();
        waitAndClickAndWait(Locator.linkWithText("Instrument billing report"));
        assertTextPresent("$950.00", 8);
        // Two rows, one for each of the two payment methods
        assertTextPresent("$3,350.00", 1);
        assertTextPresent("$10,956.00", 1);
        assertTextPresent(PAYMENT_METHOD_1, 5);
        assertTextPresent(PAYMENT_METHOD_2, 1);
        // Verify the 40/60 split (though odd since they're different rate types)
        assertTextPresent("$1,340.00", "$6,573.60");

        goToDashboard();
        clickAndWait(Locator.linkWithText("Monthly instrument billing report"));
        // Choose a date that splits a reservation into two parts
        setFormElement(Locator.name("query.param.StartBillDate"), yearMonth + "-07");
        clickButton("Submit");
        // Only some hours should be in the range for this billing report
        assertTextPresent("17.0", 2);
        assertTextPresent("$680.00", "$3,366.00");
    }

    private void attemptScheduleInsertExpectingFailure(Map<String, Object> row, String expected) throws IOException
    {
        InsertRowsCommand scheduleInsert = new InsertRowsCommand("targetedms", "instrumentSchedule");
        scheduleInsert.setRows(Arrays.asList(row));
        failInsert(scheduleInsert, expected);
    }

    @Test
    public void testSetupInsertPermissions() throws IOException, CommandException
    {
        // Validate that a collaborator can't add a project themselves
        int adminId = getCurrentUserId();
        impersonate(EXTERNAL_COLLABORATOR_USER);
        InsertRowsCommand projectInsert = new InsertRowsCommand("targetedms", "msProject");
        projectInsert.setRows(Arrays.asList(
                Map.of("Affiliation", "External", "Title", "External", "SubmitDate", "1/1/2025", "CollaborationWith", "Mike", "ScientificQuestion", "Why are collaborators so great?", "abstract", "c")
        ));

        failInsert(projectInsert, null);

        // Insert as a lab member
        stopImpersonating();
        impersonate(LAB_MEMBER_USER);
        Map<String, Object> project = projectInsert.execute(createDefaultConnection(), getProjectName()).getRows().get(0);
        int projectId = (Integer) project.get("Id");

        // Collaborator isn't part of the project, so they shouldn't be able to add a researcher
        stopImpersonating();
        impersonate(EXTERNAL_COLLABORATOR_USER);
        int collaboratorId = getCurrentUserId();
        InsertRowsCommand researcherInsert = new InsertRowsCommand("targetedms", "projectResearcher");
        researcherInsert.setRows(Arrays.asList(
                Map.of("Project", projectId, "Researcher", collaboratorId)
        ));
        failInsert(researcherInsert, null);

        // Add the collaborator
        stopImpersonating();
        impersonate(LAB_MEMBER_USER);
        researcherInsert.execute(createDefaultConnection(), getProjectName());

        // Now the collaborator should be able to add another researcher
        stopImpersonating();
        impersonate(EXTERNAL_COLLABORATOR_USER);
        researcherInsert.setRows(Arrays.asList(
                Map.of("Project", projectId, "Researcher", adminId)
        ));
        researcherInsert.execute(createDefaultConnection(), getProjectName());


    }

    private void failInsert(InsertRowsCommand insert, String expectedMessage) throws IOException
    {
        try
        {
            insert.execute(createDefaultConnection(), getProjectName());
            fail("Shouldn't have permissions");
        }
        catch (CommandException e)
        {
            if (expectedMessage != null)
            {
                assertEquals(expectedMessage, e.getMessage());
            }
        }
    }



    private void assertProjectEventCounts(int expectedActiveCount, int expectedOtherCount)
    {
        Locator activeLocator = Locator.byClass("activeProjectEvent");
        Locator otherLocator = Locator.byClass("otherProjectEvent");
        waitFor(() -> expectedActiveCount == getElementCount(activeLocator) && expectedOtherCount == getElementCount(otherLocator), 5_000);
        assertElementPresent(activeLocator, expectedActiveCount);
        assertElementPresent(otherLocator, expectedOtherCount);
    }

    private void scheduleInstrument(String yearMonthDay, boolean delete)
    {
        scheduleInstrument(yearMonthDay, delete, () -> {});
    }

    private void scheduleInstrument(String yearMonthDay, boolean delete, @NotNull Runnable extraSteps)
    {
        waitAndClick(Locator.tagWithAttribute("td", "data-date", yearMonthDay));
        waitForText("Add Instrument Time");
        extraSteps.run();
        if (delete)
        {
            waitAndClick(Locator.button("Delete"));
        }
        else
        {
            waitForElementToBeVisible(EVENT_NAME_FIELD);
            setFormElement(EVENT_NAME_FIELD.findElement(getDriver()), "A name!");
            setFormElement(EVENT_NOTE_FIELD.findElement(getDriver()), "A note!");
            waitAndClick(Locator.button("Save"));
        }
    }

}
