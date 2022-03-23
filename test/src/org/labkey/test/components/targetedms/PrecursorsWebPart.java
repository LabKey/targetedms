package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class PrecursorsWebPart extends BodyWebPart<PrecursorsWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "Precursors";

    public PrecursorsWebPart(WebDriver driver)
    {
        this(driver, 0);
    }

    public PrecursorsWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
        waitForLoad();
    }

    @Override
    public void clearCache()
    {
        super.clearCache();
    }

    public void waitForLoad()
    {
        WebDriverWrapper.waitFor(() -> getTableRowCount() > 4, 10000);
    }

    public PrecursorsWebPart setSortBy(PrecursorsWebPart.SortBy option)
    {
        doAndWaitForElementToRefresh(() -> getWrapper().selectOptionByText(elementCache().sortBy, option.toString()),
                getVisibleTable().append("//tbody/tr[1]/td[1]"), //Checking for staleness in the first element of the table
                1000);
        return this;
    }

    public Locator.XPathLocator getVisibleTable()
    {
        if (elementCache().table.findElement(this).getAttribute("style").contains("display: none;"))
            return elementCache().ratioTable;

        return elementCache().table;
    }

    public List<String> getSortByOptions()
    {
        return getWrapper().getSelectOptions(elementCache().sortBy);
    }

    public PrecursorsWebPart selectIntensity()
    {
        getWrapper().checkRadioButton(elementCache().intensity);
        return this;
    }

    public PrecursorsWebPart selectRatio()
    {
        getWrapper().checkRadioButton(elementCache().ratio);
        return this;
    }

    public boolean isSelected(String label)
    {
        if (label.equals("Intensity"))
            return getWrapper().isChecked(elementCache().intensity);
        else
            return getWrapper().isChecked(elementCache().ratio);
    }

    public PrecursorsWebPart selectValueType(String value)
    {
        getWrapper()._ext4Helper.selectComboBoxItem(elementCache().valueType, value);
        return this;
    }

    public int getTableRowCount()
    {
        return elementCache().table.append("//tbody/tr").findElements(this).size();
    }

    public int getRatioTableRowCount()
    {
        return elementCache().ratioTable.append("//tbody/tr").findElements(this).size();
    }

    public String getTableElement(int row, int column)
    {
        return elementCache().table.append("//tbody/tr[" + row + "]/td[" + column + "]").findElement(this).getText();
    }

    public String getRatioTableElement(int row, int column)
    {
        return elementCache().ratioTable.append("//tbody/tr[" + row + "]/td[" + column + "]").findElement(this).getText();
    }

    public List<String> getTableHeaders()
    {
        List<String> columnNames = new ArrayList<>();
        List<WebElement> we = elementCache().table.append("//thead/tr/th").findElements(this);
        for (WebElement e : we)
            columnNames.add(e.getText());
        return columnNames;
    }

    public List<String> getRatioTableHeaders()
    {
        List<String> columnNames = new ArrayList<>();
        List<WebElement> we = elementCache().ratioTable.append("//thead/tr/th").findElements(this);
        for (WebElement e : we)
            columnNames.add(e.getText());
        return columnNames;
    }

    @Override
    protected PrecursorsWebPart.Elements newElementCache()
    {
        return new PrecursorsWebPart.Elements();
    }

    public enum SortBy
    {
        Intensity("Intensity"),
        Sequence("Sequence"),
        Sequence_Location("Sequence Location"),
        Coefficient_of_Variation("Coefficient of Variation"),
        Light_heavy_ratio("Light/heavy ratio");

        private String _text;

        SortBy(String text)
        {
            _text = text;
        }

        public static PrecursorsWebPart.SortBy getEnum(String value)
        {
            for (PrecursorsWebPart.SortBy v : values())
                if (v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException(value);
        }

        public String toString()
        {
            return _text;
        }
    }

    public class Elements extends BodyWebPart.ElementCache
    {
        Locator.XPathLocator sortBy = Locator.id("peptideSort");
        Locator.XPathLocator valueType = Locator.id("valueType");
        Locator.XPathLocator intensity = Locator.name("intensityRatioToggle").withAttribute("value", "intensity");
        Locator.XPathLocator ratio = Locator.name("intensityRatioToggle").withAttribute("value", "ratio");

        Locator.XPathLocator table = Locator.id("cvTable");
        Locator.XPathLocator ratioTable = Locator.id("ratioCvTable");
    }
}
