package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.tests.panoramapremium.TargetedMSPremiumTest;

import java.util.Arrays;
import java.util.List;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class TargetedMSQCConfigureMetricTest extends TargetedMSPremiumTest
{
    private final static String SUBFOLDER_1 = "QC Subfolder 1";
    private final static String SUBFOLDER_2 = "QC_Subfolder_2";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSQCConfigureMetricTest init = (TargetedMSQCConfigureMetricTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    private void doSetup()
    {
        setupFolder(FolderType.QC);
    }

   /*
        Regression coverage for Issue 49427: Inconsistent number of metrics in Panorama QC folder
    */
    @Test
    public void testInconsistentMetricDisplay()
    {
        log("Create subfolder, import data and configure custom metrics in " + SUBFOLDER_1);
        setupSubfolder(getProjectName(), SUBFOLDER_1, FolderType.QC);
        importData(SProCoP_FILE);
        navigateToFolder(getProjectName(), SUBFOLDER_1);
        new PanoramaDashboard(this).getQcSummaryWebPart().clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);
        configureQCMetrics.disableMetric("Precursor Area")
                .disableMetric("Retention Time")
                .disableMetric("Transition Area")
                .clickSave();

        log("Create subfolder, import data and configure custom metrics in " + SUBFOLDER_2);
        goToProjectHome();
        setupSubfolder(getProjectName(), SUBFOLDER_2, FolderType.QC);
        importData(SProCoP_FILE);
        navigateToFolder(getProjectName(), SUBFOLDER_2);
        new PanoramaDashboard(this).getQcSummaryWebPart().clickMenuItem("Configure QC Metrics");
        configureQCMetrics = new ConfigureMetricsUIPage(this);
        configureQCMetrics.disableMetric("Precursor Area")
                .clickSave();

        log("Verify the data is displayed correctly on parent folder");
        goToProjectHome();
        QCSummaryWebPart qcSummary = new PanoramaDashboard(this).getQcSummaryWebPart();
        List<QCSummaryWebPart.QcSummaryTile> subFolderTile = qcSummary.getQcSummaryTiles();
        Assert.assertEquals("Incorrect metric displayed for " + subFolderTile.get(1).getFolderName(), 9, subFolderTile.get(1).getMetricsCount());
        Assert.assertEquals("Incorrect metric displayed for " + subFolderTile.get(2).getFolderName(), 11, subFolderTile.get(2).getMetricsCount());
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMSQCConfigureMetricTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
