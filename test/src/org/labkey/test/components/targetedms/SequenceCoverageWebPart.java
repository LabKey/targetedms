package org.labkey.test.components.targetedms;


import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.html.SelectWrapper;
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
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().peptideMap));
    }

    public String getDisplayBy()
    {
        return elementCache().displayBy.getFirstSelectedOption().getText();
    }

    public SequenceCoverageWebPart setDisplayBy(String value)
    {
        return doAndWaitForUpdate(() -> elementCache().displayBy.selectByVisibleText(value));
    }

    public String getReplicate()
    {
        return elementCache().replicate.getFirstSelectedOption().getText();
    }

    public SequenceCoverageWebPart setReplicate(String value)
    {
        return doAndWaitForUpdate(() -> elementCache().replicate.selectByVisibleText(value));
    }

    public SequenceCoverageWebPart setModifiedForm(String value)
    {
        return doAndWaitForUpdate(() -> getWrapper().checkRadioButton(elementCache().modifiedForms.withAttribute("value", value)));
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
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(Locator.tagWithAttribute("img", "alt", "close").findElement(elementCache().peptideDetailsHelp))).click();
            getWrapper().shortWait().until(ExpectedConditions.invisibilityOf(elementCache().peptideDetailsHelp));
        }
        WebElement numLink = getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(
                Locator.tagWithClass("a", "_helpPopup").withText(value).childTag("div")));
        getWrapper().mouseOver(numLink);
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().peptideDetailsHelp));

        return Locator.id("helpDivBody").withText().waitForElement(elementCache().peptideDetailsHelp, 1_000).getText();
    }

    public SequenceCoverageWebPart doAndWaitForUpdate(Runnable runnable)
    {
        getWrapper().doAndWaitForPageToLoad(runnable);
        return new SequenceCoverageWebPart(getDriver());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends BodyWebPart<?>.ElementCache
    {
        final Select displayBy = SelectWrapper.Select(Locator.name("peptideSettings")).findWhenNeeded(this);
        final Select replicate = SelectWrapper.Select(Locator.name("replicateSettings")).findWhenNeeded(this);
        final Locator.XPathLocator modifiedForms = Locator.name("combinedOrStacked");
        final WebElement peptideMap = Locator.id("peptideMap").findWhenNeeded(this);
        final WebElement heatMapLegend = Locator.tagWithClass("div", "heatmap").child(Locator.tag("svg")).findWhenNeeded(this);
        final WebElement peptideDetailsHelp = Locator.id("helpDiv").findWhenNeeded(getDriver());
    }
}
