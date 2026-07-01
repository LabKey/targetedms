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

        // The two folders hold identical imports, so the cross-folder total should be exactly double
        // the count in a single folder.
        int singleFolderFileCount = getFileCount(getProjectName());
        int expectedCrossFolderFileCount = singleFolderFileCount * 2;
        assertTrue("Expected the single folder to contain sample files with acquisition times", singleFolderFileCount > 0);

        beginAt(WebTestHelper.buildURL("targetedms", getProjectName(), "showInstrument", Map.of("name", _instrumentName)));

        verifyCalendar();
        verifyRunsAcquiredGrids(expectedCrossFolderFileCount);
    }

    private void verifyCalendar()
    {
        log("Verifying the cross-folder Utilization Calendar renders");
        assertElementPresent(Locator.tagWithClass("span", "labkey-wp-title-text").withText("Utilization Calendar"));

        // Wait for the async selectRows call to populate the calendar with day cells
        waitForElement(Locator.tagWithClassContaining("div", "day-content"));
        assertEquals("Calendar should default to a single month",
                "1 month", getSelectedOptionText(Locator.id("utilizationMonthNumberSelect")));
    }

    private void verifyRunsAcquiredGrids(int expectedCrossFolderFileCount)
    {
        InstrumentUtilizationWebPart utilization = new InstrumentUtilizationWebPart(getDriver());

        log("Verifying the By Day grid is shown by default and aggregates across folders");
        assertTrue("By Day grid should be visible by default", utilization.isByDayVisible());
        assertFalse("By Month grid should be hidden by default", utilization.isByMonthVisible());

        DataRegionTable byDay = utilization.getByDayTable();
        assertTrue("By Day grid is missing expected columns",
                byDay.getColumnLabels().containsAll(List.of("Date", "Runs", "Files")));
        assertEquals("By Day grid should sum files across both folders",
                expectedCrossFolderFileCount, utilization.getTotalFiles(byDay));

        log("Toggling to the By Month summary");
        utilization.showByMonth();
        assertTrue("By Month grid should be visible after toggling", utilization.isByMonthVisible());
        assertFalse("By Day grid should be hidden after toggling", utilization.isByDayVisible());

        DataRegionTable byMonth = utilization.getByMonthTable();
        assertTrue("By Month grid is missing expected columns",
                byMonth.getColumnLabels().containsAll(List.of("Month", "Runs", "Files")));
        // Grouping by month rather than day changes the row count but not the overall file total
        assertEquals("By Month grid should sum to the same cross-folder file total",
                expectedCrossFolderFileCount, utilization.getTotalFiles(byMonth));

        log("Toggling back to the By Day grid");
        utilization.showByDay();
        assertTrue("By Day grid should be visible after toggling back", utilization.isByDayVisible());
        assertFalse("By Month grid should be hidden after toggling back", utilization.isByMonthVisible());
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

    /** @return the number of sample files (with an acquisition time) for the instrument in a single container */
    private int getFileCount(String containerPath) throws IOException, CommandException
    {
        SelectRowsCommand command = new SelectRowsCommand("targetedms", "SampleFile");
        command.setColumns(List.of("Id"));
        command.setFilters(List.of(
                new Filter("InstrumentNickname", _instrumentName),
                new Filter("AcquiredTime", null, Filter.Operator.NONBLANK)));
        SelectRowsResponse response = command.execute(createDefaultConnection(), containerPath);
        return response.getRowCount().intValue();
    }
}
