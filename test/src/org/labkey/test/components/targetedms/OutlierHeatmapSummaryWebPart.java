package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.components.html.Table;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class OutlierHeatmapSummaryWebPart extends BodyWebPart<OutlierHeatmapSummaryWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "Outlier Heatmap Summary";

    public OutlierHeatmapSummaryWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
        waitForTable();
    }

    public void waitForTable()
    {
        WebDriverWrapper.waitFor(() -> elementCache().heatmapLoc.isDisplayed(this),
                "Outlier Heatmap did not load on time",
                getWrapper().longWaitForPage);
    }

    public OutlierHeatmapSummaryWebPart setDateRange(HeatmapDateRange value, boolean waitForReferesh)
    {
        if (waitForReferesh)
        {
            doAndWaitForElementToRefresh(() -> elementCache().dateRange.selectByVisibleText(value.getLabel()),
                    elementCache().heatmapLoc, getWrapper().defaultWaitForPage);
        }
        else
        {
            elementCache().dateRange.selectByVisibleText(value.getLabel());
        }

        return this;
    }

    public OutlierHeatmapSummaryWebPart setStartDate(String value)
    {
        elementCache().startDate.set(value);
        return this;
    }

    public OutlierHeatmapSummaryWebPart setEndDate(String value)
    {
        doAndWaitForElementToRefresh(() -> elementCache().endDate.set(value),
                elementCache().heatmapLoc, getWrapper().defaultWaitForPage);

        return this;
    }

    public WebElement getCellElement(int rowIndex, QCPlotsWebPart.MetricType metricType)
    {
        return elementCache().heatmapTable.getDataAsElement(rowIndex, getMetricIndex(metricType));
    }

    public int getMetricIndex(QCPlotsWebPart.MetricType metricType)
    {
       return elementCache().heatmapTable.getTableHeaderIndex(metricType.toString());
    }

    public String getTotalReplicateCount()
    {
       return elementCache().replicateCount.getText();
    }

    public Table getHeatmapTable()
    {
        return elementCache().heatmapTable;
    }
    @Override
    protected OutlierHeatmapSummaryWebPart.Elements newElementCache()
    {
        return new OutlierHeatmapSummaryWebPart.Elements();
    }

    public enum HeatmapDateRange
    {
        All_Dates("All dates"),
        Last_7_Days("Last 7 days"),
        Last_15_Days("Last 15 days"),
        Last_30_Days("Last 30 days"),
        Last_90_Days("Last 90 days"),
        Last_180_Days("Last 180 days"),
        Last_365_Days("Last 365 days"),
        Custom_Range("Custom range");

        private final String _label;

        HeatmapDateRange(String label)
        {
            _label = label;
        }

        public String getLabel()
        {
            return _label;
        }
    }

    public class Elements extends BodyWebPart<?>.ElementCache
    {
        final Select dateRange = SelectWrapper.Select(Locator.id("date-range")).refindWhenNeeded(this);
        final Input startDate = new Input(Locator.id("start-date").refindWhenNeeded(this), getDriver());
        final Input endDate = new Input(Locator.id("end-date").refindWhenNeeded(this), getDriver());
        final Locator heatmapLoc = Locator.id("heatmap-table");
        final Table heatmapTable = new Table(getDriver(), heatmapLoc.refindWhenNeeded(this));
        final WebElement replicateCount = Locator.id("total-replicates").refindWhenNeeded(this);
    }
}
