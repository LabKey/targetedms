package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class UtilizationCalendarWebPart extends BodyWebPart<UtilizationCalendarWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "Utilization Calendar";

    public UtilizationCalendarWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
        waitForPage();
    }

    public void waitForPage()
    {
        getWrapper().waitFor(()-> elementCache().yearTitle.isDisplayed(this),
                "Calendar did not load on time",
                getWrapper().defaultWaitForPage);
    }
    public UtilizationCalendarWebPart(WebDriver driver, WebElement webPartElement)
    {
        super(driver, webPartElement);
    }

    public UtilizationCalendarWebPart setDisplay(String value)
    {
        getWrapper().doAndWaitForElementToRefresh(() -> getWrapper().selectOptionByValue(elementCache().display, value),
                elementCache().yearTitle, getWrapper().shortWait());

        return this;
    }

    public String getDisplay()
    {
        return getWrapper().getSelectedOptionText(elementCache().display);
    }

    public UtilizationCalendarWebPart setHeatMap(String value)
    {
        getWrapper().selectOptionByText(elementCache().heatMap, value);
        return this;
    }

    public String getHeatMap()
    {
        return getWrapper().getSelectedOptionText(elementCache().heatMap);
    }

    public String getDisplayedMonth()
    {
        return elementCache().yearTitle.findWhenNeeded(elementCache().calendar).getText();
    }

    public UtilizationCalendarWebPart markOffline (String startDate, String description)
    {
        return markOffline(startDate, startDate, description);
    }
    public UtilizationCalendarWebPart markOffline(String startDate, String endDate, String description)
    {
        setDisplay("1");
        elementCache().dayLoc.withText(startDate).findElement(elementCache().calendar).click();
        ModalDialog offlineAnnotationDialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Instrument Offline Annotation").find();
        offlineAnnotationDialog.dismiss("Save");
        return this;
    }

    @Override
    protected UtilizationCalendarWebPart.Elements newElementCache()
    {
        return new UtilizationCalendarWebPart.Elements();
    }

    public class Elements extends BodyWebPart<?>.ElementCache
    {
        private final WebElement display = Locator.id("monthNumberSelect").findWhenNeeded(this);
        private final WebElement heatMap = Locator.id("heatMapSource").findWhenNeeded(this);
        private final WebElement calendar = Locator.tagWithId("div", "calendar").findWhenNeeded(this);
        private final Locator yearTitle = Locator.tagWithClass("th","year-title");
        private final Locator dayLoc = Locator.tagWithClassContaining("div", "day-content");

    }
}
