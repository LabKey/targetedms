package org.labkey.test.tests.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.UtilizationCalendarWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSUtilizationCalendarTest extends TargetedMSTest
{
    @Override
    protected @Nullable String getProjectName()
    {
        return getCurrentTestClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSUtilizationCalendarTest init = (TargetedMSUtilizationCalendarTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(TargetedMSTest.FolderType.QC);
        importData(SProCoP_FILE);
    }

    @Test
    public void testUtilizationCalendarActions()
    {
        goToProjectHome();
        UtilizationCalendarWebPart utilizationCalendar = new PanoramaDashboard(this)
                .getQcSummaryWebPart()
                .gotoUtilizationCalendar();

        log("Verifying Display options and Validations");
        Assert.assertEquals("Incorrect default value for display months", "1 month", utilizationCalendar.getDisplay());
        Assert.assertEquals("Incorrect month displayed", "August 2013", utilizationCalendar.getDisplayedMonth());

        utilizationCalendar.setDisplay("4");
        Assert.assertEquals("Incorrect month displayed", "May 2013 - August 2013", utilizationCalendar.getDisplayedMonth());

        utilizationCalendar.setDisplay("12");
        Assert.assertEquals("Incorrect month displayed", "2012 - 2013", utilizationCalendar.getDisplayedMonth());

        utilizationCalendar.setDisplay("1");
        Assert.assertEquals("Validation failed for empty description", "Error saving. Missing value for required property: Description",
                utilizationCalendar.getValidationErrorMsg("2013-08-2", null, ""));
        Assert.assertEquals("Validation failed for invalid date", "Error saving. Could not convert value '2013-0802' (String) for Timestamp field 'EndDate'",
                utilizationCalendar.getValidationErrorMsg("2013-08-2", "2013-0802", null));
        Assert.assertEquals("Validate failed for end date < start date", "Error saving. End date cannot be before the start date",
                utilizationCalendar.getValidationErrorMsg("2013-08-2", "2013-08-01", null));

        log("Marking single day offline");
        String offlineDate = "2013-08-13";
        utilizationCalendar.markOffline(offlineDate, "Offline for single day");
        Assert.assertTrue("Date " + offlineDate + " did not update to offline", utilizationCalendar.isOffline(offlineDate));

        log("Marking multiple days offline");
        String startDate = "2013-08-28";
        String endDate = "2013-08-30";
        utilizationCalendar.markOffline(startDate, endDate, "Offline for multiple days");
        Assert.assertTrue("Date " + startDate + " did not update to offline", utilizationCalendar.isOffline(startDate));
        Assert.assertTrue("Date 2013-08-29 did not update to offline", utilizationCalendar.isOffline("2013-08-29"));
        Assert.assertTrue("Date " + endDate + " did not update to offline", utilizationCalendar.isOffline(endDate));

        log("Verify audit log entries");
        DataRegionTable auditTable = goToAdminConsole().clickAuditLog().selectView("Query update events").getLogTable();
        auditTable.setFilter("ProjectId", "Equals", getProjectName());
        Assert.assertEquals("Incorrect audit log entries", Arrays.asList("A row was inserted.", "A row was inserted."),
                auditTable.getColumnDataAsText("Comment"));
    }

    @Test
    public void testReaderRoleAccessibility()
    {
        goToProjectHome();
        impersonateRole("Reader");
        UtilizationCalendarWebPart utilizationCalendar = new PanoramaDashboard(this)
                .getQcSummaryWebPart()
                .gotoUtilizationCalendar();
        try
        {
            utilizationCalendar.clickDate("2013-08-01");
            Assert.fail("Readers should not be able to update the status");
        }
        catch (Exception e)
        {
            Assert.assertTrue("Reader cannot update the status", true);
        }
        stopImpersonating();
    }
}
