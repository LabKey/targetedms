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
package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The "Instrument Utilization Across Folders" web part on the Show Instrument page. It shows an
 * instrument's utilization, aggregated across all readable folders, in three tabs: a heatmap calendar,
 * a runs-by-month grid, and a runs-by-day grid. Only the active tab's pane is visible at a time.
 */
public class InstrumentUtilizationWebPart extends BodyWebPart<InstrumentUtilizationWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "Instrument Utilization Across Folders";
    public static final String BY_DAY_REGION = "UtilizationByDay";
    public static final String BY_MONTH_REGION = "UtilizationByMonth";
    public static final String FILES_COLUMN = "Files";

    public InstrumentUtilizationWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
        WebDriverWrapper.waitFor(() -> elementCache().calendarTab.isDisplayed(),
                "Instrument utilization web part did not load", getWrapper().defaultWaitForPage);
    }

    public boolean isCalendarVisible()
    {
        return elementCache().calendarPane.isDisplayed();
    }

    public boolean isByDayVisible()
    {
        return elementCache().byDayPane.isDisplayed();
    }

    public boolean isByMonthVisible()
    {
        return elementCache().byMonthPane.isDisplayed();
    }

    public InstrumentUtilizationWebPart showCalendar()
    {
        if (!isCalendarVisible())
            elementCache().calendarTab.click();
        WebDriverWrapper.waitFor(this::isCalendarVisible, "Calendar tab did not become visible", 5000);
        return this;
    }

    public InstrumentUtilizationWebPart showByDay()
    {
        if (!isByDayVisible())
            elementCache().byDayTab.click();
        WebDriverWrapper.waitFor(this::isByDayVisible, "By Day tab did not become visible", 5000);
        return this;
    }

    public InstrumentUtilizationWebPart showByMonth()
    {
        if (!isByMonthVisible())
            elementCache().byMonthTab.click();
        WebDriverWrapper.waitFor(this::isByMonthVisible, "By Month tab did not become visible", 5000);
        return this;
    }

    /** Selects the By Day tab (a hidden data region reports no cell text) and returns its grid. */
    public DataRegionTable getByDayTable()
    {
        showByDay();
        return new DataRegionTable(BY_DAY_REGION, getDriver());
    }

    /** Selects the By Month tab (a hidden data region reports no cell text) and returns its grid. */
    public DataRegionTable getByMonthTable()
    {
        showByMonth();
        return new DataRegionTable(BY_MONTH_REGION, getDriver());
    }

    /** Sum of the (integer) "Files" column, i.e. the total number of sample files represented by the grid. */
    public int getTotalFiles(DataRegionTable table)
    {
        int total = 0;
        for (String value : table.getColumnDataAsText(FILES_COLUMN))
        {
            total += Integer.parseInt(value.trim());
        }
        return total;
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends BodyWebPart<?>.ElementCache
    {
        final WebElement calendarTab = Locator.css("#utilizationTabs a[data-utilization-tab='calendar']").findWhenNeeded(this);
        final WebElement byMonthTab = Locator.css("#utilizationTabs a[data-utilization-tab='month']").findWhenNeeded(this);
        final WebElement byDayTab = Locator.css("#utilizationTabs a[data-utilization-tab='day']").findWhenNeeded(this);
        final WebElement calendarPane = Locator.id("utilizationTabCalendar").findWhenNeeded(this);
        final WebElement byMonthPane = Locator.id("utilizationTabMonth").findWhenNeeded(this);
        final WebElement byDayPane = Locator.id("utilizationTabDay").findWhenNeeded(this);
    }
}
