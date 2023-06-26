package org.labkey.test.tests.panoramapremium;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;

import static org.junit.Assert.assertTrue;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSIsotopologueTest extends TargetedMSPremiumTest
{
    protected static final String ISOTOPOLOGUE_FILE_ANNOTATED = "PRM_7x5mix_A40010_QEHF_examples_v3.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSIsotopologueTest init = (TargetedMSIsotopologueTest) getCurrentTest();
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
        qcPlotsWebPart.clickMenuItem("Configure QC Metrics");
        waitForText("Isotopologue Accuracy", "Isotopologue LOD", "Isotopologue LOQ", "Isotopologue Regression RSquared");

        log("Verifying that two new metric properties are added");
        clickButton("Add New Custom Metric", 0);
        Window metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Metric").waitFor();
        assertElementPresent(Locator.name("enabledSchema"));
        assertElementPresent(Locator.name("enabledQuery"));

    }
}
