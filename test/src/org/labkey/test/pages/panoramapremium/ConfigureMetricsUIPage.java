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
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.PortalBodyPanel;
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
        waitForTraceMetricDialog();
        editTraceMetricValues(traceProperties, duplicateNameErrorExpected);
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

    public void editTraceMetric(String metric, Map<TraceMetricProperties, String> metricProperties)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForTraceMetricDialog();
        editTraceMetricValues(metricProperties, false);
    }

    /**
     * Opens the metric for edit, reports which mode radio the dialog came up in, and closes it again without saving.
     * @return "timeValue" or "traceValue"
     */
    public String getTraceMetricMode(String metric)
    {
        waitAndClick(Locator.linkWithText(metric));
        waitForTraceMetricDialog();
        String mode = Locator.css("input[name='metricValue']:checked").findElement(getDriver()).getAttribute("value");
        click(Locator.id("lk-trace-metric-cancel"));
        waitForElementToDisappear(Locator.id("lk-trace-metric-dialog"));
        return mode;
    }

    private void waitForTraceMetricDialog()
    {
        waitForElement(Locator.id("lk-trace-metric-dialog"));
        // wait on the select itself, not its placeholder option: with no traces in the container the
        // dialog renders a disabled "No trace can be found" select instead
        waitForElement(Locator.id("lk-trace-use-trace"));
    }

    private void editTraceMetricValues(Map<TraceMetricProperties, String> metricProperties, boolean duplicateNameErrorExpected)
    {
        // each mode's fields are only enabled when its radio button is selected, so pick the mode
        // before filling anything in
        if (metricProperties.containsKey(TraceMetricProperties.traceValue))
            click(Locator.css("input[name='metricValue'][value='traceValue']"));
        else
            click(Locator.css("input[name='metricValue'][value='timeValue']"));

        metricProperties.forEach((prop, val) -> {
            if (prop.isSelect)
                selectOptionByText(Locator.id(prop.elementId), val);
            else
                setFormElement(Locator.id(prop.elementId), val);
        });

        if (duplicateNameErrorExpected)
        {
            click(Locator.id("lk-trace-metric-save"));
            assertTextPresent("A metric with the name \"" + metricProperties.get(TraceMetricProperties.metricName) + "\" already exists. Please choose a different name.");
            click(Locator.id("lk-trace-metric-cancel"));
        }
        else
        {
            clickAndWait(Locator.id("lk-trace-metric-save"));
            String metricName = metricProperties.get(TraceMetricProperties.metricName);
            if (metricName != null)
                waitForElement(Locator.linkWithText(metricName));
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
        metricName("lk-trace-metric-name", false),
        traceName("lk-trace-use-trace", true),
        yAxisLabel("lk-trace-ylabel", false),
        minTimeValue("lk-trace-min-time", false),
        maxTimeValue("lk-trace-max-time", false),
        traceValue("lk-trace-value", false),
        timeValueOption("lk-trace-time-option", true);

        private final String elementId;
        private final boolean isSelect;

        TraceMetricProperties(String elementId, boolean isSelect)
        {
            this.elementId = elementId;
            this.isSelect = isSelect;
        }
    }

}
