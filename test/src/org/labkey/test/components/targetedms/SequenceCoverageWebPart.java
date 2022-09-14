package org.labkey.test.components.targetedms;


import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
        elementCache().displayBy.selectByVisibleText(value);
        return this;
    }

    public String getReplicate()
    {
        return elementCache().replicate.getFirstSelectedOption().getText();
    }

    public SequenceCoverageWebPart setReplicate(String value)
    {
        elementCache().replicate.selectByVisibleText(value);
        return this;
    }

    public SequenceCoverageWebPart setModifiedForm(String value)
    {
        getWrapper().checkRadioButton(elementCache().modifiedForms.withAttribute("value", value));
        return this;
    }

    public WebElement getMap()
    {
        return elementCache().peptideMap;
    }

    public List<String> getHeatMapLegendValues()
    {
        List<String> ret = new ArrayList<>();
        List<WebElement> text = Locator.tag("text").findElements(elementCache().heatMapLegend);
        for (WebElement e : text)
        {
            String textValue = e.getText();
            if (!textValue.isBlank())
                ret.add(textValue);
        }
        return ret;
    }

    public String getPopUpDetails(String value)
    {
        if (elementCache().peptideDetailsHelp.isDisplayed())
        {
            Locator.tagWithAttribute("img", "alt", "close").findElement(elementCache().peptideDetailsHelp).click();
            getWrapper().shortWait().until(ExpectedConditions.invisibilityOf(elementCache().peptideDetailsHelp));
        }

        getWrapper().mouseOver(Locator.tagWithAttributeContaining("a", "id", "helpPopup").withText(value));
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().peptideDetailsHelp));

        return Locator.id("helpDivBody").findElement(elementCache().peptideDetailsHelp).getText();
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends BodyWebPart.ElementCache
    {
        Select displayBy = new Select(Locator.name("peptideSettings").findWhenNeeded(this));
        Select replicate = new Select(Locator.name("replicateSettings").findWhenNeeded(this));
        Locator.XPathLocator modifiedForms = Locator.name("combinedOrStacked");
        WebElement peptideMap = Locator.id("peptideMap").findWhenNeeded(this);
        WebElement heatMapLegend = Locator.tagWithClass("div", "heatmap").child(Locator.tag("svg")).findWhenNeeded(this);
        WebElement peptideDetailsHelp = Locator.id("helpDiv").findWhenNeeded(getDriver());
    }
}
