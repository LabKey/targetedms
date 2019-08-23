package org.labkey.test.tests.panoramapremium;

import org.labkey.test.Locator;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.tests.targetedms.TargetedMSTest;

public class TargetedMSPremiumTest extends TargetedMSTest
{
    public ConfigureMetricsUIPage goToConfigureMetricsUI()
    {
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        waitForElement(Locator.tagWithText("button", "Add New Metric"));
        return new ConfigureMetricsUIPage(this);
    }
}
