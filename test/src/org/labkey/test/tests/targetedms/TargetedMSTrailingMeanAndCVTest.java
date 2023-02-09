package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.openqa.selenium.support.ui.ExpectedConditions;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSTrailingMeanAndCVTest extends TargetedMSTest
{
    private static final GuideSet gs1 = new GuideSet("2013/08/01", "2013/08/01 00:00:01", "first guide set, entirely before initial data with no data points in range");

    @BeforeClass
    public static void initProject()
    {
        TargetedMSTrailingMeanAndCVTest init = (TargetedMSTrailingMeanAndCVTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        importData(SProCoP_FILE);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Test
    public void testTrailingMeanPlotType()
    {
        goToProjectHome();
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingMean);

        log("Verifying error checking on trailing last number");
        Assert.assertEquals("Incorrect trailing last default value", "10", qcPlotsWebPart.getTrailingLast());

        qcPlotsWebPart.setTrailingLast("-1");
        shortWait().until(ExpectedConditions.elementToBeClickable(Locators.labkeyError.findWhenNeeded(getDriver())));
        Assert.assertEquals("Invalid trailing last number(-1) did not throw an error",
                "TrailingMean - Please enter a positive integer (>2) that is less than or equal to total number of available runs - 47"
                , Locator.tagWithClass("span", "labkey-error").findElement(getDriver()).getText());

        qcPlotsWebPart.setTrailingLast("48");
        waitForElementWithRefresh(Locators.labkeyError, WAIT_FOR_JAVASCRIPT);
        shortWait().until(ExpectedConditions.elementToBeClickable(Locators.labkeyError.findWhenNeeded(getDriver())));
        Assert.assertEquals("Invalid trailing last number(48) did not throw an error",
                "TrailingMean - The number you entered is larger than the number of available runs. Only 47 runs are used for calculation"
                , Locator.tagWithClass("span", "labkey-error").findElement(getDriver()).getText());

        log("Verifying y-axis value based on metric type");
        qcPlotsWebPart.setTrailingLast("3");
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.MASSACCURACY);
        Assert.assertTrue("Y axis is not labeled correctly for " + QCPlotsWebPart.MetricType.MASSACCURACY,
                qcPlotsWebPart.getSVGPlotText("precursorPlot0").contains("PPM"));

        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.RETENTION);
        Assert.assertTrue("Y axis is not labeled correctly for " + QCPlotsWebPart.MetricType.RETENTION,
                qcPlotsWebPart.getSVGPlotText("precursorPlot0").contains("Minutes"));

        log("Verifying tooltips");
        qcPlotsWebPart.waitForPlots(7);
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate("2013-08-12 04:54:55"));
        String toolTipText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        Assert.assertEquals("Invalid tooltip", "Peptide:\n" +
                "ATEEQLK ++, 409.7163\n" +
                "Value:\n" +
                "15.652\n" +
                "Replicate:\n" +
                "3 runs average\n" +
                "Acquired:\n" +
                "2013-08-09 11:39:00.000 - 2013-08-12 04:54:55", toolTipText);

        log("Selecting multiple plot types with trailing mean");
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.LeveyJennings, QCPlotsWebPart.QCPlotType.MovingRange, QCPlotsWebPart.QCPlotType.TrailingMean);
        Assert.assertEquals("Incorrect number of plots displayed when multiple plot type are selected", 21, qcPlotsWebPart.getTotalPlotCount());

        log("Verifying values with guide set");
        createGuideSetFromTable(new GuideSet("2013/08/09 23:59", "2013-08-15 00:00:04", "Trailing mean guide set"));

    }

    @Test
    public void testTrailingCVPlotType()
    {
        goToProjectHome();
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingCV);

        log("Verifying error checking on trailing last number");
        Assert.assertEquals("Incorrect trailing last default value", "10", qcPlotsWebPart.getTrailingLast());

        qcPlotsWebPart.setTrailingLast("-1");
        shortWait().until(ExpectedConditions.elementToBeClickable(Locators.labkeyError.findWhenNeeded(getDriver())));
        Assert.assertEquals("Incorrect error message for -1",
                "TrailingCV - Please enter a positive integer (>2) that is less than or equal to total number of available runs - 47"
                , Locator.tagWithClass("span", "labkey-error").findElement(getDriver()).getText());

        qcPlotsWebPart.setTrailingLast("48");
        shortWait().until(ExpectedConditions.elementToBeClickable(Locators.labkeyError.findWhenNeeded(getDriver())));
        Assert.assertEquals("Incorrect error message for 48",
                "TrailingCV - The number you entered is larger than the number of available runs. Only 47 runs are used for calculation"
                , Locator.tagWithClass("span", "labkey-error").findElement(getDriver()).getText());

        qcPlotsWebPart.setTrailingLast("5");
        log("Verifying tooltips");
        qcPlotsWebPart.waitForPlots(7);
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate("2013-08-16 10:38:57"));
        String toolTipText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        Assert.assertEquals("Invalid tooltip", "Peptide:\n" +
                "ATEEQLK ++, 409.7163\n" +
                "Value:\n" +
                "0.245\n" +
                "Replicate:\n" +
                "5 runs average\n" +
                "Acquired:\n" +
                "2013-08-15 03:34:19.000 - 2013-08-16 10:38:57", toolTipText);

        log("Verifying values with guide set");
        createGuideSetFromTable(new GuideSet("2013/08/09 23:59", "2013-08-15 00:00:04", "Trailing CV guide set"));
    }
}
