package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locators;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.support.ui.ExpectedConditions;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSTrailingMeanAndCVTest extends TargetedMSTest
{
    private final int RUN_COUNT = 47;
    private final int REPLICATE_COUNT = 7;

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

        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.moveWebPart("QC Plots", PortalHelper.Direction.UP);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Before
    public void deletingGuideSet()
    {
        goToProjectHome();
        removeAllGuideSets();
    }

    @Test
    public void testTrailingMeanPlotType()
    {
        String trailingLast;
        goToProjectHome();
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingMean);

        log("Verifying error checking on trailing last number");
        trailingLast = "-1";
        qcPlotsWebPart.setTrailingLast(trailingLast);
        longWait().until(ExpectedConditions.textToBePresentInElement(Locators.labkeyError.findWhenNeeded(getDriver()),
                "TrailingMean - Please enter a positive integer (>2) that is less than or equal to total number of available runs - 47"));

        trailingLast = "48";
        qcPlotsWebPart.setTrailingLast(trailingLast);
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.LeveyJennings, QCPlotsWebPart.QCPlotType.TrailingMean);
        longWait().until(ExpectedConditions.textToBePresentInElement(Locators.labkeyError.findWhenNeeded(getDriver()),
                "TrailingMean - The number you entered is larger than the number of available runs. Only 47 runs are used for calculation"));
        checker().verifyEquals("Incorrect plot count", REPLICATE_COUNT , qcPlotsWebPart.getPlots().size());
        checker().verifyEquals("Incorrect number of times error message was displayed", REPLICATE_COUNT,
                Locators.labkeyError.findElements(getDriver()).size());

        log("Verifying y-axis value based on metric type");
        trailingLast = "3";
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingMean);
        qcPlotsWebPart.setTrailingLast(trailingLast);
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR);
        Assert.assertTrue("Y axis is not labeled correctly for " + QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR,
                qcPlotsWebPart.getSVGPlotText("precursorPlot0").contains("PPM"));

        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.RETENTION);
        Assert.assertTrue("Y axis is not labeled correctly for " + QCPlotsWebPart.MetricType.RETENTION,
                qcPlotsWebPart.getSVGPlotText("precursorPlot0").contains("Minutes"));

        log("Verifying the count of points on the plot");
        Assert.assertEquals("Invalid point count for all replicates", REPLICATE_COUNT * RUN_COUNT, qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size());

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
                "2013-08-09 11:39 - 2013-08-12 04:54", toolTipText);

        log("Selecting multiple plot types with trailing mean");
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.LeveyJennings, QCPlotsWebPart.QCPlotType.MovingRange, QCPlotsWebPart.QCPlotType.TrailingMean);
        Assert.assertEquals("Incorrect number of plots displayed when multiple plot type are selected", 21, qcPlotsWebPart.getTotalPlotCount());

        log("Verifying values with guide set");
        createGuideSetFromTable(new GuideSet("2013/08/09 23:59", "2013-08-15 00:00:04", "Trailing mean guide set"));
        goToProjectHome();
        dashboard = new PanoramaDashboard(this);
        qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingMean);
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate("2013-08-14 00:44:46"));
        toolTipText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        Assert.assertEquals("Invalid tooltip of the point in guide set", "Peptide:\n" +
                "ATEEQLK ++, 409.7163\n" +
                "Value:\n" +
                "15.684\n" +
                "Replicate:\n" +
                "3 runs average\n" +
                "Acquired:\n" +
                "2013-08-11 18:34 - 2013-08-14 00:44", toolTipText);

        log("Verifying the count of points on the plots with guide set");
        Assert.assertEquals("Invalid point count for all replicates", 322 , qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size());
    }

    @Test
    public void testTrailingCVPlotType()
    {
        String trailingLast;
        goToProjectHome();
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingCV);

        log("Verifying error checking on trailing last number");
        trailingLast = "-1";
        qcPlotsWebPart.setTrailingLast(trailingLast);
        longWait().until(ExpectedConditions.textToBePresentInElement(Locators.labkeyError.findWhenNeeded(getDriver()),
                "TrailingCV - Please enter a positive integer (>2) that is less than or equal to total number of available runs - 47"));

        trailingLast = "48";
        qcPlotsWebPart.setTrailingLast(trailingLast);
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.LeveyJennings, QCPlotsWebPart.QCPlotType.TrailingCV);
        longWait().until(ExpectedConditions.textToBePresentInElement(Locators.labkeyError.findWhenNeeded(getDriver()),
                "TrailingCV - The number you entered is larger than the number of available runs. Only 47 runs are used for calculation"));
        checker().verifyEquals("Incorrect plot count", REPLICATE_COUNT , qcPlotsWebPart.getPlots().size());
        checker().verifyEquals("Incorrect number of times error message was displayed", REPLICATE_COUNT,
                Locators.labkeyError.findElements(getDriver()).size());

        trailingLast = "3";
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingCV);
        qcPlotsWebPart.setTrailingLast(trailingLast);
        log("Verifying tooltips");
        qcPlotsWebPart.waitForPlots(7);
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate("2013-08-12 04:54:55"));
        String toolTipText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        Assert.assertEquals("Invalid tooltip", "Peptide:\n" +
                "ATEEQLK ++, 409.7163\n" +
                "Value:\n" +
                "6.831\n" +
                "Replicate:\n" +
                "3 runs average\n" +
                "Acquired:\n" +
                "2013-08-09 11:39 - 2013-08-12 04:54", toolTipText);
        qcPlotsWebPart.closeBubble();
        log("Verifying the count of points on the plot");
        Assert.assertEquals("Invalid point count for all replicates", REPLICATE_COUNT * RUN_COUNT,
                qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size());

        log("Verifying values with guide set");
        createGuideSetFromTable(new GuideSet("2013-08-21 04:46", "2013-08-21 13:15", "Trailing CV guide set"));
        goToProjectHome();
        dashboard = new PanoramaDashboard(this);
        qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setQCPlotTypes(QCPlotsWebPart.QCPlotType.TrailingCV);
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate("2013-08-21 09:07:36"));
        toolTipText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        Assert.assertEquals("Invalid tooltip of the point in guide set", "Peptide:\n" +
                "ATEEQLK ++, 409.7163\n" +
                "Value:\n" +
                "3.704\n" +
                "Replicate:\n" +
                "3 runs average\n" +
                "Acquired:\n" +
                "2013-08-21 04:46 - 2013-08-21 09:07", toolTipText);
        qcPlotsWebPart.closeBubble();

        log("Verifying the count of points on the plot with guide set");
        Assert.assertEquals("Invalid number of point count for all replicates - plots with guide set", REPLICATE_COUNT * RUN_COUNT ,
                qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size());
    }
}
