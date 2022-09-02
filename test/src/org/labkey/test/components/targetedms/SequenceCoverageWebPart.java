package org.labkey.test.components.targetedms;


import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

public class SequenceCoverageWebPart extends BodyWebPart<SequenceCoverageWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "Sequence Coverage";

    public SequenceCoverageWebPart(WebDriver driver)
    {
        this(driver, 0);
    }

    public SequenceCoverageWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
        waitForLoad();
    }

    public void waitForLoad()
    {
        WebDriverWrapper.waitFor(() -> getMap() != null, 10000);
    }

    public String getDisplayBy()
    {
        return elementCache().displayBy.getFirstSelectedOption().getText();
    }

    public SequenceCoverageWebPart setDisplayBy(String value)
    {
        elementCache().displayBy.selectByValue(value);
        waitForDisplayToRefresh();
        return this;
    }

    public String getReplicate()
    {
        return elementCache().replicate.getText();
    }

    public SequenceCoverageWebPart setReplicate(String value)
    {
        getWrapper().selectOptionByText(elementCache().replicate, value);
        waitForDisplayToRefresh();
        return this;
    }

    public SequenceCoverageWebPart waitForDisplayToRefresh()
    {
        //TODO : Add logic to wait for load
        return this;
    }

    public SequenceCoverageWebPart setModifiedForm(String value)
    {
        if (value.equals("combined"))
            getWrapper().checkRadioButton(elementCache().combined);
        else
            getWrapper().checkRadioButton(elementCache().stacked);

        waitForDisplayToRefresh();
        return this;
    }

    public WebElement getMap()
    {
        return elementCache().peptideMap;
    }

    public List<String> getHeatMapLegendValues()
    {
        List<String> ret = new ArrayList<>();
        List<WebElement> text = elementCache().heatMapLegend.withChild(Locator.tag("text")).findElements(this);
        for( WebElement e : text)
        {
            if(e.getText() != null)
                ret.add(e.getText());
        }
        return ret;
    }
    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends BodyWebPart.ElementCache
    {
//        WebElement displayBy = Locator.name("peptideSettings").findElement(this);
        Select displayBy = new Select(Locator.name("peptideSettings").findWhenNeeded(this));
        WebElement replicate = Locator.name("replicateSettings").findWhenNeeded(this);
        Locator.XPathLocator combined = Locator.name("combinedOrStacked").withAttribute("value", "combined");
        Locator.XPathLocator stacked = Locator.name("combinedOrStacked").withAttribute("value", "stacked");
        WebElement peptideMap = Locator.id("peptideMap").findWhenNeeded(this);
        Locator.XPathLocator heatMapLegend = Locator.tagWithClass("div","heatmap");
    }
}
