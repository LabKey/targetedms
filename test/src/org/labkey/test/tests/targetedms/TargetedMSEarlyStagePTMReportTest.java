/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the PTM (post-translational modification) peptide report feature, including data pre-pivoting for
 * early-stage PTM analysis.
 */
@Category({})
public class TargetedMSEarlyStagePTMReportTest extends TargetedMSTest
{
    public final static String IMPORT_FILE = "ModifiedPeptidesWithCDRAnnotation.sky.zip";
    private static final String POPULATE_CACHE_METHOD = "TargetedMS: populatePTMPercentsGroupedPrepivotCache";
    private static final String TRUNCATE_CACHE_METHOD = "TargetedMS: truncatePTMPercentsGroupedPrepivotCache";
    private static final String POPULATE_CACHE_JOB_DESCRIPTION = "Populate PTM percents cache for existing ExperimentMAM runs";

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
        // Changing an existing filter's type intermittently drops the typed value
        table.clearFilter("PeptideModifiedSequence");
        table.setFilter("PeptideModifiedSequence", "Contains", "VTL");
        table = new DataRegionTable("query", this);
        assertEquals(List.of("false", "false"), table.getColumnDataAsText("IsCdr"));
        assertEquals(List.of(" ", " "), table.getColumnDataAsText("Risk"));
    }

    @Test
    public void testEarlyStagePTMReport()
    {
        verifyEarlyStagePTMReport();
    }

    @Test
    public void testCachePopulationUpgradeCode() throws Exception
    {
        long runId = getRunId();
        int cachedRows = getCachedRowCount(runId);
        assertTrue("Expected cache rows from import for run " + runId, cachedRows > 0);

        log("Populating the cache should skip a run that's already cached");
        invokeCachePopulationUpgradeCode()
                .assertLogTextContains("run " + runId + " in /" + getProjectName() + ", 0 rows in");
        assertEquals("Cache rows after populating an already-cached run", cachedRows, getCachedRowCount(runId));

        log("Truncating and repopulating the cache");
        invokeUpgradeCode(TRUNCATE_CACHE_METHOD);
        assertEquals("Cache rows after truncating", 0, getCachedRowCount(runId));
        invokeCachePopulationUpgradeCode()
                .assertLogTextContains("run " + runId + " in /" + getProjectName() + ", " + cachedRows + " rows in");
        assertEquals("Cache rows after repopulating", cachedRows, getCachedRowCount(runId));

        goToProjectHome();
        goToSchemaBrowser();
        verifyPrepivotData(viewQueryData("targetedms", "PTMPercentsGroupedPrepivotCache"));
        verifyEarlyStagePTMReport();
    }

    /** Chrome may report opaque colors as rgba(r, g, b, 1) */
    private void assertColor(String message, String expectedRgb, WebElement cell)
    {
        String actual = cell.getCssValue("background-color");
        assertEquals(message, expectedRgb, actual.replaceFirst("^rgba\\((.*), 1\\)$", "rgb($1)"));
    }

    private void invokeUpgradeCode(String method)
    {
        beginAt(WebTestHelper.buildURL("admin-sql", "/", "upgradeCode"));
        selectOptionByValue(Locator.name("combined"), method);
        clickButton("Invoke");
    }

    /** The job is queued in the root container, so find it there and wait for it to complete */
    private PipelineStatusDetailsPage invokeCachePopulationUpgradeCode() throws IOException, CommandException
    {
        int previousJobId = getLatestCachePopulationJobId();
        invokeUpgradeCode(POPULATE_CACHE_METHOD);
        int jobId = getLatestCachePopulationJobId();
        assertTrue("Cache population job wasn't queued", jobId > previousJobId);
        beginAt(WebTestHelper.buildURL("pipeline-status", "/", "details", Map.of("rowId", jobId)));
        return new PipelineStatusDetailsPage(this).waitForComplete();
    }

    private int getLatestCachePopulationJobId() throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("pipeline", "Job");
        cmd.setColumns(List.of("RowId"));
        cmd.addFilter(new Filter("Description", POPULATE_CACHE_JOB_DESCRIPTION));
        cmd.setSorts(List.of(new Sort("RowId", Sort.Direction.DESCENDING)));
        cmd.setMaxRows(1);
        List<Map<String, Object>> rows = cmd.execute(createDefaultConnection(), "/").getRows();
        return rows.isEmpty() ? -1 : ((Number) rows.getFirst().get("RowId")).intValue();
    }

    private long getRunId() throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "Runs");
        cmd.setColumns(List.of("Id"));
        List<Map<String, Object>> rows = cmd.execute(createDefaultConnection(), getProjectName()).getRows();
        assertEquals("Imported runs", 1, rows.size());
        return ((Number) rows.getFirst().get("Id")).longValue();
    }

    private int getCachedRowCount(long runId) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "PTMPercentsGroupedPrepivotCache");
        cmd.setColumns(List.of("RunId"));
        cmd.addFilter(new Filter("RunId", runId));
        return cmd.execute(createDefaultConnection(), getProjectName()).getRowCount().intValue();
    }

    private void verifyEarlyStagePTMReport()
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
        assertColor("Incorrect risk category color for QVT/Sample1 - Green", "rgb(246, 246, 246)",
                Locator.xpath("//table/tbody/tr[" + (qvtRowIndex + 1) + "]/td[6]").findElement(reportTable));
        assertColor("Incorrect risk category color for VTN/Sample1 - Yellow", "rgb(254, 255, 63)",
                Locator.xpath("//table/tbody/tr[" + (vtnRowIndex + 1) + "]/td[6]").findElement(reportTable));
        assertColor("Incorrect risk category color for VTN/QE_2 - Red", "rgb(250, 8, 26)",
                Locator.xpath("//table/tbody/tr[" + (vtnRowIndex + 1) + "]/td[8]").findElement(reportTable));
        assertColor("Incorrect risk category color for EEQ/Sample1 - Green", "rgb(246, 246, 246)",
                Locator.xpath("//table/tbody/tr[" + (eeqRowIndex + 1) + "]/td[6]").findElement(reportTable));
        assertColor("Incorrect risk category color for WQQ/Sample1 - Green", "rgb(137, 202, 83)",
                Locator.xpath("//table/tbody/tr[" + (wqqRowIndex + 1) + "]/td[6]").findElement(reportTable));
    }
}
