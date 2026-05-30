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
        return qcPlotsWebPart.clickConfigureQCMetrics();
    }

    protected void verifyMetricNotPresent(QCPlotsWebPart qcPlotsWebPart, String metricName)
    {
        List<String> qcMetricOptions = qcPlotsWebPart.getMetric1TypeOptions();

        log("Verifying disabled metric not present in QC Plot dashboard dropdown");
        qcMetricOptions.forEach(qcMetric -> assertFalse("Disabled QC Metric found - " + metricName, qcMetric.equalsIgnoreCase(metricName)));
    }

    protected boolean verifyMetricIsPresent(QCPlotsWebPart qcPlotsWebPart, String metricName)
    {
        List<String> qcMetricOptions = qcPlotsWebPart.getMetric1TypeOptions();
        for (String type : qcMetricOptions)
        {
            if (type.equalsIgnoreCase(metricName))
                return true;
        }

        return false;
    }
}
