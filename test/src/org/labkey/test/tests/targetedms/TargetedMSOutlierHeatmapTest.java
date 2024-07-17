package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.OutlierHeatmapSummaryWebPart;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSOutlierHeatmapTest extends TargetedMSTest
{
    private static final String OUTLIER_HEATMAP_TAB = "Outlier Heatmap Summary";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSOutlierHeatmapTest init = (TargetedMSOutlierHeatmapTest) getCurrentTest();
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

        PortalHelper _portalHelper = new PortalHelper(getDriver());
        _portalHelper.enterAdminMode();
        _portalHelper.deleteTab("Raw Data");
        _portalHelper.deleteTab("Pareto Plot");
        _portalHelper.deleteTab("Annotations");
        _portalHelper.addTab(OUTLIER_HEATMAP_TAB);

        _portalHelper.addWebPart("Outlier Heatmap Summary View");
        _portalHelper.exitAdminMode();
    }

    @Test
    public void testHeatMapColorAndValues()
    {
        log("Updating the Metric type values");
        goToProjectHome();
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);
        configureQCMetrics.setFixedDeviationFromMean(QCPlotsWebPart.MetricType.PRECURSOR_AREA, "-5", "5")
                .clickSave();

        clickPortalTab(OUTLIER_HEATMAP_TAB);
        OutlierHeatmapSummaryWebPart outlierHeatmap = new OutlierHeatmapSummaryWebPart(getDriver());
        Assert.assertEquals("Incorrect outlier count", "2",
                outlierHeatmap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());
        Assert.assertEquals("Incorrect total replicate count", "47", outlierHeatmap.getTotalReplicateCount().trim());

        log("Verify data range: " + OutlierHeatmapSummaryWebPart.HeatmapDateRange.Last_7_Days);
        outlierHeatmap.setDateRange(OutlierHeatmapSummaryWebPart.HeatmapDateRange.Last_7_Days);
        Assert.assertEquals("Incorrect outlier count for " + QCPlotsWebPart.MetricType.FWHM, "1",
                outlierHeatmap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());

        log("Verify heatmap colors");
        Assert.assertTrue("Incorrect heatmap color for highest(Red)",
                outlierHeatmap.getCellElement(1, QCPlotsWebPart.MetricType.PRECURSOR_AREA).getAttribute("style").contains("background-color: rgb(255, 0, 0)"));
        Assert.assertTrue("Incorrect heatmap color for lowest(White)",
                outlierHeatmap.getCellElement(1, QCPlotsWebPart.MetricType.ISOTOPE_DOTP).getAttribute("style").contains("background-color: rgb(255, 255, 255)"));
        Assert.assertTrue("Incorrect heatmap color for middle range(Lighter red)",
                outlierHeatmap.getCellElement(1, QCPlotsWebPart.MetricType.RETENTION).getAttribute("style").contains("background-color: rgb(255, 182, 182)"));

        log("Verify Custom date range");
        outlierHeatmap.setDateRange(OutlierHeatmapSummaryWebPart.HeatmapDateRange.Custom_Range)
                .setStartDate("08/01/2013")
                .setEndDate("08/15/2013");
        Assert.assertEquals("Incorrect outlier count for " + QCPlotsWebPart.MetricType.PRECURSOR_AREA, "11",
                outlierHeatmap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());
    }

    @Test
    public void testMetricUsageOutlierHeatmap()
    {
        goToProjectHome();
        clickPortalTab(OUTLIER_HEATMAP_TAB);
        OutlierHeatmapSummaryWebPart outlierHeatmap = new OutlierHeatmapSummaryWebPart(getDriver());
        Assert.assertEquals("Missing Metric type in the heatmap", Arrays.asList("", "Full Width at Base (FWB)",
                        "Full Width at Half Maximum (FWHM)", "Isotope dotp", "Precursor Area", "Precursor Mass Error",
                        "Retention Time", "TIC Area", "Total Peak Area (Precursor + Transition)", "Transition & Precursor Areas",
                        "Transition Area", "Transition Mass Error", "Transition/Precursor Area Ratio", "Total")
                , outlierHeatmap.getHeatmapTable().getTableHeaderTexts());

        log("Updating the Metric type values");
        goToProjectHome();
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);
        configureQCMetrics.setShowMetricNoOutlier(QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR)
                .setFixedValueCutOff(QCPlotsWebPart.MetricType.RETENTION, "-15", "15")
                .disableMetric(QCPlotsWebPart.MetricType.TOTAL_PEAK.toString())
                .clickSave();

        clickPortalTab(OUTLIER_HEATMAP_TAB);
        outlierHeatmap = new OutlierHeatmapSummaryWebPart(getDriver());
        Assert.assertEquals("Metric type " + QCPlotsWebPart.MetricType.TOTAL_PEAK + " should not be present",
                Arrays.asList("", "Full Width at Base (FWB)", "Full Width at Half Maximum (FWHM)", "Isotope dotp", "Precursor Area",
                        "Precursor Mass Error", "Retention Time", "TIC Area", "Transition & Precursor Areas", "Transition Area",
                        "Transition Mass Error", "Transition/Precursor Area Ratio", "Total"),
                outlierHeatmap.getHeatmapTable().getTableHeaderTexts());
        Assert.assertEquals("Outlier value should not be calculated for " + QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR,
                Arrays.asList("0", "0", "0", "0", "0", "0", "0", "0", "0"),
                outlierHeatmap.getHeatmapTable().getTableHeaderColumnData(QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR.toString()));
        Assert.assertEquals("Incorrect value for fixed value cutoff in " + QCPlotsWebPart.MetricType.RETENTION,
                Arrays.asList("23", "47", "26", "47", "0", "47", "47", "47", "284"),
                outlierHeatmap.getHeatmapTable().getTableHeaderColumnData(QCPlotsWebPart.MetricType.RETENTION.toString()));
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMSOutlierHeatmapTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
