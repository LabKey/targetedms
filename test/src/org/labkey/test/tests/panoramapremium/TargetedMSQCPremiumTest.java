package org.labkey.test.tests.panoramapremium;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.labkey.test.Locator;
import org.labkey.test.categories.Git;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.tests.targetedms.TargetedMSTest;
import org.labkey.test.util.ApiPermissionsHelper;
import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(Git.class)
public class TargetedMSQCPremiumTest extends TargetedMSTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + "Premium Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCPremiumTest init = (TargetedMSQCPremiumTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _containerHelper.enableModules(Arrays.asList("Dumbster", "PanoramaPremium"));
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).setUserPermissions(USER, "Reader");
        importData(SProCoP_FILE);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testConfigureQCMetrics()
    {
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        uncheckCheckbox(Locator.checkboxByName("Peak Area"));
        click(Locator.buttonContainingText("Save"));

        List<String> qcMetricOptions = qcPlotsWebPart.getMetricTypeOptions();

        log("Verifying disabled metric not present in QC Plot dashboard dropdown");
        qcMetricOptions.forEach(qcMetric-> assertFalse("Disabled QC Metric found - Peak Area", qcMetric.equalsIgnoreCase("Peak Area")));

        impersonate(USER);
        BootstrapMenu qcPlotMenu = qcPlotsWebPart.getWebParMenu();
        log("Verifying Configure QC Metrics Menu option not present for non admin");
        try
        {
            qcPlotMenu.findVisibleMenuItems();
            assert false;
        }
        catch (NoSuchElementException ex)
        {
            assert true : "No menu option present";
        }

    }

    @Test
    public void testNotification()
    {
        goToProjectHome();
        PanoramaDashboard qcDashboard = goToDashboard();
        QCSummaryWebPart qcSummary = qcDashboard.getQcSummaryWebPart();

        log("Clicking the outliew notification link");
        qcSummary.clickMenuItem("Subscribe Outlier Notifications");
        checkRadioButton(Locator.radioButtonByName("subscriptionType").index(1));
        setFormElement(Locator.name("outlierCount"), "1");
        clickAndWait(Locator.tagWithAttribute("input", "value", "Save"));

        log("Importing the file to trigger the notification");
        importData(SProCoP_FILE_ANNOTATED, 2);

        log("Verifying the notification");
        goToModule("Dumbster");
        EmailRecordTable notifications = new EmailRecordTable(this);
        assertEquals("Mismatch in the expected number of notification", 1, notifications.getEmailCount());
        EmailRecordTable.EmailMessage message = notifications.getMessageWithSubjectContaining("Panorama QC Notification");
        notifications.clickMessage(message);
        assertTextPresent("56"); //Total outliers

    }
}
