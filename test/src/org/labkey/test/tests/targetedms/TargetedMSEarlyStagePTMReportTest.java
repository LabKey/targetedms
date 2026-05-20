package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the PTM (post-translational modification) peptide report feature, including data pre-pivoting for
 * early-stage PTM analysis.
 */
@Category({})
public class TargetedMSEarlyStagePTMReportTest extends TargetedMSTest
{
    public final static String IMPORT_FILE = "ModifiedPeptidesWithCDRAnnotation.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSEarlyStagePTMReportTest init = getCurrentTest();
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
    public void testEarlyStagePrepivot()
    {
        // Test against the live-query version
        goToProjectHome();
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", "PTMPercentsGroupedPrepivot");
        verifyPrepivotData(table);

        // Test against the cached version too
        goToSchemaBrowser();
        table = viewQueryData("targetedms", "PTMPercentsGroupedPrepivotCache");
        verifyPrepivotData(table);
    }

    private void verifyPrepivotData(DataRegionTable table)
    {
        // Test special-cased peptide
        table.setFilter("PeptideModifiedSequence", "Starts With", "EEQ");
        table = new DataRegionTable("query", this);
        assertEquals(List.of("false", "false", "false", "false", "false", "false"), table.getColumnDataAsText("IsCdr"));
        assertEquals(List.of(" ", " ", " ", " ", " ", " "), table.getColumnDataAsText("Risk"));

        // Test "normal" peptide
        table.setFilter("PeptideModifiedSequence", "Starts With", "WQQ");
        table = new DataRegionTable("query", this);
        assertEquals(List.of("false", "false"), table.getColumnDataAsText("IsCdr"));
        assertEquals(List.of("Low", "Medium"), table.getColumnDataAsText("Risk"));

        // Test CDR peptide
        table.setFilter("PeptideModifiedSequence", "Starts With", "VTN");
        table = new DataRegionTable("query", this);
        assertEquals(List.of("true", "true"), table.getColumnDataAsText("IsCdr"));
        assertEquals(List.of("Medium", "High"), table.getColumnDataAsText("Risk"));

        // Test special-cased N-Term Modification, present on QVTL peptide (Q is modified, so don't use it in the filter)
        table.setFilter("PeptideModifiedSequence", "Contains", "VTL");
        table = new DataRegionTable("query", this);
        assertEquals(List.of("false", "false"), table.getColumnDataAsText("IsCdr"));
        assertEquals(List.of(" ", " "), table.getColumnDataAsText("Risk"));
    }

    @Test
    public void testEarlyStagePTMReport()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(IMPORT_FILE));
        waitAndClickAndWait(Locator.linkWithText("Early Stage PTM Report"));
        DataRegionTable reportTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("earlyStagePtmReport").waitFor();

        log("Verifying the table headers");
        assertEquals("Incorrect column headers", Arrays.asList("Chain", "Site Location", "Sequence", "Modification", "Max Percent Modified",
                "Percent Modified", "Total Percent Modified", "Percent Modified", "Total Percent Modified"), reportTable.getColumnLabels());
        assertEquals("Incorrect Sample Names displayed as headers", "Sample1 QE_2",
                Locator.xpath("//table/thead[2]/tr").findElement(reportTable).getText());

        int qvtRowIndex = 0;  // Special-cased modification: Gln->pyro-Glu (N-term Q)
        int vtnRowIndex = 1;
        int eeqRowIndex = 3;  // Special-cased peptide: EEQYNSTYR(V)
        int wqqRowIndex = 7;

        log("Verifying the modified percentage for sequence with CDR Range and stressed or not stressed updates");
        assertEquals("Incorrect percentages for (K)VTNMDPADTATYYCAR(D) sequence", Arrays.asList("(K)VTNMDPADTATYYCAR(D)", "11.3%", "11.3%", "11.1%", "11.1%"),
                reportTable.getRowDataAsText(vtnRowIndex, "Sequence", "QE_1::PercentModified", "QE_1::TotalPercentModified",
                        "QE_2::PercentModified", "QE_2::TotalPercentModified"));
        assertEquals("Incorrect percentages for (R)WQQGNVFSCSVMHEALHNHYTQK(S) sequence", Arrays.asList("(R)WQQGNVFSCSVMHEALHNHYTQK(S)", "22.1%", "22.1%", "24.1%", "24.1%"),
                reportTable.getRowDataAsText(wqqRowIndex, "Sequence", "QE_1::PercentModified", "QE_1::TotalPercentModified",
                        "QE_2::PercentModified", "QE_2::TotalPercentModified"));

        log("Verifying the cell colors: Gray, Green, Yellow and Red");
        assertEquals("Incorrect risk category color for QVT/Sample1 - Green", "rgb(246, 246, 246)",
                Locator.xpath("//table/tbody/tr[" + (qvtRowIndex + 1) + "]/td[6]").findElement(reportTable).getCssValue("background-color"));
        assertEquals("Incorrect risk category color for VTN/Sample1 - Yellow", "rgb(254, 255, 63)",
                Locator.xpath("//table/tbody/tr[" + (vtnRowIndex + 1) + "]/td[6]").findElement(reportTable).getCssValue("background-color"));
        assertEquals("Incorrect risk category color for VTN/QE_2 - Red", "rgb(250, 8, 26)",
                Locator.xpath("//table/tbody/tr[" + (vtnRowIndex + 1) + "]/td[8]").findElement(reportTable).getCssValue("background-color"));
        assertEquals("Incorrect risk category color for EEQ/Sample1 - Green", "rgb(246, 246, 246)",
                Locator.xpath("//table/tbody/tr[" + (eeqRowIndex + 1) + "]/td[6]").findElement(reportTable).getCssValue("background-color"));
        assertEquals("Incorrect risk category color for WQQ/Sample1 - Green", "rgb(137, 202, 83)",
                Locator.xpath("//table/tbody/tr[" + (wqqRowIndex + 1) + "]/td[6]").findElement(reportTable).getCssValue("background-color"));
    }
}
