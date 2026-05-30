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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;

import static org.junit.Assert.assertTrue;

/**
 * Validates isotopologue metric display and calculation in the Panorama Dashboard QC metrics panel.
 */
@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSIsotopologueTest extends TargetedMSPremiumTest
{
    @BeforeClass
    public static void initProject()
    {
        TargetedMSIsotopologueTest init = getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        importData(ISOTOPOLOGUE_FILE_ANNOTATED);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testIsotopologueMetric()
    {
        log("Starting the test with Panorama Dashboard");
        goToProjectHome();
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();

        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true);

        log("Verifying if all the metrics are present");
        assertTrue("Accuracy metric is not present", verifyMetricIsPresent(qcPlotsWebPart, "Isotopologue Accuracy"));
        assertTrue("LOD metric is not present", verifyMetricIsPresent(qcPlotsWebPart, "Isotopologue LOD"));
        assertTrue("LOQ metric is not present", verifyMetricIsPresent(qcPlotsWebPart, "Isotopologue LOQ"));
        assertTrue("Regression metric is not present", verifyMetricIsPresent(qcPlotsWebPart, "Isotopologue Regression RSquared"));

        // Issue 45015 - make sure abbreviations are correct. Unicode escape is for ellipsis character
        waitForText("ELA\u2026GFK");
        assertTextPresent("ELA\u2026GFk", "ELA\u2026PV\u2026", "ELA\u2026Pv\u2026", "ELA\u2026p\u2026");

        log("Verifying isotopologue is present while configuring the metric");
        qcPlotsWebPart.clickConfigureQCMetrics();
        waitForText("Isotopologue Accuracy", "Isotopologue LOD", "Isotopologue LOQ", "Isotopologue Regression RSquared");
    }
}
