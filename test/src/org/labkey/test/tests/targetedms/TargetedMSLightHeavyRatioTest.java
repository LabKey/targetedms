package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.targetedms.PrecursorsWebPart;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.Locator.tag;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSLightHeavyRatioTest extends TargetedMSTest
{
    private static final String SKY_FILE = "150604_VDBG_Hoofnagle_Precision_1119.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSLightHeavyRatioTest init = (TargetedMSLightHeavyRatioTest) getCurrentTest();
        init.doInit();
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doInit()
    {
        setupFolder(FolderType.LibraryProtein);
        importData(SKY_FILE);
    }

    @Test
    public void testIntensity()
    {
        goToProjectHome();
        clickTab("Proteins");
        DataRegionTable table = new DataRegionTable("PeptideGroup", getDriver());
        table.setFilter("Label", "Equals", "gi|324021745|ref|NP_001191236.1|");
        clickAndWait(reproducibilityReportLink());

        log("Verifying the default checks for Intensity");
        PrecursorsWebPart precursorsWebPart = new PrecursorsWebPart(getDriver());
        assertTrue("Default selection was not intensity", precursorsWebPart.isSelected("Intensity"));
        assertEquals("Incorrect Sort by options for Intensity", Arrays.asList("Intensity", "Sequence", "Sequence Location", "Coefficient of Variation"),
                precursorsWebPart.getSortByOptions());
        assertEquals("Incorrect column headers", Arrays.asList("Sequence", "Copy", "Charge", "mZ", "Start Index", "Length", "Inter-day CV", "Intra-day CV", "Total CV", "Mean Intensity", "Max Intensity", "Min Intensity"),
                precursorsWebPart.getTableHeaders());
        assertEquals("Incorrect number of rows displayed in the table", 8, precursorsWebPart.getTableRowCount());
        assertEquals("Incorrect first row sorting in " + PrecursorsWebPart.SortBy.Intensity.toString(), "VLEPTLK[+8.0]",
                precursorsWebPart.getTableElement(1, 1));
        assertEquals("Incorrect last row after sorting in " + PrecursorsWebPart.SortBy.Intensity.toString(), "LPDATPTELAK",
                precursorsWebPart.getTableElement(8, 1));
        assertEquals("Incorrect Mean intensity for VLEPTLK", "3.925e+6", precursorsWebPart.getTableElement(2, 9));
        assertEquals("Incorrect Max intensity for VLEPTLK", "4.882e+6", precursorsWebPart.getTableElement(2, 10));
        assertEquals("Incorrect Min intensity for VLEPTLK", "3.232e+6", precursorsWebPart.getTableElement(2, 11));

        log("Verifying Sequence sort by option");
        precursorsWebPart.setSortBy(PrecursorsWebPart.SortBy.Sequence);
        assertEquals("Incorrect first row after sorting in " + PrecursorsWebPart.SortBy.Sequence.toString(), "ELPEHTVK",
                precursorsWebPart.getTableElement(1, 1));
        assertEquals("Incorrect last row after sorting in " + PrecursorsWebPart.SortBy.Sequence.toString(), "VLEPTLK[+8.0]",
                precursorsWebPart.getTableElement(8, 1));

        log("Verifying Coefficient of Variation sort by option");
        precursorsWebPart.setSortBy(PrecursorsWebPart.SortBy.Coefficient_of_Variation);
        assertEquals("Incorrect first row after sorting in " + PrecursorsWebPart.SortBy.Coefficient_of_Variation.toString(), "31.5%",
                precursorsWebPart.getTableElement(1, 8));
        assertEquals("Incorrect last row after sorting in " + PrecursorsWebPart.SortBy.Coefficient_of_Variation.toString(), "8.1%",
                precursorsWebPart.getTableElement(8, 8));
    }

    @Test
    public void testLightHeavyRatio()
    {
        goToProjectHome();
        clickTab("Proteins");
        DataRegionTable table = new DataRegionTable("PeptideGroup", getDriver());
        table.setFilter("Label", "Equals", "gi|324021745|ref|NP_001191236.1|");
        clickAndWait(reproducibilityReportLink());

        log("Verifying values for light/heavy ratio");
        PrecursorsWebPart precursorsWebPart = new PrecursorsWebPart(getDriver());
        assertFalse("Default selection was Light/heavy ratio", precursorsWebPart.isSelected("Light/Heavy Ratio"));
        precursorsWebPart.selectRatio();

        assertEquals("Incorrect Sort by options for Light/heavy ratio", Arrays.asList("Light/heavy ratio", "Sequence", "Sequence Location", "Coefficient of Variation"),
                precursorsWebPart.getSortByOptions());
        assertEquals("Incorrect column headers", Arrays.asList("Sequence", "Copy", "Light charge", "Heavy charge", "Light mZ", "Heavy mZ", "Start Index", "Length", "Inter-day CV", "Intra-day CV", "Total CV", "Mean Ratio", "Max Ratio", "Min Ratio"),
                precursorsWebPart.getRatioTableHeaders());
        assertEquals("Incorrect number of rows displayed in ratio table", 4, precursorsWebPart.getRatioTableRowCount());
        assertEquals("Incorrect Mean ratio for VLEPTLK", "0.918", precursorsWebPart.getRatioTableElement(2, 11));
        assertEquals("Incorrect Max ratio for VLEPTLK", "1.050", precursorsWebPart.getRatioTableElement(2, 12));
        assertEquals("Incorrect Min ratio for VLEPTLK", "0.838", precursorsWebPart.getRatioTableElement(2, 13));

        log("Verifying Light/Heavy ration sort by option");
        assertEquals("Incorrect first row sorting in " + PrecursorsWebPart.SortBy.Light_heavy_ratio.toString(), "ELPEHTVK",
                precursorsWebPart.getRatioTableElement(1, 1));
        assertEquals("Incorrect last row after sorting in " + PrecursorsWebPart.SortBy.Light_heavy_ratio.toString(), "LPDATPTELAK",
                precursorsWebPart.getRatioTableElement(4, 1));

        log("Verifying Sequence location sort by option");
        precursorsWebPart.setSortBy(PrecursorsWebPart.SortBy.Sequence_Location);
        assertEquals("Incorrect first row after sorting in " + PrecursorsWebPart.SortBy.Coefficient_of_Variation.toString(), "295",
                precursorsWebPart.getRatioTableElement(1, 6));
        assertEquals("Incorrect last row after sorting in " + PrecursorsWebPart.SortBy.Coefficient_of_Variation.toString(), "448",
                precursorsWebPart.getRatioTableElement(4, 6));
    }

    private Locator.XPathLocator reproducibilityReportLink()
    {
        return tag("a").withChild(tag("i").withAttributeContaining("class", "fa-th")
                .withAttribute("title", "Reproducibility Report"));
    }
}
