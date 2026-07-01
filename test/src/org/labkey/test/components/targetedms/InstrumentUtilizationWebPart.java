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
 * The "Runs Acquired" web part on the Show Instrument page. It shows an instrument's utilization,
 * aggregated across all readable folders, as a grid of runs/files by day, with a client-side toggle
 * to switch to a by-month summary.
 */
public class InstrumentUtilizationWebPart extends BodyWebPart<InstrumentUtilizationWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "Runs Acquired";
    public static final String BY_DAY_REGION = "UtilizationByDay";
    public static final String BY_MONTH_REGION = "UtilizationByMonth";
    public static final String FILES_COLUMN = "Files";

    public InstrumentUtilizationWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
        WebDriverWrapper.waitFor(() -> elementCache().byDayToggle.isDisplayed(),
                "Runs Acquired web part did not load", getWrapper().defaultWaitForPage);
    }

    public boolean isByDayVisible()
    {
        return elementCache().byDayGrid.isDisplayed();
    }

    public boolean isByMonthVisible()
    {
        return elementCache().byMonthGrid.isDisplayed();
    }

    public InstrumentUtilizationWebPart showByDay()
    {
        if (!isByDayVisible())
            elementCache().byDayToggle.click();
        WebDriverWrapper.waitFor(this::isByDayVisible, "By Day grid did not become visible", 5000);
        return this;
    }

    public InstrumentUtilizationWebPart showByMonth()
    {
        if (!isByMonthVisible())
            elementCache().byMonthToggle.click();
        WebDriverWrapper.waitFor(this::isByMonthVisible, "By Month grid did not become visible", 5000);
        return this;
    }

    public DataRegionTable getByDayTable()
    {
        return new DataRegionTable(BY_DAY_REGION, getDriver());
    }

    public DataRegionTable getByMonthTable()
    {
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
        final WebElement byDayToggle = Locator.id("utilizationToggleDay").findWhenNeeded(this);
        final WebElement byMonthToggle = Locator.id("utilizationToggleMonth").findWhenNeeded(this);
        final WebElement byDayGrid = Locator.id("utilizationByDayGrid").findWhenNeeded(this);
        final WebElement byMonthGrid = Locator.id("utilizationByMonthGrid").findWhenNeeded(this);
    }
}
