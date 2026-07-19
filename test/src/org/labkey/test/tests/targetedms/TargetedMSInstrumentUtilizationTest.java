/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.targetedms.InstrumentUtilizationWebPart;
import org.labkey.test.util.DataRegionTable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the instrument-scoped, cross-folder utilization views on the Show Instrument page: the
 * "Utilization Calendar" web part and the "Runs Acquired" by-day/by-month grids. The same Skyline
 * document is imported into two folders so we can assert that the views aggregate across every folder
 * the user can read, not just the current one.
 */
@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSInstrumentUtilizationTest extends TargetedMSTest
{
    private static final String QC_SUBFOLDER = "QC Subfolder";

    private String _instrumentName;

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSInstrumentUtilizationTest init = getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        setupSubfolder(getProjectName(), QC_SUBFOLDER, FolderType.QC);

        // Import the same document into two folders so the same instrument has data in both
        goToProjectHome();
        importData(SProCoP_FILE);

        clickFolder(QC_SUBFOLDER);
        importData(SProCoP_FILE);
    }

    @Test
    public void testCrossFolderInstrumentUtilization() throws IOException, CommandException
    {
        _instrumentName = getInstrumentNickname();

        // The grids use an AllFolders filter, so use that same scope as the ground truth (a shared server may hold this instrument's data elsewhere)
        int singleFolderFileCount = getFileCount(getProjectName(), null);
        assertTrue("Expected the single folder to contain sample files with acquisition times", singleFolderFileCount > 0);
        int expectedCrossFolderFileCount = getFileCount(getProjectName(), ContainerFilter.AllFolders);
        assertTrue("Cross-folder total should aggregate beyond a single folder",
                expectedCrossFolderFileCount > singleFolderFileCount);

        beginAt(WebTestHelper.buildURL("targetedms", getProjectName(), "showInstrument", Map.of("name", _instrumentName)));

        InstrumentUtilizationWebPart utilization = new InstrumentUtilizationWebPart(getDriver());
        verifyCalendarTab(utilization);
        verifyRunsGrids(utilization, expectedCrossFolderFileCount);
        verifyDrillIntoSamples(utilization);
    }

    /**
     * The Skyline Document Count / Replicate Count cells in the summary grids drill into the Samples tab
     * with a matching date filter applied. Verify the by-day and by-month links land on the Samples tab
     * with the expected AcquiredTime filter and a populated grid.
     */
    private void verifyDrillIntoSamples(InstrumentUtilizationWebPart utilization)
    {
        log("Drilling into the Samples tab from a Summary by Day count link");
        DataRegionTable byDay = utilization.getByDayTable();
        int dayReplicates = Integer.parseInt(byDay.getDataAsText(0, "Replicate Count").trim());
        utilization = utilization.drillIntoSamples(byDay, 0);
        assertTrue("Samples tab should open when a day's count is clicked", utilization.isSamplesVisible());
        assertTrue("A day drill-in should apply a single-day AcquiredTime filter",
                getDriver().getCurrentUrl().contains("dateeq"));
        // A single day fits on one grid page, so this can be exact
        waitForSamplesRowCount(dayReplicates);

        log("Drilling into the Samples tab from a Summary by Month count link");
        DataRegionTable byMonth = utilization.getByMonthTable();
        utilization = utilization.drillIntoSamples(byMonth, 0);
        assertTrue("Samples tab should open when a month's count is clicked", utilization.isSamplesVisible());
        // A month can exceed one grid page, so verify the filter is applied and the grid is populated (not an exact count)
        String monthUrl = getDriver().getCurrentUrl();
        assertTrue("A month drill-in should apply a month-range AcquiredTime filter",
                monthUrl.contains("dategte") && monthUrl.contains("datelt"));
        assertTrue("Month drill-in should show the instrument's replicates for that month",
                new DataRegionTable(InstrumentUtilizationWebPart.SAMPLE_FILE_REGION, getDriver()).getDataRowCount() > 0);
    }

    /** Waits for the sample-file grid to refresh to the expected filtered row count (rebuilt each poll to dodge staleness). */
    private void waitForSamplesRowCount(int expected)
    {
        waitFor(() -> new DataRegionTable(InstrumentUtilizationWebPart.SAMPLE_FILE_REGION, getDriver()).getDataRowCount() == expected,
                "Samples grid did not filter to the expected " + expected + " row(s)", 10_000);
    }

    private void verifyCalendarTab(InstrumentUtilizationWebPart utilization)
    {
        log("Verifying the utilization web part opens on the Utilization Calendar tab");
        assertTrue("The Utilization Calendar tab should be active by default", utilization.isCalendarVisible());

        // Wait for the async selectRows call to populate the calendar with day cells
        waitForElement(Locator.tagWithClassContaining("div", "day-content"));
        assertEquals("Calendar should default to a four-month view",
                "4 months", getSelectedOptionText(Locator.id("utilizationMonthNumberSelect")));
    }

    private void verifyRunsGrids(InstrumentUtilizationWebPart utilization, int expectedCrossFolderFileCount)
    {
        log("Selecting the Runs by Day tab and verifying it aggregates across folders");
        utilization.showByDay();
        assertTrue("By Day grid should be visible after selecting its tab", utilization.isByDayVisible());
        assertFalse("Calendar should be hidden when the By Day tab is active", utilization.isCalendarVisible());

        DataRegionTable byDay = utilization.getByDayTable();
        assertTrue("By Day grid is missing expected columns",
                byDay.getColumnLabels().containsAll(List.of("Date", "Skyline Document Count", "Replicate Count")));
        assertEquals("By Day grid should sum files across both folders",
                expectedCrossFolderFileCount, utilization.getTotalFiles(byDay));

        log("Selecting the Runs by Month tab");
        utilization.showByMonth();
        assertTrue("By Month grid should be visible after selecting its tab", utilization.isByMonthVisible());
        assertFalse("By Day grid should be hidden when the By Month tab is active", utilization.isByDayVisible());

        DataRegionTable byMonth = utilization.getByMonthTable();
        assertTrue("By Month grid is missing expected columns",
                byMonth.getColumnLabels().containsAll(List.of("Month", "Skyline Document Count", "Replicate Count")));
        // Grouping by month rather than day changes the row count but not the overall file total
        assertEquals("By Month grid should sum to the same cross-folder file total",
                expectedCrossFolderFileCount, utilization.getTotalFiles(byMonth));

        log("Selecting the Samples tab and verifying the full sample-file listing aggregates across folders");
        utilization.showSamples();
        assertTrue("Samples grid should be visible after selecting its tab", utilization.isSamplesVisible());
        assertFalse("By Month grid should be hidden when the Samples tab is active", utilization.isByMonthVisible());

        // The exact total is asserted above via the summary grids; the raw listing can exceed a page, so just bound it
        DataRegionTable samples = utilization.getSamplesTable();
        int sampleRows = samples.getDataRowCount();
        assertTrue("Samples grid should list the instrument's sample files (" + sampleRows + ")",
                sampleRows > 0 && sampleRows <= expectedCrossFolderFileCount);

        log("Selecting the Calendar tab again");
        utilization.showCalendar();
        assertTrue("Calendar should be visible after selecting its tab", utilization.isCalendarVisible());
        assertFalse("Samples grid should be hidden when the calendar tab is active", utilization.isSamplesVisible());
    }

    /** @return the default nickname (model - serial number) for the instrument that acquired the imported data */
    private String getInstrumentNickname() throws IOException, CommandException
    {
        SelectRowsCommand command = new SelectRowsCommand("targetedms", "SampleFile");
        command.setColumns(List.of("InstrumentNickname"));
        command.setMaxRows(1);
        SelectRowsResponse response = command.execute(createDefaultConnection(), getProjectName());
        assertFalse("No sample files were imported", response.getRows().isEmpty());
        return (String) response.getRows().get(0).get("InstrumentNickname");
    }

    /**
     * @param containerFilter scope for the count, or null for just the given container
     * @return the number of sample files (with an acquisition time) for the instrument in the requested scope
     */
    private int getFileCount(String containerPath, @Nullable ContainerFilter containerFilter) throws IOException, CommandException
    {
        SelectRowsCommand command = new SelectRowsCommand("targetedms", "SampleFile");
        command.setColumns(List.of("Id"));
        command.setFilters(List.of(
                new Filter("InstrumentNickname", _instrumentName),
                new Filter("AcquiredTime", null, Filter.Operator.NONBLANK)));
        if (containerFilter != null)
        {
            command.setContainerFilter(containerFilter);
        }
        SelectRowsResponse response = command.execute(createDefaultConnection(), containerPath);
        return response.getRowCount().intValue();
    }
}
