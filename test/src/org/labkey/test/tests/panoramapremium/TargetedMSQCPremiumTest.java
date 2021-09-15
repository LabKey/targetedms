/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.tests.panoramapremium;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Git;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.components.targetedms.TargetedMSRunsTable;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(Git.class)
public class TargetedMSQCPremiumTest extends TargetedMSPremiumTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCPremiumTest init = (TargetedMSQCPremiumTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _containerHelper.enableModules(Arrays.asList("Dumbster", "PanoramaPremium"));
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).setUserPermissions(USER, "Reader");
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
        apiContainerHelper.deleteProject("PressureTraceQC", afterTest);
    }

    @Test
    public void testConfigureQCMetrics()
    {
        String metric = "Peak Area";
        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.disableMetric(metric);

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        verifyMetricNotPresent(qcPlotsWebPart, "Peak Area");

        //re-enabling peak area metric
        goToConfigureMetricsUI();
        configureUI.enableMetric(metric);

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

        log("Clicking the outliew notification link");
        qcSummary.clickMenuItem("Subscribe Outlier Notifications");
        checkRadioButton(Locator.radioButtonByName("subscriptionType").index(1));
        setFormElement(Locator.name("outlierCount"), "1");
        clickAndWait(Locator.tagWithAttribute("input", "value", "Save"));

        log("Importing the file to trigger the notification");
        importData(SProCoP_FILE_ANNOTATED, 2);

        log("Verifying the notification");
        goToModule("Dumbster");
        EmailRecordTable notifications = new EmailRecordTable(this);
        assertEquals("Mismatch in the expected number of notification", 1, notifications.getEmailCount());
        EmailRecordTable.EmailMessage message = notifications.getMessageWithSubjectContaining("Panorama QC Notification");
        notifications.clickMessage(message);
        assertTextPresent("56 outliers"); //Total outliers

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
        String schema1Name = "targetedms";
        String series1Query = "AQCTest_Metric"; //starting the query name with A to make it appear top in the list

        log("Adding new test custom metric");
        //need to preserve the insertion order
        Map<ConfigureMetricsUIPage.CustomMetricProperties, String > metricProperties = new LinkedHashMap<>();
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricName, metricName);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.series1Schema, schema1Name);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.series1Query, series1Query);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.series1AxisLabel, metricName);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricType, ConfigureMetricsUIPage.MetricType.Precursor.name());

        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.addNewCustomMetric(metricProperties);

        log("Verifying new metric got added");
        goToConfigureMetricsUI();
        waitForElement(Locator.linkWithText(metricName));
        assertTextPresent(metricName);

        log("Disabling added test metric");
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        goToConfigureMetricsUI();
        configureUI.disableMetric(metricName);

        verifyMetricNotPresent(qcPlotsWebPart, metricName);

        configureUI = goToConfigureMetricsUI();
        metricProperties.clear();
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricName, metricName+"-Edited");
        configureUI.editMetric(metricName, metricProperties);

        log("Verifying new metric got edited");
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        waitForElement(Locator.linkWithText(metricName + "-Edited"));
        assertTextPresent(metricName + "-Edited");
    }

    @Test
    public void testTraceMetric()
    {
        String projectName = "PressureTraceQC";
        String metricName = "Pressure At 5";
        String traceName = "ColumnOven_FC_BridgeFlow (channel 5)";
        String yAxisLabel = "psi";
        String timeValue = "5";
        String skyFile = "SampleFileChromInfo.sky.zip";

        setUpFolder(projectName, FolderType.QC);
        importData(skyFile);

        log("Add new test trace metric");
        Map<ConfigureMetricsUIPage.TraceMetricProperties, String> metricProperties = new LinkedHashMap<>();
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.metricName, metricName);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.traceName, traceName);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.yAxisLabel, yAxisLabel);
        metricProperties.put(ConfigureMetricsUIPage.TraceMetricProperties.timeValue, timeValue);

        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.addNewTraceMetric(metricProperties);

        log("Verify new trace metric got added");
        goToConfigureMetricsUI();
        waitForElement(Locator.linkWithText(metricName));
        assertTextPresent(metricName);

        log("Verify trace values after metric addition");
        goToSchemaBrowser();
        DataRegionTable traceValuesTable = viewQueryData("targetedms", "QCTraceMetricValues");
        assertTrue("Trace values are not present", traceValuesTable.getDataRowCount() > 0);

        log("Verify qc plots");
        var firstTraceValue = traceValuesTable.getRowDataAsMap("sampleFileId", "Site95_STUDY9S_PHASEI_6ProtMix_QC_01");
        goToProjectHome(projectName);
        PanoramaDashboard dashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = dashboard.getQcPlotsWebPart();
        _ext4Helper.selectComboBoxItem(Locator.id("metric-type-field"), metricName);
        qcPlotsWebPart.waitForPlots(1, true);
        String pressurePlotSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
        assertFalse("Pressure trace plot is not present", pressurePlotSVGText.isEmpty());
        assertTrue("Y axis label is not correct or present", pressurePlotSVGText.contains(yAxisLabel));
        mouseOver(qcPlotsWebPart.getPointByAcquiredDate("2009-11-03 19:37:28"));
        waitForElement(qcPlotsWebPart.getBubble());
        String pressureTracehoverText = waitForElement(qcPlotsWebPart.getBubbleContent()).getText();
        assertTrue("Wrong value present", pressureTracehoverText.contains(firstTraceValue.get("value")));

        log("Delete run and verify trace metric values are deleted");
        clickTab("Runs");
        TargetedMSRunsTable runsTable = new TargetedMSRunsTable(this);
        runsTable.deleteRun(skyFile);
        goToSchemaBrowser();
        traceValuesTable = viewQueryData("targetedms", "QCTraceMetricValues");
        assertEquals("Values in QCTraceMetricValues are not deleted on deleting run", 0, traceValuesTable.getDataRowCount());

        log("Reimport run and verify QCTraceMetricValues has values after import");
        importData(skyFile, 2);
        goToSchemaBrowser();
        traceValuesTable = viewQueryData("targetedms", "QCTraceMetricValues");
        assertTrue("Trace values after import are not present", traceValuesTable.getDataRowCount() > 0);
    }

}
