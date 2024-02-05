package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;

@Category({})
public class TargetedMSEarlyStagePTMReportTest extends TargetedMSTest
{
    public final static String IMPORT_FILE = "ModifiedPeptidesWithCDRAnnotation.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSEarlyStagePTMReportTest init = (TargetedMSEarlyStagePTMReportTest) getCurrentTest();
        init.doInit();
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doInit()
    {
        setupFolder(FolderType.ExperimentMAM);
        importData(IMPORT_FILE);
    }

    @Test
    public void testEarlyStagePTMReport()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(IMPORT_FILE));
        waitAndClickAndWait(Locator.linkWithText("Early Stage PTM Report"));
        DataRegionTable reportTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("earlyStagePtmReport").waitFor();

        log("Verifying the table headers");
        Assert.assertEquals("Incorrect column headers", Arrays.asList("Chain", "Site Location", "Sequence", "Modification", "Max Percent Modified",
                "Percent Modified", "Total Percent Modified", "Percent Modified", "Total Percent Modified"), reportTable.getColumnLabels());
        Assert.assertEquals("Incorrect Sample Names displayed as headers", "Sample1 Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_2",
                Locator.xpath("//table/thead[2]/tr").findElement(reportTable).getText());

        int vtnRowIndex = 8;
        int wqqRowIndex = 13;

        log("Verifying the modified percentage for sequence with CDR Range and stressed or not stressed updates");
        Assert.assertEquals("Incorrect percentages for (K)VTNMDPADTATYYCAR(D) sequence", Arrays.asList("(K)VTNMDPADTATYYCAR(D)", "11.3%", "11.3%", "11.1%", "11.1%"),
                reportTable.getRowDataAsText(vtnRowIndex, "Sequence", "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_1::PercentModified", "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_1::TotalPercentModified",
                        "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_2::PercentModified", "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_2::TotalPercentModified"));
        Assert.assertEquals("Incorrect percentages for (R)WQQGNVFSCSVMHEALHNHYTQK(S) sequence", Arrays.asList("(R)WQQGNVFSCSVMHEALHNHYTQK(S)", "22.1%", "22.1%", "24.1%", "24.1%"),
                reportTable.getRowDataAsText(wqqRowIndex, "Sequence", "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_1::PercentModified", "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_1::TotalPercentModified",
                        "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_2::PercentModified", "Sc_WCL-250ng-5ngNIST_rKCTi_3hRP-20cm_DE30_QE_2::TotalPercentModified"));

        log("Verifying the cell colors:Green, Yellow and Red");
        Assert.assertEquals("Incorrect risk category color for - Green", "rgb(137, 202, 83)",
                Locator.xpath("//table/tbody/tr[" + wqqRowIndex + "]/td[6]").findElement(reportTable).getCssValue("background-color"));
        Assert.assertEquals("Incorrect risk category color for - Yellow", "rgb(254, 255, 63)",
                Locator.xpath("//table/tbody/tr[" + vtnRowIndex + "]/td[6]").findElement(reportTable).getCssValue("background-color"));
        Assert.assertEquals("Incorrect risk category color for - Red", "rgb(250, 8, 26)",
                Locator.xpath("//table/tbody/tr[" + vtnRowIndex + "]/td[8]").findElement(reportTable).getCssValue("background-color"));
    }
}
