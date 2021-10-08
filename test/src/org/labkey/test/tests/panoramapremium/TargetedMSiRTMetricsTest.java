package org.labkey.test.tests.panoramapremium;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Git;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;

import java.util.Arrays;

@Category(Git.class)
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class TargetedMSiRTMetricsTest extends TargetedMSPremiumTest
{
    protected static final String NON_IRTMETRICS_SKYFILE = "QC_1.sky.zip";
    protected static final String IRTMETRICS_SKYFILE = "DIA-QE-Bruderer-iRT-minimized.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSiRTMetricsTest init = (TargetedMSiRTMetricsTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _containerHelper.enableModules(Arrays.asList("Dumbster", "PanoramaPremium"));
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Test
    public void testFileWithoutIRTMetricValues()
    {
        String subFolderName = "Non iRTMetric";
        goToProjectHome();
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC);

        navigateToFolder(getProjectName(), subFolderName);
        importData(NON_IRTMETRICS_SKYFILE);

        navigateToFolder(getProjectName(), subFolderName);
        log("Verify iRT metric value not present with Auto");
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        verifyMetricNotPresent(qcPlotsWebPart, "iRT Correlation");
        verifyMetricNotPresent(qcPlotsWebPart, "iRT Intercept");
        verifyMetricNotPresent(qcPlotsWebPart, "iRT Slope");

        log("Enabling the metric values");
        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.enableMetric("iRT Correlation");
        configureUI.enableMetric("iRT Intercept");
        configureUI.enableMetric("iRT Slope");
        configureUI.clickSave();

        log("Verifying iRT metric present after enabling");
        verifyMetricIsPresent(qcPlotsWebPart, "iRT Correlation");
        verifyMetricIsPresent(qcPlotsWebPart, "iRT Intercept");
        verifyMetricIsPresent(qcPlotsWebPart, "iRT Slope");
    }

    @Test
    public void testFileWithIRTMetricValue()
    {
        String subFolderName = "iRTMetric";
        goToProjectHome();
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC);

        navigateToFolder(getProjectName(), subFolderName);
        importData(IRTMETRICS_SKYFILE);

        log("Verifying iRT metric present upon import");
        navigateToFolder(getProjectName(), subFolderName);
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        verifyMetricIsPresent(qcPlotsWebPart, "iRT Correlation");
        verifyMetricIsPresent(qcPlotsWebPart, "iRT Intercept");
        verifyMetricIsPresent(qcPlotsWebPart, "iRT Slope");

        log("Verifying the iRT Correlation plot values");
        String acquiredDate = "2014-03-17 06:46:14";
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.IRTCORRELATION);
        checker().verifyEquals("Incorrect plot displayed for iRT Correlation metric", Arrays.asList("iRT Correlation"), qcPlotsWebPart.getPlotTitles());
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate(acquiredDate));
        waitForElement(qcPlotsWebPart.getBubble());
        String ticAreaHoverText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        checker().withScreenshot("IRTCorrelation").verifyTrue("Incorrect iRT Correlation value calculated", ticAreaHoverText.contains("0.9992"));
        qcPlotsWebPart.closeBubble();

        log("Verifying the iRT Intercept plot values");
        acquiredDate = "2014-03-16 05:21:58";
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.IRTINTERCEPT);
        checker().verifyEquals("Incorrect plot displayed for iRT Intercept metric", Arrays.asList("iRT Intercept"), qcPlotsWebPart.getPlotTitles());
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate(acquiredDate));
        waitForElement(qcPlotsWebPart.getBubble());
        ticAreaHoverText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        checker().withScreenshot("IRTIntercept").verifyTrue("Incorrect  iRT Intercept value calculated", ticAreaHoverText.contains("34.2"));
        qcPlotsWebPart.closeBubble();

        log("Verifying the iRT Slope plot values");
        acquiredDate = "2014-03-17 06:46:14";
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.IRTSLOPE);
        checker().verifyEquals("Incorrect plot displayed for iRT Slope metric", Arrays.asList("iRT Slope"), qcPlotsWebPart.getPlotTitles());
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate(acquiredDate));
        waitForElement(qcPlotsWebPart.getBubble());
        ticAreaHoverText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        checker().withScreenshot("IRTSlope").verifyTrue("Incorrect iRT Slope value calculated", ticAreaHoverText.contains("0.6463"));
        qcPlotsWebPart.closeBubble();

        log("Verifying the tooltip area of QC summary web part");
        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();
        QCSummaryWebPart.QcSummaryTile qcSummaryTile = qcSummaryWebPart.getQcSummaryTiles().get(0);
        mouseOver(qcSummaryTile.getRecentSampleFiles().get(0));
        waitForElement(qcSummaryWebPart.getBubble());
        checker().verifyTrue("iRT Correlation missing in tooltip", isElementPresent(Locator.linkWithText("iRT Correlation")));
        checker().verifyTrue("iRT Intercept missing in tooltip", isElementPresent(Locator.linkWithText("iRT Intercept")));
        checker().verifyTrue("iRT Slope missing in tooltip", isElementPresent(Locator.linkWithText("iRT Slope")));
    }
}
