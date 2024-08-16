package org.labkey.test.components.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
        WebDriverWrapper.waitFor(() -> elementCache().yearTitle.isDisplayed(this),
                "Calendar did not load on time",
                getWrapper().defaultWaitForPage);
    }

    public UtilizationCalendarWebPart(WebDriver driver, WebElement webPartElement)
    {
        super(driver, webPartElement);
    }

    public UtilizationCalendarWebPart setDisplay(String value)
    {
        dismissTooltip();
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
        dismissTooltip();
        getWrapper().selectOptionByText(elementCache().heatMap, value);
        return this;
    }

    private void dismissTooltip()
    {
        getWrapper().mouseOver(elementCache().yearTitle);
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfElementLocated(elementCache().tooltip));
    }

    public String getHeatMap()
    {
        return getWrapper().getSelectedOptionText(elementCache().heatMap);
    }

    public String getDisplayedMonth()
    {
        return elementCache().yearTitle.findWhenNeeded(elementCache().calendar.findWhenNeeded(this)).getText();
    }

    public UtilizationCalendarWebPart markOffline(String startDate, @Nullable String endDate, @Nullable String description)
    {
        ModalDialog modalDialog = clickDate(startDate);
        getWrapper().setFormElement(Locator.name("event-description").findElement(modalDialog.getComponentElement()), description);
        getWrapper().setFormElement(Locator.name("event-end-date").findElement(modalDialog.getComponentElement()), endDate);
        doAndWaitForElementToRefresh(() -> modalDialog.dismiss("Save"), elementCache().dayLoc.withText(extractDay(startDate)), 10);
        return this;
    }

    public UtilizationCalendarWebPart markOfflineExpectingError(String startDate, @Nullable String endDate, @Nullable String description, String errorMsg)
    {
        ModalDialog modalDialog = clickDate(startDate);
        if (description != null)
            getWrapper().setFormElement(Locator.name("event-description").findElement(modalDialog.getComponentElement()), description);
        if (endDate != null)
            getWrapper().setFormElement(Locator.name("event-end-date").findElement(modalDialog.getComponentElement()), endDate);
        getWrapper().longWait().until(ExpectedConditions.elementToBeClickable(Locator.button("Save")));
        modalDialog.dismiss("Save");
        getWrapper().waitForElement(elementCache().error_msg.withText(errorMsg));
        Assert.assertEquals("Incorrect validation message", errorMsg, elementCache().error_msg.findElement(modalDialog.getComponentElement()).getText());
        modalDialog.dismiss("Cancel");
        return this;
    }

    public ModalDialog clickDate(String date)
    {
        String clickDate = extractDay(date);
        getWrapper().log("Clicking " + clickDate + " to update the status");
        elementCache().dayLoc.withText(clickDate).findElement(elementCache().calendar.findWhenNeeded(this)).click();
        return new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Instrument Offline Annotation").waitFor();
    }

    private String extractDay(String date)
    {
        return date.substring(date.lastIndexOf("-") + 1);
    }
    public boolean isOffline(String date)
    {
        String day = date.substring(date.lastIndexOf("-") + 1);
        return getWrapper().isElementPresent(elementCache().offlineDayLoc.withText(day));
    }

    public UtilizationCalendarWebPart markOnline(String date)
    {
        if (isOffline(date))
        {
            ModalDialog modalDialog = clickDate(date);
            modalDialog.dismiss("Delete");
        }
        return this;
    }

    public String getToolTipText(String date)
    {
        String day = date.substring(date.lastIndexOf("-") + 1);
        getWrapper().log("Clicking " + day + " to update the status");
        WebDriverWrapper.sleep(500);
        getWrapper().mouseOver(elementCache().dayLoc.withText(day).findElement(elementCache().calendar.findWhenNeeded(this)));
        return getWrapper().waitForElement(elementCache().tooltip).getText();
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
        private final Locator calendar = Locator.tagWithId("div", "calendar");
        private final Locator yearTitle = Locator.tagWithClass("th", "year-title");
        private final Locator dayLoc = Locator.tagWithClassContaining("div", "day-content");

        private final Locator offlineDayLoc = Locator.tagWithClassContaining("div", "day-content offline");

        private final Locator tooltip = Locator.tagWithAttribute("div", "role", "tooltip");
        private final Locator error_msg = Locator.id("annotation-save-error");

    }
}
