package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSMxNReproducibilityReportTest extends TargetedMSTest
{
    private static final String SKY_FILE = "UW_Cpep_Linearity_Imprecision_trimmed.sky.zip";
    private static final String SKY_FILE_WITH_SINGLE_REPLICATE = "ListTest.sky.zip";


    @BeforeClass
    public static void setupProject()
    {
        TargetedMSMxNReproducibilityReportTest init = (TargetedMSMxNReproducibilityReportTest) getCurrentTest();
        init.setupFolder(FolderType.Library);

    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS MxN Reproducibility Report Test";
    }

    @Test
    public void testReproducibilityReport()
    {
        goToProjectHome();
        importData(SKY_FILE);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(SKY_FILE));

        log("Clicking on the protein");
        clickAndWait(Locator.linkWithText("PEPTIDE_GROUP_0"));

        waitForElement(Locator.tagWithName("a", "Protein"));

        checker().verifyTrue("Reproducibility Report is not present", isElementPresent(Locator.linkWithText("Reproducibility Report")));
        clickAndWait(Locator.linkWithText("Reproducibility Report"));

        checker().verifyEquals("Incorrect number of graphs for EAEDLQVGQVE", getPrecursorChromeInfo("EAEDLQVGQVE"),
                getGraphCount("EAEDLQVGQVE"));

        checker().verifyEquals("Incorrect number of graphs for EAEDL[+7.0]QVGQVE", getPrecursorChromeInfo("EAEDL[+7.0]QVGQVE"),
                getGraphCount("EAEDL[+7.0]QVGQVE"));
    }

    @Test
    public void testSingleReplicateFile()
    {
        String subFolderName = "Single Replicate Test Case";
        goToProjectHome();
        setupSubfolder(getProjectName(), subFolderName, FolderType.Library);

        navigateToFolder(getProjectName(), subFolderName);
        importData(SKY_FILE_WITH_SINGLE_REPLICATE);

        navigateToFolder(getProjectName(), subFolderName);
        clickAndWait(Locator.linkWithText(SKY_FILE_WITH_SINGLE_REPLICATE));

        log("Clicking on the protein");
        clickAndWait(Locator.linkWithText("gi|171455|gb|AAA88712.1|"));
        waitForElement(Locator.tagWithName("a", "Protein"));

        checker().verifyFalse("Reproducibility Report should not be present", isElementPresent(Locator.linkWithText("Reproducibility Report")));
    }

    private int getGraphCount(String sequence)
    {
        pushLocation();
        Locator.linkWithText(sequence).findElement(getDriver()).click();
        // graph count
        int count = Locator.tagWithAttributeContaining("span", "id", "chrom").findElements(getDriver()).size();
        popLocation();
        return count;
    }

    private int getPrecursorChromeInfo(String sequence)
    {
        pushLocation();
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", "PrecursorChromInfo");
        table.setFilter("PrecursorId", "Equals", sequence);
        int count = table.getDataRowCount();
        popLocation();
        return count;
    }
}
