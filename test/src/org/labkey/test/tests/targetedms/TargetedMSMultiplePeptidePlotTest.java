package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class TargetedMSMultiplePeptidePlotTest extends TargetedMSTest
{
    @BeforeClass
    public static void setupProject()
    {
        TargetedMSMultiplePeptidePlotTest init = (TargetedMSMultiplePeptidePlotTest) getCurrentTest();
        init.setupFolder(FolderType.Library);
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS Multiple Peptide Plot Test";
    }

    @Test
    public void testMultiplePeptidePlotOnSameChromatogram()
    {
        goToProjectHome();
        importData(SProCoP_FILE);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(SProCoP_FILE));

        log("Clicking one of the proteins");
        clickAndWait(Locator.linkWithText("gi|14278146|pdb|1B8E|"));

        log("Verifying the number of plots is same as number of replicates");
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).withName("GroupChromatograms").waitFor();
        table.clickHeaderMenu("Row Size", "1 per row");
        table.getPagingWidget().clickShowAll();
        checker().verifyEquals("Number of plots is not same as number of replicates", 47, table.getDataRowCount());

        log("Verifying the paging controls");
        table.clickHeaderMenu("Row Size", "4 per row");
        checker().verifyEquals("Incorrect rows of replicate displayed for 4 per row", 12, table.getDataRowCount());

        table.clickHeaderMenu("Row Size", "10 per row");
        checker().verifyEquals("Incorrect rows of replicate displayed for 10 per row", 5, table.getDataRowCount());

        log("Verifying Display chart settings");
        String height = "800";
        String width = "500";
        String replicateName = "Q_Exactive_08_09_2013_JGB_32";

        click(Locator.tagWithText("strong", "Display Chart Settings"));
        setFormElement(shortWait().until(ExpectedConditions.visibilityOfElementLocated(Locator.name("chartWidth"))), width);
        setFormElement(shortWait().until(ExpectedConditions.visibilityOfElementLocated(Locator.name("chartHeight"))), height);
        new ComboBox.ComboBoxFinder(getDriver()).withInputNamed("replicatesFilter")
                .findWhenNeeded(getDriver()).setMultiSelect(true).selectComboBoxItem(replicateName);

        clickButton("Update");
        waitForTextToDisappear("Loading...");

        checker().verifyEquals("Only one graph should have been displayed", 1, table.getDataRowCount());
        checker().verifyNotNull("Couldn't find plot with width " + width,
                Locator.tagWithAttribute("rect", "width", width).findElement(getDriver()));
        checker().verifyNotNull("Couldn't find plot with height " + height,
                Locator.tagWithAttribute("rect", "height", height).findElement(getDriver()));
        checker().verifyTrue("Incorrect replicate", isElementPresent(Locator.tagContainingText("div", replicateName)));
        checker().screenShotIfNewError("singleReplicate");

        log("Verifying exporting of PNG and PDF plots");
        mouseOver(Locator.tagWithAttributeContaining("div", "alt", "Chromatogram"));
        File pdfFile = doAndWaitForDownload(() -> click(Locator.tagWithClass("i", "fa fa-file-pdf-o")));
        File pngFile = doAndWaitForDownload(() -> click(Locator.tagWithClass("i", "fa fa-file-image-o")));
        checker().verifyTrue("Error in exported PDF file", pdfFile.length() != 0);
        checker().verifyTrue("Error in exported PNG file", pngFile.length() != 0);

        log("Verifying the chromatogram plots for peptides");
        clickAndWait(Locator.linkWithText("VYVEELKPTPEGDLEILLQK"));
        table = new DataRegionTable("PeptidePrecursorChromatograms", getDriver());
        // There are 47 replicates. In the default view we expect a row for each of the first 10 replicates
        checker().verifyEquals("Expected one row each for the first 10 replicates", 10, table.getDataRowCount());
        // Each row should have two chromatogram charts from a replicate - the total precursor chromatogram and the fragment ion chromatogram for the precursor
        int expectedGraphCount = 20;
        waitForElementToDisappear(Locator.tagWithAttributeContaining("div", "alt", "Chromatogram Q_Exactive")
                .withText("Loading..."), WebDriverWrapper.WAIT_FOR_PAGE);
        List<WebElement> svgs = Locator.tag("svg").findElements(table);
        checker().withScreenshot("SVGCount").verifyEquals("Incorrect SVG graphs", expectedGraphCount, svgs.size());
        // Change the row size to 2 replicates per row
        table.clickHeaderMenu("Row Size", "2 replicates per row");
        // Each row will have chromatograms from two replicates. We expect 5 rows since we are displaying the first 10 replicates.
        checker().verifyEquals("Incorrect row count after changing row size to 2 replicates per row", 5, table.getDataRowCount());
    }
}
