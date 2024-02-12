package org.labkey.test.tests.panoramapremium;

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
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSiRTMetricsTest extends TargetedMSPremiumTest
{
    protected static final String NON_IRTMETRICS_SKYFILE = "QC_1.sky.zip";
    protected static final String IRTMETRICS_SKYFILE = "DIA-QE-Bruderer-iRT-minimized.sky.zip";
    String subFolderName = "iRTMetric";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSiRTMetricsTest init = (TargetedMSiRTMetricsTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
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

        log("Verify no data for iRT metric in Configure Metrics UI");
        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.verifyNoDataForMetric("iRT Correlation");
        configureUI.verifyNoDataForMetric("iRT Intercept");
        configureUI.verifyNoDataForMetric("iRT Slope");
        configureUI.clickSave();
    }

    @Test
    public void testFileWithIRTMetricValue()
    {
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
        checker().withScreenshot("IRTCorrelation").verifyTrue("Incorrect iRT Correlation value calculated", ticAreaHoverText.contains("0.999"));
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
        checker().withScreenshot("IRTSlope").verifyTrue("Incorrect iRT Slope value calculated", ticAreaHoverText.contains("0.646"));
        qcPlotsWebPart.closeBubble();
        mouseOut();

        log("Verifying the tooltip area of QC summary web part");
        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();
        QCSummaryWebPart.QcSummaryTile qcSummaryTile = qcSummaryWebPart.getQcSummaryTiles().get(0);
        mouseOver(qcSummaryTile.getRecentSampleFiles().get(0));
        final WebElement bubble = waitForElement(qcSummaryWebPart.getBubble().withDescendant(Locator.linkWithText("sample8_R03")));
        checker().verifyTrue("iRT Correlation missing in tooltip", Locator.linkWithText("iRT Correlation").existsIn(bubble));
        checker().verifyTrue("iRT Intercept missing in tooltip", Locator.linkWithText("iRT Intercept").existsIn(bubble));
        checker().verifyTrue("iRT Slope missing in tooltip", Locator.linkWithText("iRT Slope").existsIn(bubble));

        /*
            Test coverage for Issue 37745, "Synchronize Skyline and Panorama plot colors,"
         */
        goToProjectFolder(getProjectName(), subFolderName);
        clickTab("Runs");
        clickAndWait(Locator.linkWithText(IRTMETRICS_SKYFILE));
        clickAndWait(Locator.linkWithText("Biognosys standards"));

        final String peptide1 = "LFLQFGAQGSPFLK";
        final String peptide1Color = "#804E00";
        final String peptide2 = "YILAGVENSK";
        final String peptide2Color = "#90BDDA";

        Assert.assertEquals("Plot color for " + peptide1, peptide1Color, getChromatogramPlotColor(peptide1));
        Assert.assertEquals("Plot color for " + peptide2, peptide2Color, getChromatogramPlotColor(peptide2));

        navigateToFolder(getProjectName(), subFolderName);
        qcDashboard = new PanoramaDashboard(this);
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.RETENTION);
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);

        verifyQCPlotColors(peptide1, peptide1Color);
        verifyQCPlotColors(peptide2, peptide2Color);
    }

    private static final Pattern rgbPattern = Pattern.compile("rgba?\\((?<r>[0-9]+), ?(?<g>[0-9]+), ?(?<b>[0-9]+).*\\)");
    /**
     * Convert a CSS color style into a consistent format.
     *  - Hex: '#FFFFFF'
     *  - RGB: 'rgb(r, g, b)'
     *  - RGBA: 'rgba(r, g, b, a)'
     * @param color color value from a web element
     * @return Hex value of the provided color
     */
    public static String hexColor(String color)
    {
        if (color.startsWith("#"))
        {
            if (color.length() != 7)
            {
                throw new IllegalArgumentException("Invalid HEX color value (not 3x8 bit hex string '#FFFFFF'): " + color);
            }
            return color;
        }
        else // rgb/rgba
        {
            Matcher matcher = rgbPattern.matcher(color);
            if (matcher.matches())
            {
                StringBuilder hexColor = new StringBuilder("#");
                {
                    // Red
                    int rInt = Integer.parseInt(matcher.group("r"));
                    String r = Integer.toHexString(rInt);
                    if (r.length() < 2)
                        hexColor.append("0");
                    hexColor.append(r);
                }
                {
                    // Green
                    int gInt = Integer.parseInt(matcher.group("g"));
                    String g = Integer.toHexString(gInt);
                    if (g.length() < 2)
                        hexColor.append("0");
                    hexColor.append(g);
                }
                {
                    // Blue
                    int bInt = Integer.parseInt(matcher.group("b"));
                    String b = Integer.toHexString(bInt);
                    if (b.length() < 2)
                        hexColor.append("0");
                    hexColor.append(b);
                }

                if (hexColor.length() != 7)
                {
                    throw new IllegalArgumentException("Invalid HEX color value (not 3x8 bit hex string '#FFFFFF'): " + color);
                }

                return hexColor.toString().toUpperCase();
            }

            throw new IllegalArgumentException("Can't parse color (should be hex, rgb, or rgba value): " + color);
        }
    }

    private String getChromatogramPlotColor(String peptide)
    {
        final String cssColor = longWait().ignoring(WebDriverException.class).until(wd ->
            Locator.id("groupChromatogramLegend")
                .append(Locator.tag("td").withPredicate("text()=" + Locator.XPathLocator.xq(peptide)).childTag("span"))
                .findElement(wd)
                .getCssValue("color")
        );
        return hexColor(cssColor);
    }

    private void verifyQCPlotColors(String peptide, String color)
    {
        List<WebElement> legendList = Locator.tagWithClass("g", "legend-item").findElements(getDriver());
        for (WebElement e : legendList)
        {
            if (e.getAttribute("title").startsWith(peptide + " "))
            {
                checker().verifyEquals("Incorrect color for QC plot for " + peptide, color,
                        Locator.tag("path").findElement(e).getAttribute("fill"));
                return;
            }

        }
        throw new RuntimeException("Did not find the peptide " + peptide);
    }
}
