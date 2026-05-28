/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.targetedms.PeptideSummaryWebPart;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the peptide/molecule summary heatmap visualization in QC dashboards.
 */
@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSPeptideSummaryHeatmapTest extends TargetedMSTest
{
    private static final String PEPTIDE_MOLECULE_SUMMARY = "Peptide/Molecule Summary";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSPeptideSummaryHeatmapTest init = getCurrentTest();
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
        ConfigureMetricsUIPage configureQCMetrics = qcPlotsWebPart.clickConfigureQCMetrics();
        configureQCMetrics.setFixedDeviationFromMean(QCPlotsWebPart.MetricType.PRECURSOR_AREA, "-5", "5")
                .clickSave();

        clickPortalTab(PEPTIDE_MOLECULE_SUMMARY);
        verifyDataAllDates();

        log("Verify data range: " + PeptideSummaryWebPart.HeatmapDateRange.Last_7_Days);
        PeptideSummaryWebPart peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        peptideSummaryHeatMap.setDateRange(PeptideSummaryWebPart.HeatmapDateRange.Last_7_Days);
        verifyDataLast7Days();

        // Go back to the dashboard and make sure the date range matches
        goToProjectHome();
        qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        qcPlotsWebPart.waitForReady();
        assertEquals("Date Range Offset not set to default value", QCPlotsWebPart.DateRangeOffset.LAST_7_DAYS, qcPlotsWebPart.getCurrentDateRangeOffset());
        qcPlotsWebPart.filterQCPlots("2013-08-10", "2013-08-15", true);

        // Now navigate through the link instead of the custom tab and webpart
        waitAndClickAndWait(Locator.linkContainingText("View all 47 replicates"));
        // Make sure the custom date range matches the other plot's
        peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        assertEquals("2013-08-10", peptideSummaryHeatMap.getStartDate());
        assertEquals("2013-08-15", peptideSummaryHeatMap.getEndDate());

        log("Verify invalid date combos produce helpful errors");
        peptideSummaryHeatMap.setCustomDateRange("2013-08-15", "2013-08-01");
        peptideSummaryHeatMap.applyExpectingError("Please choose a start date that is before the end date.");
        peptideSummaryHeatMap.setCustomDateRange(null, "2013-08-01");
        peptideSummaryHeatMap.applyExpectingError("Please select both start and end dates.");

        log("Verify Custom date range");
        peptideSummaryHeatMap.setCustomDateRange("2013-08-01", "2013-08-15");
        peptideSummaryHeatMap.apply();
        verifyDataCustomRange(peptideSummaryHeatMap);

        log("Verify Custom -> standard -> custom toggling");
        peptideSummaryHeatMap.setDateRange(PeptideSummaryWebPart.HeatmapDateRange.Last_7_Days);
        verifyDataLast7Days();

        peptideSummaryHeatMap.setDateRange(PeptideSummaryWebPart.HeatmapDateRange.Custom_Range);
        assertEquals("2013-08-01", peptideSummaryHeatMap.getStartDate());
        assertEquals("2013-08-15", peptideSummaryHeatMap.getEndDate());
        verifyDataCustomRange(peptideSummaryHeatMap);
    }

    private static void verifyDataCustomRange(PeptideSummaryWebPart peptideSummaryHeatMap)
    {
        Assert.assertEquals("Incorrect outlier count for " + QCPlotsWebPart.MetricType.PRECURSOR_AREA, "11",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.PRECURSOR_AREA).getText());
    }

    private void verifyDataLast7Days()
    {
        PeptideSummaryWebPart peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Incorrect outlier count for " + QCPlotsWebPart.MetricType.FWHM, "1",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());

        log("Verify heatmap colors");
        Assert.assertEquals("Incorrect heatmap color for darkest red", "rgb(255, 0, 0)",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.PRECURSOR_AREA).getCssValue("background-color"));
        Assert.assertEquals("Incorrect heatmap color for lightest red", "rgb(255, 245, 245)",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getCssValue("background-color"));
    }

    private void verifyDataAllDates()
    {
        PeptideSummaryWebPart peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Incorrect outlier count", "2",
                peptideSummaryHeatMap.getCellElement(1, QCPlotsWebPart.MetricType.FWHM).getText());
        assertTextPresent("Total Ion Chromatogram Area", "VYVEELKPTPEGDLEILLQK", "++, 1,157.1330");
        Assert.assertEquals("Incorrect total replicate count", "47", peptideSummaryHeatMap.getTotalReplicateCount().trim());
    }

    @Test
    public void testMetricUsagePeptideSummaryHeatmap()
    {
        goToProjectHome();
        clickPortalTab(PEPTIDE_MOLECULE_SUMMARY);
        PeptideSummaryWebPart peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Missing Metric type in the heatmap", Arrays.asList("", "Full Width at Base (FWB)",
                        "Full Width at Half Maximum (FWHM)", "Isotope dotp", "Precursor Area", "Precursor Mass Error",
                        "Retention Time", "TIC Area", "Total Peak Area (Precursor + Transition)",
                        "Transition Area", "Transition Mass Error", "Transition/Precursor Area Ratio", "Total")
                , peptideSummaryHeatMap.getHeatmapTable().getTableHeaderTexts());

        log("Updating the Metric type values");
        goToProjectHome();
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        ConfigureMetricsUIPage configureQCMetrics = qcPlotsWebPart.clickConfigureQCMetrics();
        configureQCMetrics.setShowMetricNoOutlier(QCPlotsWebPart.MetricType.TRANSITION_MASS_ERROR)
                .setFixedValueCutOff(QCPlotsWebPart.MetricType.RETENTION, "-15", "15")
                .disableMetric(QCPlotsWebPart.MetricType.TOTAL_PEAK.toString())
                .clickSave();

        clickPortalTab(PEPTIDE_MOLECULE_SUMMARY);
        peptideSummaryHeatMap = new PeptideSummaryWebPart(getDriver());
        Assert.assertEquals("Metric type " + QCPlotsWebPart.MetricType.TOTAL_PEAK + " should not be present",
                Arrays.asList("", "Full Width at Base (FWB)", "Full Width at Half Maximum (FWHM)", "Isotope dotp", "Precursor Area",
                        "Precursor Mass Error", "Retention Time", "TIC Area", "Transition Area",
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
