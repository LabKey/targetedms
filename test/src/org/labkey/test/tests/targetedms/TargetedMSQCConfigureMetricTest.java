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
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tests the QC metric configuration UI and verifies settings propagate consistently across folder hierarchies.
 */
@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 15)
public class TargetedMSQCConfigureMetricTest extends TargetedMSPremiumTest
{
    private final static String SUBFOLDER_1 = "QC Subfolder 1";
    private final static String SUBFOLDER_2 = "QC_Subfolder_2";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSQCConfigureMetricTest init = getCurrentTest();
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
        ConfigureMetricsUIPage configureQCMetrics = new PanoramaDashboard(this).getQcSummaryWebPart().clickConfigureQCMetrics();
        configureQCMetrics.disableMetric(QCPlotsWebPart.MetricType.PRECURSOR_AREA.toString())
                .disableMetric(QCPlotsWebPart.MetricType.RETENTION.toString())
                .disableMetric(QCPlotsWebPart.MetricType.TRANSITION_AREA.toString())
                .clickSave();

        log("Create subfolder, import data and configure custom metrics in " + SUBFOLDER_2);
        goToProjectHome();
        setupSubfolder(getProjectName(), SUBFOLDER_2, FolderType.QC);
        importData(SProCoP_FILE);
        navigateToFolder(getProjectName(), SUBFOLDER_2);
        configureQCMetrics = new PanoramaDashboard(this).getQcSummaryWebPart().clickConfigureQCMetrics();
        configureQCMetrics.disableMetric(QCPlotsWebPart.MetricType.PRECURSOR_AREA.toString())
                .clickSave();

        log("Verify the data is displayed correctly on parent folder");
        goToProjectHome();
        QCSummaryWebPart qcSummary = new PanoramaDashboard(this).getQcSummaryWebPart();
        List<QCSummaryWebPart.QcSummaryTile> subFolderTile = qcSummary.getQcSummaryTiles();
        Assert.assertEquals("Incorrect metric displayed for " + subFolderTile.get(1).getFolderName(), 8, subFolderTile.get(1).getMetricsCount());
        Assert.assertEquals("Incorrect metric displayed for " + subFolderTile.get(2).getFolderName(), 10, subFolderTile.get(2).getMetricsCount());
    }

    @Test
    public void testBadMetricQuery()
    {
        // Set up a metric
        String metricName = "BadMetric";
        createQuery(getProjectName(), metricName, "targetedms", "SELECT * FROM AQCTest_Metric", null,  false);
        ConfigureMetricsUIPage configureQCMetrics = goToDashboard().getQcPlotsWebPart().clickConfigureQCMetrics();
        configureQCMetrics.addNewCustomMetric(Map.of(
                ConfigureMetricsUIPage.CustomMetricProperties.metricName, metricName,
                ConfigureMetricsUIPage.CustomMetricProperties.queryName, metricName,
                ConfigureMetricsUIPage.CustomMetricProperties.yAxisLabel, "Label",
                ConfigureMetricsUIPage.CustomMetricProperties.metricType, ConfigureMetricsUIPage.MetricType.Precursor.name()), false);

        // Break the query and force a recaching
        goToSchemaBrowser();
        editQuerySource("targetedms", metricName).setSource("SELECT * FROM AQCTest_Metric_Bad").clickSaveExpectingError();
        configureQCMetrics = goToDashboard().getQcPlotsWebPart().clickConfigureQCMetrics();
        configureQCMetrics.clearMetricCache();

        // Verify helpful error and then delete the bad metric
        refresh();
        waitForText("Failed to calculate metric values");
        clickAndWait(Locator.linkWithText("View QC metrics table"));
        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.setFilter("Name", "Equals", metricName);
        table.checkCheckbox(0);
        table.deleteSelectedRows();

        goToDashboard().getQcPlotsWebPart().clickConfigureQCMetrics();
        assertTextNotPresent(metricName);
    }

    @Test
    public void testFixedDeviationFromMeanOption()
    {
        goToProjectHome();

        QCPlotsWebPart.MetricType metricType = QCPlotsWebPart.MetricType.TRANSITION_AREA;
        ConfigureMetricsUIPage configureQCMetrics = goToDashboard().getQcSummaryWebPart().clickConfigureQCMetrics();

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
        qcPlotsWebPart.setMetric1Type(metricType);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);

        String replicate = "Q_Exactive_08_09_2013_JGB_87";

        //TODO: add the verification steps.
    }

    @Test
    public void testFixedValueCutOffOption()
    {
        goToProjectHome();

        QCPlotsWebPart.MetricType metric = QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR;
        ConfigureMetricsUIPage configureQCMetrics = goToDashboard().getQcSummaryWebPart().clickConfigureQCMetrics();

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
        qcPlotsWebPart.setMetric1Type(metric);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);
        qcPlotsWebPart.waitForPlots(6);

        String replicate = "Q_Exactive_08_14_2013_JGB_54";
        verifyOutlierCount(replicate, metric, "1");
    }

    @Test
    public void testPlotOnlyOption()
    {
        goToProjectHome();

        QCPlotsWebPart.MetricType metric = QCPlotsWebPart.MetricType.ISOTOPE_DOTP;
        ConfigureMetricsUIPage configureQCMetrics = goToDashboard().getQcSummaryWebPart().clickConfigureQCMetrics();
        configureQCMetrics.setShowMetricNoOutlier(metric);
        configureQCMetrics.clickSave();

        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.setMetric1Type(metric);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);

        String replicate = "Q_Exactive_08_14_2013_JGB_54";
        verifyOutlierCount(replicate, metric, "N/A");
    }

    @Test
    public void testAnnotationBackedMetric()
    {
        String subfolderName = "AnnotationMetricsFolder";
        String metricName = "Test Annotation-Backed Metric";
        String yAxisLabel = "R Squared";
        String updatedYAxisLabel = "Updated R Squared";

        log("Create subfolder and import data with numeric precursor_result annotations");
        setupSubfolder(getProjectName(), subfolderName, FolderType.QC);
        importData(ISOTOPOLOGUE_FILE_ANNOTATED);
        navigateToFolder(getProjectName(), subfolderName);

        log("Add annotation-backed metric backed by the RSquared precursor annotation");
        ConfigureMetricsUIPage configureQCMetrics = goToDashboard().getQcSummaryWebPart().clickConfigureQCMetrics();
        configureQCMetrics.addNewAnnotationMetric(Map.of(
                ConfigureMetricsUIPage.AnnotationMetricProperties.metricName, metricName,
                ConfigureMetricsUIPage.AnnotationMetricProperties.yAxisLabel, yAxisLabel,
                ConfigureMetricsUIPage.AnnotationMetricProperties.annotationType, "precursor",
                ConfigureMetricsUIPage.AnnotationMetricProperties.annotationName, "RSquared"), false);

        log("Verify metric appears in configure QC metrics table");
        waitForElement(Locator.linkWithText(metricName));

        goToDashboard();
        log("Verify metric appears in QC plots dropdown");
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        Assert.assertTrue("Annotation-backed metric should appear in QC plots dropdown",
                verifyMetricIsPresent(qcPlotsWebPart, metricName));

        log("Test that a duplicate metric name is rejected");
        configureQCMetrics = goToDashboard().getQcSummaryWebPart().clickConfigureQCMetrics();
        configureQCMetrics.addNewAnnotationMetric(Map.of(
                ConfigureMetricsUIPage.AnnotationMetricProperties.metricName, metricName,
                ConfigureMetricsUIPage.AnnotationMetricProperties.yAxisLabel, "Duplicate Label",
                ConfigureMetricsUIPage.AnnotationMetricProperties.annotationType, "precursor",
                ConfigureMetricsUIPage.AnnotationMetricProperties.annotationName, "RSquared"), true);

        log("Edit the annotation-backed metric's y-axis label (still on same page after cancel)");
        configureQCMetrics.editAnnotationMetric(metricName, Map.of(
                ConfigureMetricsUIPage.AnnotationMetricProperties.yAxisLabel, updatedYAxisLabel));

        log("Delete the annotation-backed metric");
        configureQCMetrics = goToDashboard().getQcSummaryWebPart().clickConfigureQCMetrics();
        configureQCMetrics.deleteAnnotationMetric(metricName);
        assertTextNotPresent(metricName);
    }

    private void verifyOutlierCount(String replicate, QCPlotsWebPart.MetricType metricType, String count)
    {
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        qcSummaryWebPart.gotoUtilizationCalendar();
        qcSummaryWebPart = new QCSummaryWebPart(getDriver());
        waitForElement(Locator.tagContainingText("div", replicate));
        scrollIntoView(Locator.tagContainingText("div", replicate));
        mouseOver(Locator.tagContainingText("div", replicate));
        mouseOver(Locator.tagContainingText("div", replicate));
        waitForElement(qcSummaryWebPart.getBubble());
        WebElement bubbleContentEl = qcSummaryWebPart.getBubbleContent().findElement(getDriver());
        waitFor(()-> !bubbleContentEl.getText().isEmpty(), WAIT_FOR_PAGE);
        String bubbleContent = bubbleContentEl.getText();
        log("Bubble content " + bubbleContent);
        checker().verifyTrue("Outlier count for metric " + metricType + " is not correct",
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
