/*
 * Copyright (c) 2019-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages.panoramapremium;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.PortalBodyPanel;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;

import java.util.Map;

public class ConfigureMetricsUIPage extends PortalBodyPanel
{

    public static final String ADD_NEW_CUSTOM_METRIC = "Add New Custom Metric";
    public static final String ADD_NEW_ANNOTATION_METRIC = "Add Annotation-Backed Metric";

    public ConfigureMetricsUIPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected void waitForPage()
    {
        waitForElement(Locator.tagWithText("button", ConfigureMetricsUIPage.ADD_NEW_CUSTOM_METRIC), 15_000);
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
        waitForElement(Locator.name(metric.toString()), WAIT_FOR_PAGE);
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
        Assert.assertEquals("Data should not be present for this metric - " + metricName, "No data in this folder", getText(Locator.id(metricName)));
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
        waitFor(() ->
        {
            String errorText = errorMsgId.findElement(getDriver()).getText();
            return !errorText.isEmpty() && !errorText.equals("Saving...");
        }, WAIT_FOR_PAGE);
        return errorMsgId.findElement(getDriver()).getText();
    }

    public void addNewCustomMetric(Map<CustomMetricProperties, String> metricProperties, boolean duplicateNameErrorExpected)
    {
        click(Locator.tagWithText("button", ADD_NEW_CUSTOM_METRIC));
        waitForCustomMetricDialog();
        fillCustomMetricForm(metricProperties);
        if (duplicateNameErrorExpected)
        {
            click(Locator.id("lk-custom-metric-save"));
            assertTextPresent("A metric with the name \"" + metricProperties.get(CustomMetricProperties.metricName) + "\" already exists. Please choose a different name.");
            click(Locator.id("lk-custom-metric-cancel"));
        }
        else
        {
            clickAndWait(Locator.id("lk-custom-metric-save"));
        }
    }

    public void addNewTraceMetric(Map<TraceMetricProperties, String> traceProperties, boolean duplicateNameErrorExpected)
    {
        click(Locator.tagWithText("button", "Add New Trace Metric"));
        waitForElement(Ext4Helper.Locators.window("Add New Trace Metric"));
        Window<?> metricWindow = new Window.WindowFinder(getDriver()).withTitle("Add New Trace Metric").waitFor();
        editTraceMetricValues(metricWindow, traceProperties, duplicateNameErrorExpected);
    }

    public void addNewAnnotationMetric(Map<AnnotationMetricProperties, String> metricProperties, boolean duplicateNameErrorExpected)
    {
        click(Locator.tagWithText("button", ADD_NEW_ANNOTATION_METRIC));
        waitForAnnotationDialog();
        fillAnnotationForm(metricProperties);
        if (duplicateNameErrorExpected)
        {
            click(Locator.id("lk-annotation-metric-save"));
            String metricName = metricProperties.get(AnnotationMetricProperties.metricName);
            assertTextPresent("A metric with the name \"" + metricName + "\" already exists. Please choose a different name.");
            click(Locator.id("lk-annotation-metric-cancel"));
        }
        else
        {
            clickAndWait(Locator.id("lk-annotation-metric-save"));
        }
    }

    public void editAnnotationMetric(String metric, Map<AnnotationMetricProperties, String> metricProperties)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForAnnotationDialog();
        fillAnnotationForm(metricProperties);
        clickAndWait(Locator.id("lk-annotation-metric-save"));
    }

    public void deleteAnnotationMetric(String metric)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForAnnotationDialog();
        click(Locator.id("lk-annotation-metric-delete"));
        acceptAlert();
        waitForPage();
        waitForElementToDisappear(Locator.linkWithText(metric));
    }

    public void editMetric(String metric, Map<CustomMetricProperties, String> metricProperties)
    {
        openForEdit(metric);
        fillCustomMetricForm(metricProperties);
        clickAndWait(Locator.id("lk-custom-metric-save"));
    }

    public void deleteMetric(String metric)
    {
        openForEdit(metric);
        doAndWaitForPageToLoad(() -> {
            click(Locator.id("lk-custom-metric-delete"));
            acceptAlert();
        });
    }

    private void openForEdit(String metric)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForCustomMetricDialog();
    }

    private void waitForCustomMetricDialog()
    {
        waitForElement(Locator.id("lk-custom-metric-dialog"));
        waitForElement(Locator.tagWithText("option", "-- Select query --"));
    }

    private void fillCustomMetricForm(Map<CustomMetricProperties, String> props)
    {
        if (props.containsKey(CustomMetricProperties.metricName))
            setFormElement(Locator.id("lk-custom-metric-name"), props.get(CustomMetricProperties.metricName));
        if (props.containsKey(CustomMetricProperties.yAxisLabel))
            setFormElement(Locator.id("lk-custom-metric-ylabel"), props.get(CustomMetricProperties.yAxisLabel));
        if (props.containsKey(CustomMetricProperties.metricType))
            click(Locator.css("input[name='customMetricType'][value='" + props.get(CustomMetricProperties.metricType).toLowerCase() + "']"));
        if (props.containsKey(CustomMetricProperties.queryName))
            selectOptionByText(Locator.id("lk-custom-metric-query"), props.get(CustomMetricProperties.queryName));
    }

    private void editTraceMetricValues(Window<?> metricWindow, Map<TraceMetricProperties, String> metricProperties, boolean duplicateNameErrorExpected)
    {
        metricProperties.forEach((prop, val) -> {
            if (!prop.isSelect)
            {
                setFormElement(Locator.name(prop.name()), val);
            }
            else if (prop.formLabel != null)
            {
                _ext4Helper.selectComboBoxItem(prop.formLabel, val);
            }
            else
            {
                _ext4Helper.selectComboBoxItem(prop.loc, val);
            }
        });
        if (duplicateNameErrorExpected)
        {
            click(Ext4Helper.Locators.ext4Button("Save"));
            assertTextPresent("A metric with the name \"" + metricProperties.get(ConfigureMetricsUIPage.TraceMetricProperties.metricName) + "\" already exists. Please choose a different name.");
            click(Ext4Helper.Locators.ext4Button("Cancel"));
        }
        else
        {
            clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(metricWindow));
            waitForElement(Locator.linkWithText(metricProperties.get(ConfigureMetricsUIPage.TraceMetricProperties.metricName)));
        }
    }

    private void waitForAnnotationDialog()
    {
        waitForElement(Locator.id("lk-annotation-metric-dialog"));
        waitForElement(Locator.tagWithText("option", "-- Select annotation --"));
    }

    private void fillAnnotationForm(Map<AnnotationMetricProperties, String> props)
    {
        if (props.containsKey(AnnotationMetricProperties.metricName))
            setFormElement(Locator.id("lk-annotation-metric-name"), props.get(AnnotationMetricProperties.metricName));
        if (props.containsKey(AnnotationMetricProperties.yAxisLabel))
            setFormElement(Locator.id("lk-annotation-metric-ylabel"), props.get(AnnotationMetricProperties.yAxisLabel));
        if (props.containsKey(AnnotationMetricProperties.annotationType))
            click(Locator.css("input[name='annotationType'][value='" + props.get(AnnotationMetricProperties.annotationType) + "']"));
        if (props.containsKey(AnnotationMetricProperties.annotationName))
        {
            String annotationName = props.get(AnnotationMetricProperties.annotationName);
            waitForElement(Locator.tagWithText("option", annotationName));
            selectOptionByText(Locator.id("lk-annotation-name-select"), annotationName);
        }
    }

    private void duplicateNameErrorExpected(String metricName)
    {
        click(Locator.id("lk-custom-metric-save"));
        assertTextPresent("A metric with the name \"" + metricName + "\" already exists. Please choose a different name.");
        click(Locator.id("lk-custom-metric-cancel"));
    }

    public void clearMetricCache()
    {
        clickButton("Clear Cached Metric Values", 0);
        waitForText("Cleared cached metrics");
    }

    public enum MetricType
    {
        Precursor,
        Run
    }

    public enum CustomMetricProperties
    {
        metricName("Name", false),
        queryName("Metrics Query", true),
        yAxisLabel("Y-Axis Label", false),
        metricType("Metric Type", true);

        private final String formLabel;
        private final boolean isSelect;

        CustomMetricProperties(String formLabel, boolean isSelect)
        {
            this.formLabel = formLabel + ":";
            this.isSelect = isSelect;
        }
    }

    public enum AnnotationMetricProperties
    {
        metricName,
        yAxisLabel,
        annotationType,   // "replicate" or "precursor"
        annotationName
    }

    public enum TraceMetricProperties
    {
        metricName(null, false),
        traceName("Use Trace", true),
        yAxisLabel(null, false),
        minTimeValue(null, false),
        maxTimeValue(null, false),
        traceValue(null, false),
        timeValueOption(null, true, Ext4Helper.Locators.formItemWithInputNamed("timeValueOption")),;

        private final String formLabel;
        private final boolean isSelect;
        private Locator.XPathLocator loc;

        TraceMetricProperties(String formLabel, boolean isSelect)
        {
            if (formLabel == null)
            {
                this.formLabel = null;
            }
            else
            {
                this.formLabel = formLabel + ":";
            }
            this.isSelect = isSelect;
        }

        TraceMetricProperties(String formLabel, boolean isSelect, Locator.XPathLocator loc)
        {
            this(formLabel, isSelect);
            this.loc = loc;
        }

    }

}
