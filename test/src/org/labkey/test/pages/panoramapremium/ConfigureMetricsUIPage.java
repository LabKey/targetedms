package org.labkey.test.pages.panoramapremium;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.PortalBodyPanel;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Map;

public class ConfigureMetricsUIPage extends PortalBodyPanel
{
    public ConfigureMetricsUIPage(BaseWebDriverTest test)
    {
        super(test.getDriver());
    }

    public ConfigureMetricsUIPage setLeveyJennings(String metric, @Nullable String lowerBound, @Nullable String upperBound)
    {
        selectOptionByText(Locator.name(metric), "Levey-Jennings (+/- standard deviations)");
        if (lowerBound != null)
            setFormElement(Locator.name(metric + "-lower"), lowerBound);
        if (upperBound != null)
            setFormElement(Locator.name(metric + "-upper"), upperBound);
        return this;
    }

    public ConfigureMetricsUIPage disableMetric(QCPlotsWebPart.MetricType metricType)
    {
        waitForMetricToAppear(metricType);
        selectOptionByText(Locator.name(metricType.toString()), "Disabled, completely hide the metric");
        return this;
    }

    public ConfigureMetricsUIPage disableMetric(String metric)
    {
        selectOptionByText(waitForElement(Locator.name(metric)), "Disabled, completely hide the metric");
        return this;
    }

    public ConfigureMetricsUIPage setFixedDeviationFromMean(QCPlotsWebPart.MetricType metric, @Nullable String lowerBound, @Nullable String upperBound)
    {
        waitForMetricToAppear(metric);
        selectOptionByText(Locator.name(metric.toString()), "Fixed deviation from mean");
        if (lowerBound != null)
            setFormElement(Locator.name(metric + "-lower"), lowerBound);
        if (upperBound != null)
            setFormElement(Locator.name(metric + "-upper"), upperBound);
        return this;
    }

    public ConfigureMetricsUIPage setFixedValueCutOff(QCPlotsWebPart.MetricType metric, @Nullable String lowerBound, @Nullable String upperBound)
    {
        waitForMetricToAppear(metric);
        selectOptionByText(Locator.name(metric.toString()), "Fixed value cutoff");
        if (lowerBound != null)
            setFormElement(Locator.name(metric + "-lower"), lowerBound);
        if (upperBound != null)
            setFormElement(Locator.name(metric + "-upper"), upperBound);
        return this;
    }

    public ConfigureMetricsUIPage setShowMetricNoOutlier(QCPlotsWebPart.MetricType metric)
    {
        waitForMetricToAppear(metric);
        selectOptionByText(Locator.name(metric.toString()), "Show metric in plots, but don't identify outliers");
        return this;
    }

    public void waitForMetricToAppear(QCPlotsWebPart.MetricType metric)
    {
        shortWait().until(ExpectedConditions.visibilityOf(Locator.name(metric.toString()).findElement(getDriver())));
    }

    public String getLowerBound(String metric)
    {
        return Locator.name(metric + "-lower").findElement(getDriver()).getText();
    }

    public String getUpperBound(String metric)
    {
        return Locator.name(metric + "-upper").findElement(getDriver()).getText();
    }

    public void verifyNoDataForMetric(String metricName)
    {
        Assert.assertEquals("Data should not be present for this metric - " + metricName, getText(Locator.id(metricName)), "No data in this folder");
    }

    public void clickSave()
    {
        clickAndWait(Locator.buttonContainingText("Save"));
    }

    public String clickSaveExpectingError()
    {
        Locator.buttonContainingText("Save").findElement(getDriver()).click();
        Locator.XPathLocator errorMsgId = Locator.id("qcMetricsError");
        waitForElement(errorMsgId);
        waitFor(() -> !errorMsgId.findElement(getDriver()).getText().isEmpty(), WAIT_FOR_PAGE);
        return errorMsgId.findElement(getDriver()).getText();
    }

    public void addNewCustomMetric(Map<CustomMetricProperties, String> metricProperties)
    {
        click(Locator.tagWithText("button", "Add New Custom Metric"));
        waitForElement(Ext4Helper.Locators.window("Add New Metric"));
        Window<?> metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Metric").waitFor();
        editCustomMetricValues(metricWindow, metricProperties);
    }

    public void addNewTraceMetric(Map<TraceMetricProperties, String> traceProperties)
    {
        click(Locator.tagWithText("button", "Add New Trace Metric"));
        waitForElement(Ext4Helper.Locators.window("Add New Trace Metric"));
        Window<?> metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Trace Metric").waitFor();
        editTraceMetricValues(metricWindow, traceProperties);
    }

    public void editMetric(String metric, Map<CustomMetricProperties, String> metricProperties)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForElement(Ext4Helper.Locators.window("Edit Metric"));
        Window<?> metricWindow = new Window.WindowFinder(getDriver()).withTitle("Edit Metric").waitFor();
        editCustomMetricValues(metricWindow, metricProperties);
    }

    private void editCustomMetricValues(Window<?> metricWindow, Map<CustomMetricProperties, String> metricProperties)
    {
        metricProperties.forEach((prop, val) -> {
            if (!prop.isSelect)
            {
                setFormElement(Locator.name(prop.name()), val);
            }
            else
            {
                String label = prop.formLabel;
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
        });
        clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(metricWindow));
    }

    private void editTraceMetricValues(Window<?> metricWindow, Map<TraceMetricProperties, String> metricProperties)
    {
        metricProperties.forEach((prop, val) -> {
            if (!prop.isSelect)
            {
                setFormElement(Locator.name(prop.name()), val);
            }
            else
            {
                _ext4Helper.selectComboBoxItem(prop.formLabel, val);
            }
        });
        clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(metricWindow));
        waitForText("QC Plots");
    }

    public enum MetricType
    {
        Precursor,
        Run
    }

    public enum CustomMetricProperties
    {
        metricName("Name", false),
        series1Schema("Series 1 Schema", true),
        series2Schema("Series 2 Schema", true),
        series1Query("Series 1 Query", true),
        series2Query("Series 2 Query", true),
        series1AxisLabel("Series 1 Axis Label", false),
        series2AxisLabel("Series 2 Axis Label", false),
        metricType("Metric Type", true),
        enabledSchema("Enabled Schema", true),
        enabledQuery("Enabled Query", true);

        private final String formLabel;
        private final boolean isSelect;

        CustomMetricProperties(String formLabel, boolean isSelect)
        {
            this.formLabel = formLabel + ":";
            this.isSelect = isSelect;
        }
    }

    public enum TraceMetricProperties
    {
        metricName(null, false),
        traceName("Use Trace", true),
        yAxisLabel(null, false),
        timeValue(null, false),
        traceValue(null, false);

        private final String formLabel;
        private final boolean isSelect;

        TraceMetricProperties(String formLabel, boolean isSelect)
        {
            this.formLabel = formLabel + ":";
            this.isSelect = isSelect;
        }
    }

}
