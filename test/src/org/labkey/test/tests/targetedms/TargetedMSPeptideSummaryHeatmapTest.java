package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.PeptideSummaryWebPart;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSPeptideSummaryHeatmapTest extends TargetedMSTest
{
    private static final String PEPTIDE_MOLECULE_SUMMARY = "Peptide/Molecule Summary";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSPeptideSummaryHeatmapTest init = (TargetedMSPeptideSummaryHeatmapTest) getCurrentTest();
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
        _portalHelper.addTab(PEPTIDE_MOLECULE_SUMMARY);

        _portalHelper.addWebPart("Peptide/Molecule Summary");
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

        clickPortalTab(PEPTIDE_MOLECULE_SUMMARY);
        PeptideSummaryWebPart peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Incorrect outlier count", "2",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());
        Assert.assertEquals("Incorrect total replicate count", "47", peptideSummaryHeatMap.getTotalReplicateCount().trim());

        log("Verify data range: " + PeptideSummaryWebPart.HeatmapDateRange.Last_7_Days);
        peptideSummaryHeatMap.setDateRange(PeptideSummaryWebPart.HeatmapDateRange.Last_7_Days);
        Assert.assertEquals("Incorrect outlier count for " + QCPlotsWebPart.MetricType.FWHM, "1",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());

        log("Verify heatmap colors");
        Assert.assertEquals("Incorrect heatmap color for highest(Red)", "rgb(255, 0, 0)",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.PRECURSOR_AREA).getCssValue("background-color"));
        Assert.assertEquals("Incorrect heatmap color for lowest(White)", "rgb(0, 0, 0)",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.ISOTOPE_DOTP).getCssValue("background-color"));

        log("Verify Custom date range");
        peptideSummaryHeatMap.setCustomDateRange("2013-08-01","2013-08-15");
        Assert.assertEquals("Incorrect outlier count for " + QCPlotsWebPart.MetricType.PRECURSOR_AREA, "11",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.PRECURSOR_AREA).getText());
    }

    @Test
    public void testMetricUsagePeptideSummaryHeatmap()
    {
        goToProjectHome();
        clickPortalTab(PEPTIDE_MOLECULE_SUMMARY);
        PeptideSummaryWebPart peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Missing Metric type in the heatmap", Arrays.asList("", "Full Width at Base (FWB)",
                        "Full Width at Half Maximum (FWHM)", "Isotope dotp", "Precursor Area", "Precursor Mass Error",
                        "Retention Time", "TIC Area", "Total Peak Area (Precursor + Transition)", "Transition & Precursor Areas",
                        "Transition Area", "Transition Mass Error", "Transition/Precursor Area Ratio", "Total")
                , peptideSummaryHeatMap.getHeatmapTable().getTableHeaderTexts());

        log("Updating the Metric type values");
        goToProjectHome();
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        ConfigureMetricsUIPage configureQCMetrics = new ConfigureMetricsUIPage(this);
        configureQCMetrics.setShowMetricNoOutlier(QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR)
                .setFixedValueCutOff(QCPlotsWebPart.MetricType.RETENTION, "-15", "15")
                .disableMetric(QCPlotsWebPart.MetricType.TOTAL_PEAK.toString())
                .clickSave();

        clickPortalTab(PEPTIDE_MOLECULE_SUMMARY);
        peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Metric type " + QCPlotsWebPart.MetricType.TOTAL_PEAK + " should not be present",
                Arrays.asList("", "Full Width at Base (FWB)", "Full Width at Half Maximum (FWHM)", "Isotope dotp", "Precursor Area",
                        "Precursor Mass Error", "Retention Time", "TIC Area", "Transition & Precursor Areas", "Transition Area",
                        "Transition Mass Error", "Transition/Precursor Area Ratio", "Total"),
                peptideSummaryHeatMap.getHeatmapTable().getTableHeaderTexts());
        Assert.assertEquals("Outlier value should not be calculated for " + QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR,
                Arrays.asList("0", "0", "0", "0", "0", "0", "0", "0", "0"),
                peptideSummaryHeatMap.getHeatmapTable().getTableHeaderColumnData(QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR.toString()));
        Assert.assertEquals("Incorrect value for fixed value cutoff in " + QCPlotsWebPart.MetricType.RETENTION,
                Arrays.asList("23", "47", "26", "47", "47", "47", "47", "0", "284"),
                peptideSummaryHeatMap.getHeatmapTable().getTableHeaderColumnData(QCPlotsWebPart.MetricType.RETENTION.toString()));
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMSPeptideSummaryHeatmapTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
