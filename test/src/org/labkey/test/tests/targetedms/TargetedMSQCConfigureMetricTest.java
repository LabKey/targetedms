package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.tests.panoramapremium.TargetedMSPremiumTest;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
        importData(SProCoP_FILE);
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

    @Test
    public void testFixedDeviationFromMeanOption()
    {
        QCPlotsWebPart.MetricType metricType = QCPlotsWebPart.MetricType.TRANSITION_AREA;
        goToProjectHome();
        new PanoramaDashboard(this).getQcSummaryWebPart().clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);

        log("Validating the lower and upper limit inputs");
        configureQCMetrics.setFixedDeviationFromMean(metricType, "", null);
        Assert.assertEquals("Incorrect error for blank lower bound",
                "Error: For Mean Deviation Cut-Off configuration, you must provide a lower bound",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedDeviationFromMean(metricType, "-2", "");
        Assert.assertEquals("Incorrect error for blank upper bound",
                "Error: For Mean Deviation Cut-Off configuration, you must provide an upper bound",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedDeviationFromMean(metricType, "2", "2");
        Assert.assertEquals("Incorrect error for non negative lower bound",
                "Error: For Mean Deviation Cut-Off configuration, the lower bound must be less than 0",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedDeviationFromMean(metricType, "-2", "0");
        Assert.assertEquals("Incorrect error for zero upper bound",
                "Error: For Mean Deviation Cut-Off configuration, the upper bound must be greater than 0",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedDeviationFromMean(metricType, "-2", "2");
        configureQCMetrics.clickSave();

        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.setMetricType(metricType);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);

        String replicate = "Q_Exactive_08_09_2013_JGB_87";

        //TODO: add the verification steps.
    }

    @Test
    public void testFixedValueCutOffOption()
    {
        QCPlotsWebPart.MetricType metric = QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR;
        goToProjectHome();
        new PanoramaDashboard(this).getQcSummaryWebPart().clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);

        configureQCMetrics.setFixedValueCutOff(metric, "5", "-5");
        Assert.assertEquals("Incorrect error for upper bound < lower bound",
                "Error: Upper bound must be greater than lower bound",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedValueCutOff(metric, "a", null);
        Assert.assertEquals("Incorrect error for non integer lower bound",
                "Error: Unable to convert value 'a' to Number (Double)",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedValueCutOff(metric, "-5", "!@#$%^&*()");
        Assert.assertEquals("Incorrect error for non integer upper bound",
                "Error: Unable to convert value '!@#$%^&*()' to Number (Double)",
                configureQCMetrics.clickSaveExpectingError());

        configureQCMetrics.setFixedValueCutOff(metric, "-5", "5");
        configureQCMetrics.clickSave();
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.setMetricType(metric);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);
        qcPlotsWebPart.waitForPlots(6);

        String replicate = "Q_Exactive_08_14_2013_JGB_54";
        verifyOutlierCount(replicate, metric, "1");
    }

    @Test
    public void testPlotOnlyOption()
    {
        QCPlotsWebPart.MetricType metric = QCPlotsWebPart.MetricType.ISOTOPE_DOTP;
        goToProjectHome();
        new PanoramaDashboard(this).getQcSummaryWebPart().clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);
        configureQCMetrics.setShowMetricNoOutlier(metric);
        configureQCMetrics.clickSave();

        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.setMetricType(metric);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);

        String replicate = "Q_Exactive_08_14_2013_JGB_54";
        verifyOutlierCount(replicate, metric, "N/A");
    }

    private void verifyOutlierCount(String replicate, QCPlotsWebPart.MetricType metricType, String count)
    {
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        qcSummaryWebPart.gotoUtilizationCalendar();
        qcSummaryWebPart = new QCSummaryWebPart(getDriver());
        scrollIntoView(Locator.tagContainingText("div", replicate));
        mouseOver(Locator.tagContainingText("div", replicate));
        waitForElement(qcSummaryWebPart.getBubble());
        WebElement bubbleContentEl = qcSummaryWebPart.getBubbleContent().findElement(getDriver());
        waitFor(()-> !bubbleContentEl.getText().isEmpty(), WAIT_FOR_PAGE);
        String bubbleContent = bubbleContentEl.getText();
        log("Bubble content " + bubbleContent);
        Assert.assertTrue("Outlier count for metric " + metricType + "is not correct",
                        Pattern.compile(Pattern.quote(metricType + " " + count), Pattern.CASE_INSENSITIVE).matcher(bubbleContent).find());
        mouseOver(Locator.css(".labkey-page-nav")); //to close the bubble.
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
