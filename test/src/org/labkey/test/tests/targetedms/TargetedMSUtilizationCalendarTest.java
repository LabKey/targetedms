package org.labkey.test.tests.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.UtilizationCalendarWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;

import java.util.Calendar;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSUtilizationCalendarTest extends TargetedMSTest
{
    private final static Calendar calendar = Calendar.getInstance();
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
    public void testSteps()
    {
        goToProjectHome();
        UtilizationCalendarWebPart calendarWebPart = new PanoramaDashboard(this)
                .getQcSummaryWebPart()
                .gotoUtilizationCalendar();

        log("Verifying Display options");
        Assert.assertEquals("Incorrect default value for display months", "1 month", calendarWebPart.getDisplay());
        Assert.assertEquals("Incorrect month displayed", "August 2013", calendarWebPart.getDisplayedMonth());

        calendarWebPart.setDisplay("4");
        Assert.assertEquals("Incorrect month displayed", "May 2013 - August 2013", calendarWebPart.getDisplayedMonth());

        calendarWebPart.setDisplay("12");
        Assert.assertEquals("Incorrect month displayed", "2012 - 2013", calendarWebPart.getDisplayedMonth());

        calendarWebPart.setDisplay("1");
        calendarWebPart.markOffline("28" , "30", "Automation testing");
    }

}
