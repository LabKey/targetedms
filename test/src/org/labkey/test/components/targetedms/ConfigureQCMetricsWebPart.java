package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ConfigureQCMetricsWebPart extends WebDriverComponent<ConfigureQCMetricsWebPart.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public ConfigureQCMetricsWebPart(WebDriver driver)
    {
        _driver = driver;
        _el = Locator.tagWithName("div", "webpart").findElement(getDriver());
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public ConfigureQCMetricsWebPart configureMetric(String metricName, String value)
    {
        getWrapper().selectOptionByTextContaining(Locator.tagWithName("Select", metricName).findElement(getDriver()), value);
        return this;
    }

    public void clickSave()
    {
        getWrapper().clickAndWait(elementCache().save);
    }

    public void clickCancel()
    {
        getWrapper().clickAndWait(elementCache().cancel);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement save = Locator.button("Save").findWhenNeeded(this);
        final WebElement cancel = Locator.button("Cancel").findWhenNeeded(this);
        final WebElement addNewCustomMetric = Locator.button("Add New Custom Metric").findWhenNeeded(this);
        final WebElement addNewTraceMetric = Locator.button("Add New Trace Metric").findWhenNeeded(this);

    }
}
