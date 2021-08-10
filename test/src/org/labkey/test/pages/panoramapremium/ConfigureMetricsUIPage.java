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

    public enum CustomMetricProperties
    {
        metricName,
        series1Schema,
        series2Schema,
        series1Query,
        series2Query,
        series1AxisLabel,
        series2AxisLabel,
        metricType,
        enabledSchema,
        enabledQuery
    }

    public enum TraceMetricProperties
    {
        metricName,
        traceName,
        yAxisLabel,
        timeValue,
        traceValue
    }

    public ConfigureMetricsUIPage(BaseWebDriverTest test)
    {
        super(test.getDriver());
    }

    public void enableMetric(String metric)
    {
        selectOptionByText(Locator.name(metric),"Enabled");
        clickAndWait(Locator.buttonContainingText("Save"));
    }

    public void disableMetric(String metric)
    {
        selectOptionByText(Locator.name(metric),"Disabled");
        clickAndWait(Locator.buttonContainingText("Save"));
    }

    public void addNewCustomMetric(Map<CustomMetricProperties, String> metricProperties)
    {
        click(Locator.tagWithText("button", "Add New Custom Metric"));
        waitForElement(Ext4Helper.Locators.window("Add New Metric"));
        Window metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Metric").waitFor();
        editCustomMetricValues(metricWindow, metricProperties);
    }

    public void addNewTraceMetric(Map<TraceMetricProperties, String> traceProperties)
    {
        click(Locator.tagWithText("button", "Add New Trace Metric"));
        waitForElement(Ext4Helper.Locators.window("Add New Trace Metric"));
        Window metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Trace Metric").waitFor();
        editTraceMetricValues(metricWindow, traceProperties);
    }

    public void editMetric(String metric, Map<CustomMetricProperties, String> metricProperties)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForElement(Ext4Helper.Locators.window("Edit Metric"));
        Window metricWindow = new Window.WindowFinder(getDriver()).withTitle("Edit Metric").waitFor();
        editCustomMetricValues(metricWindow, metricProperties);
    }

    private void editCustomMetricValues(Window metricWindow, Map<CustomMetricProperties, String> metricProperties)
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
            if("enabledSchema".equals(prop.name()))
            {
                _ext4Helper.selectComboBoxItem("Enabled Schema:", val);
            }

            if("enabledQuery".equals(prop.name()))
            {
                _ext4Helper.selectComboBoxItem("Enabled Query:", val);
            }
        });
        clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(metricWindow));
    }

    private void editTraceMetricValues(Window metricWindow, Map<TraceMetricProperties, String> metricProperties)
    {
        metricProperties.forEach((prop, val) -> {
            if("metricName".equals(prop.name()) || "yAxisLabel".equals(prop.name()))
            {
                setFormElement(Locator.name(prop.name()), val);
            }
            if("traceName".equals(prop.name()))
            {
                _ext4Helper.selectComboBoxItem("Use Trace:", val);
            }
            if("timeValue".equals(prop.name()) || "traceValue".equals(prop.name()))
            {
                setFormElement(Locator.name(prop.name()), val);
            }
        });
        clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(metricWindow));
        waitForText("QC Plots");
    }

}
