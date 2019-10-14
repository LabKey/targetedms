package org.labkey.test.pages.panoramapremium;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.PortalBodyPanel;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;

import java.util.Map;

public class ConfigureMetricsUIPage extends PortalBodyPanel
{
    public enum MetricType
    {
        Precursor,
        Run
    }

    public enum MetricProperties
    {
        metricName,
        series1Schema,
        series2Schema,
        series1Query,
        series2Query,
        series1AxisLabel,
        series2AxisLabel,
        metricType
    }

    public ConfigureMetricsUIPage(BaseWebDriverTest test)
    {
        super(test.getDriver());
    }

    public void enableMetric(String metric)
    {
        checkCheckbox(Locator.checkboxByName(metric));
        click(Locator.buttonContainingText("Save"));
    }

    public void disableMetric(String metric)
    {
        uncheckCheckbox(Locator.checkboxByName(metric));
        click(Locator.buttonContainingText("Save"));
    }

    public void addNewMetric(Map<MetricProperties, String> metricProperties)
    {
        click(Locator.tagWithText("button", "Add New Metric"));
        waitForElement(Ext4Helper.Locators.window("Add New Metric"));
        Window metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Metric").waitFor();
        editMetricValues(metricWindow, metricProperties);
    }

    public void editMetric(String metric, Map<MetricProperties, String> metricProperties)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForElement(Ext4Helper.Locators.window("Edit Metric"));
        Window metricWindow = new Window.WindowFinder(getDriver()).withTitle("Edit Metric").waitFor();
        editMetricValues(metricWindow, metricProperties);
    }

    private void editMetricValues(Window metricWindow, Map<MetricProperties, String> metricProperties)
    {
        metricProperties.forEach((prop, val) -> {
            if("metricName".equals(prop.name()) || "series1AxisLabel".equals(prop.name()) || "series2AxisLabel".equals(prop.name()))
            {
                setFormElement(Locator.name(prop.name()), val);
            }
            if("series1Schema".equals(prop.name()) || "series2Schema".equals(prop.name()))
            {
                setFormElement(Locator.name(prop.name()), val);
                Locator.name(prop.name()).findElement(getDriver()).sendKeys(Keys.ENTER);
                waitForFormElementToEqual(Locator.name(prop.name()), val);
            }
            if("series1Query".equals(prop.name()) || "series2Query".equals(prop.name()))
            {
                String label = "series1Query".equals(prop.name()) ? "Series 1 Query:" : "Series 2 Query:";
                //adding waits does not help here, however it passes in catch block
                try
                {
                    _ext4Helper.selectComboBoxItem(label, val);
                }
                catch (NoSuchElementException e)
                {
                    _ext4Helper.selectComboBoxItem(label, val);
                }
            }
            if("metricType".equals(prop.name()))
            {
                _ext4Helper.selectComboBoxItem("Metric Type:", val);
            }
        });
        Ext4Helper.Locators.ext4Button("Save").findElement(metricWindow).click();
    }

}
