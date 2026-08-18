/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.tests.panoramapremium;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.components.targetedms.TargetedMSRunsTable;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.openqa.selenium.NoSuchElementException;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.components.targetedms.QCPlotsWebPart.QCPlotType.CUSUMm;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

/**
 * Tests premium-tier QC functionality: guide sets, advanced QC metrics, CUSUM plots, and premium-specific permission
 * checks.
 */
@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSQCPremiumTest extends TargetedMSPremiumTest
{
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCPremiumTest init = getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).setUserPermissions(USER, READER_ROLE);
        importData(SProCoP_FILE);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // these tests use the UIContainerHelper for project creation, but we can use the APIContainerHelper for deletion
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(getProjectName(), afterTest);
        apiContainerHelper.deleteProject("PressureTraceQC", false);
        _userHelper.deleteUsers(false, USER);
    }

    @Test
    public void testConfigureQCMetrics()
    {
        QCPlotsWebPart.MetricType metric = QCPlotsWebPart.MetricType.TOTAL_PEAK;
        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.disableMetric(metric);
        configureUI.clickSave();

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        verifyMetricNotPresent(qcPlotsWebPart, metric.toString());

        //re-enabling peak area metric
        goToConfigureMetricsUI();
        configureUI.setLeveyJennings(metric.toString(), null, null);
        clickAndWait(Locator.buttonContainingText("Save"));
        impersonate(USER);
        log("Verifying Configure QC Metrics Menu option not present for non admin");
        try
        {
            goToConfigureMetricsUI();
            fail("Shouldn't have found the QC metrics config menu item as non-admin");
        }
        catch (NoSuchElementException ignored) {}
    }

    @Test
    public void testNotification()
    {
        goToProjectHome();
        PanoramaDashboard qcDashboard = goToDashboard();
        QCSummaryWebPart qcSummary = qcDashboard.getQcSummaryWebPart();

        log("Clicking the outlier notification link");
        qcSummary.clickMenuItem("Subscribe to Outlier Notification Emails");
        waitForElement(Locator.radioButtonByName("subscriptionType").index(1));
        checkRadioButton(Locator.radioButtonByName("subscriptionType").index(1));
        // the new value outlier count in the recently imported sample is 0
        setFormElement(Locator.name("outlierCount"), "0");
        clickAndWait(Locator.tagWithAttribute("input", "value", "Save"));

        log("Importing the file to trigger the notification");
        importData(SProCoP_FILE_ANNOTATED, 2);

        log("Verifying the notification");
        goToModule("Dumbster");
        EmailRecordTable notifications = new EmailRecordTable(this);
        assertEquals("Mismatch in the expected number of notification", 1, notifications.getEmailCount());
        EmailRecordTable.EmailMessage message = notifications.getMessageWithSubjectContaining("Panorama QC Notification");
        notifications.clickMessage(message);
        assertTextPresent("0 outliers"); //Total outliers

        //Added additional verification as part of isotoplogue story
        qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        verifyMetricNotPresent(qcPlotsWebPart,"Isotopologue Accuracy");
        verifyMetricNotPresent(qcPlotsWebPart,"Isotopologue LOD");
        verifyMetricNotPresent(qcPlotsWebPart,"Isotopologue LOQ");
        verifyMetricNotPresent(qcPlotsWebPart,"Isotopologue Regression RSquared");

    }

    @Test
    public void testAddNewMetric()
    {
        String metricName = "Test Custom Metric";
        String series1Query = "AQCTest_Metric"; //starting the query name with A to make it appear top in the list

        log("Adding new test custom metric");
        //need to preserve the insertion order
        Map<ConfigureMetricsUIPage.CustomMetricProperties, String > metricProperties = new LinkedHashMap<>();
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricName, metricName);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.queryName, series1Query);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.yAxisLabel, metricName);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricType, ConfigureMetricsUIPage.MetricType.Precursor.name());

        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.addNewCustomMetric(metricProperties, false);
        configureUI = goToConfigureMetricsUI();
        configureUI.addNewCustomMetric(metricProperties, true);

        log("Verifying new metric got added");
        goToConfigureMetricsUI();
        waitForElement(Locator.linkWithText(metricName));
        assertTextPresent(metricName);

        log("Disabling added test metric");
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        goToConfigureMetricsUI();
        configureUI.disableMetric(metricName);
        configureUI.clickSave();

        verifyMetricNotPresent(qcPlotsWebPart, metricName);

        configureUI = goToConfigureMetricsUI();
        metricProperties.clear();
        String metricName2 = metricName + "-Edited";
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricName, metricName2);
        configureUI.editMetric(metricName, metricProperties);

        log("Verifying new metric got edited");
        waitForElement(Locator.linkWithText(metricName2));
        configureUI.setLeveyJennings(metricName2, "-3", "3");
        configureUI.clickSave();
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        List<String> metricOptions = getMetric1TypeOptions(qcPlotsWebPart);
        assertTrue("Didn't find '" + metricName2 + "' in :" + metricOptions, metricOptions.contains(metricName2));
        goToConfigureMetricsUI().deleteMetric(metricName2);
        goToDashboard();
        metricOptions = getMetric1TypeOptions(qcPlotsWebPart);
        assertFalse("Found '" + metricName2 + "' in :" + metricOptions, metricOptions.contains(metricName2));
    }

    private static List<String> getMetric1TypeOptions(QCPlotsWebPart qcPlotsWebPart)
    {
        return qcPlotsWebPart.getMetric1TypeOptions();
    }

    @Test
    public void testTraceMetric() throws IOException, CommandException
    {
        String projectName = "PressureTraceQC";
        String traceName = "ColumnOven_FC_BridgeFlow (channel 5)";

        final String firstMetric = "First Pressure After 5";
        final String minMetric = "Min between 5 and 7";
        final String maxMetric = "Max between 5 and 7";

        setUpFolder(projectName, FolderType.QC);
        importData(SAMPLE_FILE_CHROM_INFO);

        addNewTimeTraceMetrics(firstMetric, "First", traceName, false);
        addNewTimeTraceMetrics(firstMetric, "First", traceName, true);
        addNewTimeTraceMetrics(minMetric, "Min", "ColumnPressure (channel 4)", false);
        addNewTimeTraceMetrics(maxMetric, "Max", traceName, false);

        log("Verify trace values after metric addition");
        assertTrue("Trace values are not present", getTraceMetricValueRowCount() > 0);

        goToProjectHome(projectName);
        log("Verify qc plots");
        verifyQCPlot(firstMetric, "7.363");
        verifyQCPlot(minMetric, "72.878");
        verifyQCPlot(maxMetric, "17.508");

        // Make sure the second dropdown shows similarly scoped metrics to the first drop-down
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        assertEquals(Arrays.asList("", firstMetric, minMetric), qcPlotsWebPart.getMetric2TypeOptions());
        qcPlotsWebPart.setMetric1Type(QCPlotsWebPart.MetricType.RETENTION);
        List<String> metric2Options = qcPlotsWebPart.getMetric2TypeOptions();
        assertFalse("Shouldn't have run-scoped metrics in the second dropdown: " + metric2Options, metric2Options.contains(firstMetric));
        assertTrue("Should have precursor-scoped metrics in the second dropdown: " + metric2Options, metric2Options.contains(QCPlotsWebPart.MetricType.TRANSITION_AREA.toString()));
        assertFalse("Shouldn't have the same metric in the second dropdown: " + metric2Options, metric2Options.contains(QCPlotsWebPart.MetricType.RETENTION.toString()));

        // use the pressure trace: its values stay well above the trace-value threshold below,
        // whereas ColumnOven_FC_BridgeFlow sits around 7 and would never reach it
        verifyTraceMetricModeSwitch(projectName, "ColumnPressure (channel 4)");

        log("Delete run and verify trace metric values are deleted");
        clickTab("Runs");
        TargetedMSRunsTable runsTable = new TargetedMSRunsTable(this);
        runsTable.deleteRun(SAMPLE_FILE_CHROM_INFO);
        assertEquals("Values in QCTraceMetricValues are not deleted on deleting run", 0, getTraceMetricValueRowCount());

        log("Reimport run and verify QCTraceMetricValues has values after import");
        importData(SAMPLE_FILE_CHROM_INFO, 2);
        assertTrue("Trace values are not present", getTraceMetricValueRowCount() > 0);
    }

    private int getTraceMetricValueRowCount() throws IOException, CommandException
    {
        return new SelectRowsCommand("targetedms", "QCTraceMetricValues").
                execute(createDefaultConnection(), getCurrentContainerPath()).
                getRows().
                size();
    }

    private void verifyQCPlot(String metricName, String tooltipValue)
    {
        log("Verify qc plots");
        Assertions.assertThat(getQCPlotHoverText(metricName)).as("Tooltip value").contains(tooltipValue);
    }

    private String getQCPlotHoverText(String metricName)
    {
        refresh();
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        _ext4Helper.selectComboBoxItem(Locator.id("metric-type-field1"), metricName);
        qcPlotsWebPart.waitForPlots(1);
        String pressurePlotSVGText = qcPlotsWebPart.getSVGPlotText("precursorPlot0");
        assertFalse("Pressure trace plot is not present", pressurePlotSVGText.isEmpty());
        assertTrue("Y axis label is not correct or present", pressurePlotSVGText.contains("psi"));
        qcPlotsWebPart.openExclusionBubble("2009-11-03 19:37:28");
        return waitForElementToBeVisible(qcPlotsWebPart.getBubbleContent()).getText();
    }

    /**
     * A trace metric holds two mutually exclusive configurations - a time-value one and a trace-value one - and the
     * server reads TimeValueOption first. If switching modes doesn't clear the columns for the mode being left behind,
     * the row keeps both and the metric goes on computing the old one while the UI shows the new one.
     */
    private void verifyTraceMetricModeSwitch(String projectName, String traceName) throws IOException, CommandException
    {
        final String metricName = "Mode Switch";
        // the pressure trace stays above this for the whole 5-7 minute window (the Min there is
        // ~72.878), so the trace is guaranteed to reach it and the metric always has a value
        final String traceValue = "50";

        log("Add " + metricName + " in time-value mode");
        addNewTimeTraceMetrics(metricName, "Min", traceName, false);
        assertTraceMetricConfig(projectName, metricName, "Min", 5.0, 7.0, null);

        goToProjectHome(projectName);
        String timeModeHoverText = getQCPlotHoverText(metricName);

        log("Switch " + metricName + " to trace-value mode");
        Map<ConfigureMetricsUIPage.TraceMetricProperties, String> traceMode = new LinkedHashMap<>();
        traceMode.put(ConfigureMetricsUIPage.TraceMetricProperties.metricName, metricName);
        traceMode.put(ConfigureMetricsUIPage.TraceMetricProperties.traceValue, traceValue);
        goToConfigureMetricsUI().editTraceMetric(metricName, traceMode);

        assertTraceMetricConfig(projectName, metricName, null, null, null, 50.0);
        assertEquals("Mode the edit dialog reopened in", "traceValue",
                goToConfigureMetricsUI().getTraceMetricMode(metricName));

        goToProjectHome(projectName);
        Assertions.assertThat(getQCPlotHoverText(metricName))
                .as("Plotted value after switching to trace-value mode")
                .isNotEqualTo(timeModeHoverText);

        log("Switch " + metricName + " back to time-value mode");
        Map<ConfigureMetricsUIPage.TraceMetricProperties, String> timeMode = new LinkedHashMap<>();
        timeMode.put(ConfigureMetricsUIPage.TraceMetricProperties.metricName, metricName);
        timeMode.put(ConfigureMetricsUIPage.TraceMetricProperties.timeValueOption, "Min");
        timeMode.put(ConfigureMetricsUIPage.TraceMetricProperties.minTimeValue, "5");
        timeMode.put(ConfigureMetricsUIPage.TraceMetricProperties.maxTimeValue, "7");
        goToConfigureMetricsUI().editTraceMetric(metricName, timeMode);

        assertTraceMetricConfig(projectName, metricName, "Min", 5.0, 7.0, null);
        assertEquals("Mode the edit dialog reopened in", "timeValue",
                goToConfigureMetricsUI().getTraceMetricMode(metricName));
    }

    private void assertTraceMetricConfig(String projectName, String metricName, String timeValueOption,
                                         Double minTimeValue, Double maxTimeValue, Double traceValue)
            throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "qcmetricconfiguration");
        cmd.setColumns(Arrays.asList("Name", "TimeValueOption", "MinTimeValue", "MaxTimeValue", "TraceValue"));
        cmd.addFilter(new Filter("Name", metricName));
        List<Map<String, Object>> rows = cmd.execute(createDefaultConnection(), "/" + projectName).getRows();

        assertEquals("Number of '" + metricName + "' metrics", 1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals("TimeValueOption", timeValueOption, row.get("TimeValueOption"));
        assertEquals("MinTimeValue", minTimeValue, asDouble(row.get("MinTimeValue")));
        assertEquals("MaxTimeValue", maxTimeValue, asDouble(row.get("MaxTimeValue")));
        assertEquals("TraceValue", traceValue, asDouble(row.get("TraceValue")));
    }

    private Double asDouble(Object value)
    {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private void addNewTimeTraceMetrics(String metricName, String timeValueOption, String traceName, boolean duplicateNameErrorExpected)
    {
        String yAxisLabel = "psi";
        String minTimeValue = "5";
        String maxTimeValue = "7";

        log("Add new test trace metric " + metricName);
        Map<ConfigureMetricsUIPage.TraceMetricProperties, String> metricProperties = new LinkedHashMap<>();
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.metricName, metricName);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.traceName, traceName);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.yAxisLabel, yAxisLabel);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.timeValueOption, timeValueOption);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.minTimeValue, minTimeValue);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.maxTimeValue, maxTimeValue);

        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.addNewTraceMetric(metricProperties, duplicateNameErrorExpected);

        log("Verify new trace metrics got added");
        goToConfigureMetricsUI();
        waitForElement(Locator.linkWithText(metricName));
        assertTextPresent(metricName);
    }

    @Test
    public void testDefaultViewSettingQCPlots()
    {
        goToProjectHome();

        PanoramaDashboard panoramaDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = panoramaDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setMetric1Type(QCPlotsWebPart.MetricType.TOTAL_PEAK);
        qcPlotsWebPart.checkPlotType(CUSUMm);
        qcPlotsWebPart.setShowExcludedPoints(true);
        qcPlotsWebPart.saveAsDefaultView();

        log("Verifying the values are set after save as default view action");
        checker().verifyTrue("Incorrect value for Show Excluded points", qcPlotsWebPart.isShowExcludedPointsChecked());
        checker().verifyEquals("Incorrect Metric value", QCPlotsWebPart.MetricType.TOTAL_PEAK.toString(),
                qcPlotsWebPart.getCurrentMetric1Type().toString());

        impersonate(USER);
        checker().verifyTrue("Incorrect value for Show Excluded points for different user " + USER , qcPlotsWebPart.isShowExcludedPointsChecked());
        checker().verifyEquals("Incorrect Metric value for different user " + USER, QCPlotsWebPart.MetricType.TOTAL_PEAK.toString(),
                qcPlotsWebPart.getCurrentMetric1Type().toString());
        checker().verifyEquals("Reader user should not have save as default permission", Arrays.asList("Revert to Default View"),
                getListOfMenuItems(qcPlotsWebPart));

        stopImpersonating();

        log("Verifying revert to default view action");
        goToProjectHome();
        panoramaDashboard = new PanoramaDashboard(this);
        qcPlotsWebPart = panoramaDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setMetric1Type(QCPlotsWebPart.MetricType.RETENTION);
        qcPlotsWebPart.revertToDefaultView();

        checker().verifyEquals("Incorrect Metric value", QCPlotsWebPart.MetricType.TOTAL_PEAK.toString(),
                qcPlotsWebPart.getCurrentMetric1Type().toString());
    }

    private List<String> getListOfMenuItems(QCPlotsWebPart qcPlotsWebPart)
    {
        qcPlotsWebPart.getTitleMenu().expand();
        return getTexts(qcPlotsWebPart.getTitleMenu().findVisibleMenuItems());
    }
}
