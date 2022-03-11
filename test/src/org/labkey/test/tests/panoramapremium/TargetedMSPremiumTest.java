package org.labkey.test.tests.panoramapremium;

import org.labkey.test.Locator;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.tests.targetedms.TargetedMSTest;

import java.util.List;

import static org.junit.Assert.assertFalse;

public class TargetedMSPremiumTest extends TargetedMSTest
{
    public ConfigureMetricsUIPage goToConfigureMetricsUI()
    {
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        waitForElement(Locator.tagWithText("button", "Add New Custom Metric"));
        return new ConfigureMetricsUIPage(this);
    }

    protected void verifyMetricNotPresent(QCPlotsWebPart qcPlotsWebPart, String metricName)
    {
        List<String> qcMetricOptions = qcPlotsWebPart.getMetricTypeOptions();

        log("Verifying disabled metric not present in QC Plot dashboard dropdown");
        qcMetricOptions.forEach(qcMetric -> assertFalse("Disabled QC Metric found - " + metricName, qcMetric.equalsIgnoreCase(metricName)));
    }

    protected boolean verifyMetricIsPresent(QCPlotsWebPart qcPlotsWebPart, String metricName)
    {
        List<String> qcMetricOptions = qcPlotsWebPart.getMetricTypeOptions();
        for (String type : qcMetricOptions)
        {
            if (type.equalsIgnoreCase(metricName))
                return true;
        }

        return false;
    }
}
