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
import org.labkey.test.categories.Git;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.ApiPermissionsHelper;

import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
        BootstrapMenu qcPlotMenu = qcPlotsWebPart.getWebParMenu();
        log("Verifying Configure QC Metrics Menu option not present for non admin");
        try
        {
            qcPlotMenu.findVisibleMenuItems();
            assert false;
        }
        catch (NoSuchElementException ex)
        {
            assert true : "No menu option present";
        }

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
        Map<ConfigureMetricsUIPage.MetricProperties , String > metricProperties = new LinkedHashMap<>();
        metricProperties.put(ConfigureMetricsUIPage.MetricProperties.metricName, metricName);
        metricProperties.put(ConfigureMetricsUIPage.MetricProperties.series1Schema, schema1Name);
        metricProperties.put(ConfigureMetricsUIPage.MetricProperties.series1Query, series1Query);
        metricProperties.put(ConfigureMetricsUIPage.MetricProperties.series1AxisLabel, metricName);
        metricProperties.put(ConfigureMetricsUIPage.MetricProperties.metricType, ConfigureMetricsUIPage.MetricType.Precursor.name());

        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.addNewMetric(metricProperties);

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
        metricProperties.put(ConfigureMetricsUIPage.MetricProperties.metricName, metricName+"-Edited");
        configureUI.editMetric(metricName, metricProperties);

        log("Verifying new metric got edited");
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        waitForElement(Locator.linkWithText(metricName + "-Edited"));
        assertTextPresent(metricName + "-Edited");
    }

}
