/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.test.components.targetedms;

import org.apache.commons.collections4.SetUtils;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.selenium.ScrollUtils;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.components.ext4.Window.Window;
import static org.labkey.test.util.selenium.ScrollUtils.Alignment.center;

public final class QCPlotsWebPart extends BodyWebPart<QCPlotsWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "QC Plots";

    public QCPlotsWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
    }

    public QCPlotsWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
    }

    @Override
    public void waitForReady()
    {
        waitForPlotPanel();
    }

    private WebElement waitForPlotPanel()
    {
        List<WebElement> els = new ArrayList<>();
        WebDriverWrapper.waitFor(() -> els.addAll(elementCache().findSeriesPanels()) ||
                        els.addAll(elementCache().findPlotErrors()) ||
                        els.addAll(elementCache().findNoRecordsMessage()) ||
                        els.addAll(elementCache().findNoDataMessage()),
                "QC Plots Webpart load", 10_000);
        return els.get(0);
    }

    private void doAndWaitForUpdate(Runnable action)
    {
        WebElement plot = waitForPlotPanel();

        action.run();

        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();
    }

    @LogMethod(quiet = true)
    public void setScale(@LoggedParam Scale scale)
    {
        if (getCurrentScale() != scale)
        {
            doAndWaitForUpdate(() -> getWrapper()._ext4Helper.selectComboBoxItem(elementCache().scaleCombo, scale.toString()));
        }
    }

    public Scale getCurrentScale()
    {
        WebElement scaleInput = elementCache().scaleCombo.append(Locator.tag("input")).waitForElement(this, 1000);
        return Scale.getEnum(scaleInput.getAttribute("value"));
    }

    @LogMethod(quiet = true)
    private void setDateRangeOffset(@LoggedParam DateRangeOffset dateRangeOffset)
    {
        if (getCurrentDateRangeOffset() != dateRangeOffset)
        {
            Runnable selectDateRange = () -> getWrapper()._ext4Helper.selectComboBoxItem(elementCache().dateRangeCombo, dateRangeOffset.toString());
            if (dateRangeOffset == DateRangeOffset.ALL)
            {
                doAndWaitForUpdate(selectDateRange);
            }
            else
            {
                selectDateRange.run();
            }
        }
    }

    public DateRangeOffset getCurrentDateRangeOffset()
    {
        WebElement scaleInput = elementCache().dateRangeCombo.append(Locator.tag("input")).waitForElement(this, 1000);
        return DateRangeOffset.getEnum(scaleInput.getAttribute("value"));
    }

    @LogMethod(quiet = true)
    public void setStartDate(@LoggedParam String startDate)
    {
        getWrapper().setFormElement(elementCache().startDate, startDate);
    }

    public String getCurrentStartDate()
    {
        return getWrapper().getFormElement(elementCache().startDate);
    }

    @LogMethod(quiet = true)
    public void setEndDate(@LoggedParam String endDate)
    {
        getWrapper().setFormElement(elementCache().endDate, endDate);
    }

    public String getCurrentEndDate()
    {
        return getWrapper().getFormElement(elementCache().endDate);
    }


    private void setMetricType(MetricType metricType, MetricType currentMetricType, Locator.XPathLocator metricTypeCombo)
    {
        if (currentMetricType != metricType)
        {
            doAndWaitForUpdate(() ->
            {
                getWrapper().scrollIntoView(metricTypeCombo, true);
                getWrapper()._ext4Helper.selectComboBoxItem(metricTypeCombo, metricType.toString());
            });
        }
    }

    @LogMethod
    public void setMetric1Type(@LoggedParam MetricType metricType)
    {
        setMetricType(metricType, getCurrentMetric1Type(), elementCache().metric1TypeCombo);
    }

    @LogMethod
    public void setMetric2Type(@LoggedParam MetricType metricType)
    {
        setMetricType(metricType, getCurrentMetric2Type(), elementCache().metric2TypeCombo);
    }

    @LogMethod
    public void setQCPlotTypes(@LoggedParam QCPlotType... qcPlotTypes)
    {
        Set<QCPlotType> currentQCPlotTypes = getCurrentQCPlotTypes();
        toggleQCPlotTypes(SetUtils.disjunction(Set.of(qcPlotTypes), currentQCPlotTypes));
    }

    private void toggleQCPlotTypes(Set<QCPlotType> plotTypes)
    {
        if (!plotTypes.isEmpty())
        {
            String[] typeLabels = plotTypes.stream().map(QCPlotType::getLabel).toArray(String[]::new);
            doAndWaitForUpdate(() -> elementCache().qcPlotTypeCombo.toggleComboBoxItems(typeLabels));
        }
    }

    public String getTrailingLast()
    {
        return getWrapper().getFormElement(elementCache().trailingLast);
    }

    public void setTrailingLast(String value)
    {
        doAndWaitForUpdate(() -> getWrapper().setFormElement(elementCache().trailingLast, value));
    }

    public List<String> getMetric1TypeOptions()
    {
        return getWrapper()._ext4Helper.getComboBoxOptions(elementCache().metric1TypeCombo);
    }

    public List<String> getMetric2TypeOptions()
    {
        return getWrapper()._ext4Helper.getComboBoxOptions(elementCache().metric2TypeCombo);
    }

    public List<String> getQCPlotTypeOptions()
    {
        return elementCache().qcPlotTypeCombo.getComboBoxOptions();
    }

    private MetricType getCurrentMetricType(Locator.XPathLocator metricTypeCombo)
    {
        WebElement typeInput = metricTypeCombo.append(Locator.tag("input")).waitForElement(this, 1000);
        try
        {
            return MetricType.getEnum(typeInput.getDomProperty("value"));
        }
        catch (IllegalArgumentException e)
        {
            // Custom metrics aren't in our enum
            return null;
        }
    }

    public MetricType getCurrentMetric1Type()
    {
        return getCurrentMetricType(elementCache().metric1TypeCombo);
    }

    public MetricType getCurrentMetric2Type()
    {
        return getCurrentMetricType(elementCache().metric2TypeCombo);
    }

    public Set<QCPlotType> getCurrentQCPlotTypes()
    {
        WebElement typeInput = Locator.tag("input").waitForElement(elementCache().qcPlotTypeCombo, 1000);
        return Arrays.stream(typeInput.getDomProperty("value").split(", ?"))
                .filter(s -> !s.isEmpty())
                .map(QCPlotType::getEnum).collect(Collectors.toSet());
    }

    public void setGroupXAxisValuesByDate(boolean check)
    {
        if (isGroupXAxisValuesByDateChecked() != check)
        {
            if (check)
                doAndWaitForUpdate(() -> elementCache().xAxisGroupingDateRadio.check());
            else
                doAndWaitForUpdate(() -> elementCache().xAxisGroupingReplicateRadio.check());
        }
    }

    public boolean isGroupXAxisValuesByDateChecked()
    {
        try
        {
            return elementCache().xAxisGroupingDateRadio.isSelected();
        }
        catch (NoSuchElementException | StaleElementReferenceException e)
        {
            // Fallback: if radios are not present yet, assume unchecked
            return false;
        }
    }

    public void setShowAllPeptidesInSinglePlot(boolean check)
    {
        // 'check' means show all series combined in a single plot
        try
        {
            if (isShowAllPeptidesInSinglePlotChecked() != check)
            {
                if (check)
                    doAndWaitForUpdate(() -> elementCache().plotsCombinedRadio.check());
                else
                    doAndWaitForUpdate(() -> elementCache().plotsPerPrecursorRadio.check());
            }
        }
        catch (NoSuchElementException | StaleElementReferenceException e)
        {
            // Fallback: ignore if control not present yet
        }
    }

    /**
     * This should be called only when a plot is visible.
     */
    public void setShowAllPeptidesInSinglePlot(boolean check, int expectedPlotCount)
    {
        setShowAllPeptidesInSinglePlot(check);
        waitForPlots(expectedPlotCount);
    }

    public void setShowExcludedPoints(boolean check)
    {
        if (check)
        {
            elementCache().excludedReplicatesShow.check();
        }
        else
        {
            elementCache().excludedReplicatesHide.check();
        }
    }

    public boolean isShowExcludedPointsChecked()
    {
        return elementCache().excludedReplicatesShow.isChecked();
    }

    public void setShowReferenceGuideSet(boolean check)
    {
        if (isShowReferenceGuideSetChecked() != check)
        {
            if (check)
            {
                elementCache().referenceGuideSetShow.check();
            }
            else
            {
                elementCache().referenceGuideSetHide.check();
            }
        }
    }

    public void setShowExcludedPrecursors(boolean check)
    {
        if (check)
        {
            elementCache().excludedPrecursorsShow.check();
        }
        else
        {
            elementCache().excludedPrecursorsHide.check();
        }
    }

    public boolean isShowReferenceGuideSetChecked()
    {
        return elementCache().referenceGuideSetShow.isChecked();
    }

    public boolean isShowAllPeptidesInSinglePlotChecked()
    {
        try
        {
            return elementCache().plotsCombinedRadio.isSelected();
        }
        catch (NoSuchElementException | StaleElementReferenceException e)
        {
            return false;
        }
    }

    public QCPlotsWebPart saveAsDefaultView()
    {
        clickMenuItem(false, "Save as Default View");
        getWrapper().acceptAlert();
        return this;
    }

    public QCPlotsWebPart revertToDefaultView()
    {
        clickMenuItem("Revert to Default View");
        return this;
    }

    public void waitForPlots(Integer plotCount)
    {
        if (plotCount > 0)
        {
            Supplier<String> messageSupplier = () -> "Waiting for " + plotCount + " plots. Found: " + elementCache().findPlots().size();
            WebDriverWrapper.waitFor(() -> elementCache().findPlots().size() == plotCount, messageSupplier, WebDriverWrapper.WAIT_FOR_PAGE);
        }
        else
        {
            getWrapper().longWait().until(ExpectedConditions.textToBePresentInElement(elementCache().plotPanel, "There were no records found. The date filter applied may be too restrictive."));
        }
    }

    public boolean isCombinedPlotControlVisible()
    {
        return elementCache().plotsCombinedRadio.isDisplayed();
    }

    public List<QCPlot> getPlots()
    {
        return elementCache().findSeriesPanels().stream().map(QCPlot::new).toList();
    }

    public String getSVGPlotText(String plotIdSuffix)
    {
        Locator loc = Locator.tagWithClass("div", "tiledPlotPanel").append(
                Locator.tag("div").attributeEndsWith("id", plotIdSuffix)
                        .withDescendant(Locator.xpath("//*[local-name() = 'svg']")));
        WebElement svg = loc.findElement(this);
        return svg.getText();
    }

    public List<String> getPlotTitles()
    {
        List<String> titles = new ArrayList<>();

        for (QCPlot plot : getPlots())
        {
            titles.add(plot.getPrecursor());
        }

        return titles;
    }

    public void filterQCPlotsToInitialData(int expectedPlotCount, boolean resetForm)
    {
        if (resetForm)
        {
            resetInitialQCPlotFields();
        }

        filterQCPlots("2013-08-09", "2013-08-27", resetForm);
    }

    @LogMethod
    public void resetInitialQCPlotFields()
    {
        // revert to the initial form values if any of them have changed
        setMetric1Type(MetricType.RETENTION);
        setDateRangeOffset(DateRangeOffset.ALL);
        setQCPlotTypes(QCPlotsWebPart.QCPlotType.MetricValue);
        setScale(QCPlotsWebPart.Scale.LINEAR);
        setGroupXAxisValuesByDate(false);
        setShowAllPeptidesInSinglePlot(false);

    }

    @LogMethod
    public void filterQCPlots(@LoggedParam String startDate, @LoggedParam String endDate, boolean waitForPlotsToRefresh)
    {
        setDateRangeOffset(DateRangeOffset.CUSTOM);
        setStartDate(startDate);
        if (waitForPlotsToRefresh)
        {
            doAndWaitForUpdate(() -> setEndDate(endDate));
        }
        else
        {
            setEndDate(endDate);
        }
    }

    public int getGuideSetTrainingRectCount()
    {
        return elementCache().guideSetTrainingRect.findElements(getDriver()).size();
    }

    public List<String> getGuideSetTrainingRectTitle(int count)
    {
        List<String> titles = new ArrayList<>();
        int i = 1;
        for (WebElement e : elementCache().guideSetTrainingRect.findElements(getDriver()))
        {
            titles.add(e.getText());
            if (i < count)
                i++;
            else
                break; //Get only information of guideSet based on the count.
        }

        return titles;
    }

    public String getExperimentRangeRectTitle()
    {
        return elementCache().experimentRangeRect.waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).getText();
    }

    public int getGuideSetErrorBarPathCount()
    {
        return Locator.css("svg g g.error-bar").findElements(getDriver()).size();
    }

    public List<WebElement> getPointElements(String attr, String value, boolean isPrefix)
    {
        Locator.tag("svg").waitForElement(this, WAIT_FOR_JAVASCRIPT);
        List<WebElement> matchingPoints = new ArrayList<>();

        for (WebElement point : elementCache().svgPointPath.findElements(this))
        {
            if ((isPrefix && point.getAttribute(attr).startsWith(value))
                    || (!isPrefix && point.getAttribute(attr).equals(value)))
            {
                matchingPoints.add(point);
            }
        }

        return matchingPoints;
    }

    public WebElement getPointByAcquiredDate(String dateStr)
    {
        dateStr = dateStr.replaceAll("/", "-"); // convert 2013/08/14 -> 2013-08-14
        WebElement point = elementCache().svgPoint.attributeStartsWith("id", dateStr).findElementOrNull(this);
        if (point == null)
        {
            throw new NoSuchElementException("Unable to find svg point with with acquired date: " + dateStr);
        }
        return point;
    }

    public int getTotalPlotCount()
    {
        return elementCache().findPlots().size();
    }

    public WebElement openExclusionBubble(String acquiredDate)
    {
        getWrapper().shortWait().ignoring(StaleElementReferenceException.class).withMessage("Exclusion pop-up for Acquired Date = " + acquiredDate)
                .until(input -> {
                    WebElement point = getPointByAcquiredDate(acquiredDate);
                    ScrollUtils.scrollIntoView(point, center, center);
                    getWrapper().mouseOverWithoutScrolling(point);
                    return getWrapper().isElementPresent(Locator.tagWithClass("div", "qc-plot-hover-panel")
                            .withDescendant(Locator.tagWithClass("div", "qc-hover-field")
                                    .containing(acquiredDate.substring(0, 16)))); // drop seconds part (e.g. "2013-08-12 04:54") for trailing mean/CV
                });
        return elementCache().tippyBubble.findElement(getDriver());
    }

    @LogMethod
    public void createGuideSet(@LoggedParam GuideSet guideSet, String expectErrorMsg)
    {
        waitForReady();
        getWrapper().clickButton("Create Guide Set", 0);

        WebElement startPoint;
        WebElement endPoint;
        int xStartOffset, yStartOffset;
        int xEndOffset, yEndOffset;
        yStartOffset = 10;
        yEndOffset = 10;

        // If StartDate is empty use the far left of the svg as the starting point.
        if (!guideSet.getStartDate().trim().isEmpty())
        {
            startPoint = getPointByAcquiredDate(guideSet.getStartDate());
            xStartOffset = -10;
        }
        else
        {
            startPoint = elementCache().svgBackgrounds.findElements(this).get(0);
            xStartOffset = -1 * (Integer.parseInt(startPoint.getAttribute("width")) / 2);
        }

        // If EndDate is empty use the far right of the svg as the ending point.
        if (!guideSet.getEndDate().trim().isEmpty())
        {
            endPoint = getPointByAcquiredDate(guideSet.getEndDate());
            xEndOffset = 10;
        }
        else
        {
            endPoint = elementCache().svgBackgrounds.findElements(this).get(0);
            xEndOffset = (Integer.parseInt(endPoint.getAttribute("width")) / 2) - 1;
        }

        getWrapper().scrollIntoView(startPoint, true);

        Actions builder = new Actions(getWrapper().getDriver());

        builder.moveToElement(startPoint, xStartOffset, yStartOffset).clickAndHold().moveToElement(endPoint, xEndOffset, yEndOffset).release().perform();

        List<WebElement> gsButtons = elementCache().guideSetSvgButton.findElements(this);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(gsButtons.get(0)));

        Integer brushPointCount = getPointElements("fill", "rgba(20, 204, 201, 1)", false).size();
        assertEquals("Unexpected number of points selected via brushing", guideSet.getBrushSelectedPoints(), brushPointCount);

        boolean expectPageReload = expectErrorMsg == null;
        if (guideSet.getBrushSelectedPoints() != null && guideSet.getBrushSelectedPoints() < 5)
        {
            gsButtons.get(0).click(); // Create button : index 0
            Window<?> warning = Window(getDriver()).withTitle("Create Guide Set Warning").waitFor();
            if (expectPageReload)
                warning.clickButton("Yes");
            else
                warning.clickButton("Yes", false);
        }
        else if (expectPageReload)
        {
            getWrapper().clickAndWait(gsButtons.get(0)); // Create button : index 0
            waitForReady();
        }
        else
        {
            gsButtons.get(0).click(); // Create button : index 0
        }

        if (expectErrorMsg != null)
        {
            Window<?> error = Window(getDriver()).withTitle("Error Creating Guide Set").waitFor();
            getWrapper().assertElementPresent(elementCache().extFormDisplay.withText(expectErrorMsg));
            error.clickButton("OK", true);
            gsButtons.get(1).click(); // Cancel button : index 1
        }
    }

    public int getLogScaleInvalidCount()
    {
        return elementCache().logScaleInvalid().size();
    }

    public int getLogScaleWarningCount()
    {
        return elementCache().logScaleWarning().size();
    }

    public int getLogScaleEpsilonWarningCount()
    {
        return elementCache().logScaleEpsilonWarning().size();
    }

    public Locator getLegendItemLocator(String text, boolean exactMatch)
    {
        if (exactMatch)
            return elementCache().legendItem.withText(text);
        else
            return elementCache().legendItem.containing(text);
    }

    public Locator getTreeLegendItemLocator(String text, boolean exactMatch)
    {
        if (exactMatch)
            return elementCache().combinedTreeLegendItem.withText(text);
        else
            return elementCache().combinedTreeLegendItem.containing(text);
    }

    public Locator getTreeLegendPrecursorLocator(String text)
    {
        // Use containing to ignore charge state suffixes like "(2+)"
        return elementCache().combinedTreeLegendPrecursor.containing(text);
    }

    public Locator getLegendItemLocatorByTitle(String text)
    {
        // Use containing instead of withText() to ignore the +2 or other suffices
        return elementCache().legendItemTitle.containing(text);
    }

    public Locator getLegendPopupItemLocator(String text, boolean exactMatch)
    {
        if (exactMatch)
            return elementCache().legendItemPopup.withText(text);
        else
            return elementCache().legendItemPopup.containing(text);
    }

    public String getPaginationText()
    {
        return elementCache().paginationPanel.getText();
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public void openLegendPopup()
    {
        getWrapper().waitAndClick(Locator.tagWithText("span", "View Legend"));
        Window(getDriver()).withTitle("Legends").waitFor();
    }

    public void checkPlotType(QCPlotType plotType)
    {
        if (!isPlotTypeSelected(plotType))
        {
            toggleQCPlotTypes(Set.of(plotType));
        }
    }

    public boolean isPlotTypeSelected(QCPlotType plotType)
    {
        return getCurrentQCPlotTypes().contains(plotType);
    }

    public void checkAllPlotTypes(boolean selected)
    {
        if (selected)
        {
            setQCPlotTypes(QCPlotsWebPart.QCPlotType.values());
        }
        else
        {
            setQCPlotTypes();
        }
    }

    public void goToPreviousPage()
    {
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().paginationPrevBtn.findElement(this).click());
    }

    public void goToNextPage()
    {
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().paginationNextBtn.findElement(this).click());
    }

    public Locator.XPathLocator getBubbleContent()
    {
        return elementCache().tippyBubble;
    }

    public ConfigureMetricsUIPage clickConfigureQCMetrics()
    {
        clickMenuItem("Configure QC Metrics");
        return new ConfigureMetricsUIPage(getDriver());
    }

    public enum Scale
    {
        LINEAR("Linear"),
        LOG("Log"),
        PERCENT_OF_MEAN("Percent of Mean"),
        STANDARD_DEVIATIONS("Standard Deviations"),
        DELTA_FROM_MEAN("Delta from Mean");

        private final String _text;

        Scale(String text)
        {
            _text = text;
        }

        public static Scale getEnum(String value)
        {
            for (Scale v : values())
                if (v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException();
        }

        public String toString()
        {
            return _text;
        }
        }

    public enum DateRangeOffset
    {
        ALL(0, "All dates"),
        LAST_7_DAYS(180, "Last 7 days"),
        LAST_180_DAYS(180, "Last 180 days"),
        CUSTOM(-1, "Custom range");

        private final Integer _offset;
        private final String _label;

        DateRangeOffset(Integer offset, String label)
        {
            _offset = offset;
            _label = label;
        }

        public static DateRangeOffset getEnum(String value)
        {
            for (DateRangeOffset v : values())
                if (v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException(value);
        }

        public Integer getOffset()
        {
            return _offset;
        }

        public String toString()
        {
            return _label;
        }
    }

    public enum QCPlotType
    {
        MetricValue("Metric Value", "", true),
        MovingRange("Moving Range", "_mR", true),
        CUSUMm("CUSUMm", "_CUSUMm", true),
        CUSUMv("CUSUMv", "_CUSUMv", true),
        TrailingCV("Trailing CV", "", false),
        TrailingMean("Trailing Mean", "", false);

        private final String _label;
        private final String _idSuffix;
        private final boolean _standardPointCount;

        QCPlotType(String label, String idSuffix, boolean standardPointCount)
        {
            _label = label;
            _idSuffix = idSuffix;
            _standardPointCount = standardPointCount;
        }

        public String getLabel()
        {
            return _label;
        }

        public String getIdSuffix()
        {
            return _idSuffix;
        }

        public boolean isStandardPointCount()
        {
            return _standardPointCount;
        }

        @Override
        public String toString()
        {
            return _label;
        }

        public static QCPlotType getEnum(String value)
        {
            for (QCPlotType v : values())
                if (v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException(value);
        }
    }

    public enum QCPlotExclusionState
    {
        Include("Include"),
        ExcludeMetric("Exclude replicate for this metric"),
        ExcludeAll("Exclude replicate for all metrics");

        private final String _label;

        QCPlotExclusionState(String label)
        {
            _label = label;
        }

        public String getLabel()
        {
            return _label;
        }
    }

    public enum MetricType
    {
        RETENTION("Retention Time"),
        TOTAL_PEAK("Total Peak Area (Precursor + Transition)"),
        PRECURSOR_AREA("Precursor Area"),
        FWHM("Full Width at Half Maximum (FWHM)"),
        FWB("Full Width at Base (FWB)"),
        LHRATIO("Light/Heavy Ratio"),
        TPAREARATIO("Transition/Precursor Area Ratio"),
        TRANSITION_AREA("Transition Area"),
        PRECURSOR_MASS_ERROR("Precursor Mass Error"),
        TRANSITION_MASS_ERROR("Transition Mass Error"),
        IRTINTERCEPT("iRT Intercept"),
        IRTSLOPE("iRT Slope"),
        IRTCORRELATION("iRT Correlation"),
        ISOTOPE_DOTP("Isotope dotp"),
        TIC_AREA("TIC Area");

        private final String _text;

        MetricType(String text)
        {
            _text = text;
        }

        public static MetricType getEnum(String value)
        {
            if (value == null || value.isEmpty())
            {
                return null;
            }

            for (MetricType v : values())
                if (v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException(value);
        }

        public String toString()
        {
            return _text;
        }
    }

    public class Elements extends BodyWebPart<?>.ElementCache
    {
        WebElement startDate = Locator.css("#start-date-field input").findWhenNeeded(this);
        WebElement endDate = Locator.css("#end-date-field input").findWhenNeeded(this);
        WebElement applyRangeButton = Ext4Helper.Locators.ext4Button("Apply").findWhenNeeded(this);
        Locator.XPathLocator scaleCombo = Locator.id("scale-combo-box");
        Locator.XPathLocator dateRangeCombo = Locator.id("daterange-combo-box");
        Locator.XPathLocator metric1TypeCombo = Locator.id("metric-type-field1");
        Locator.XPathLocator metric2TypeCombo = Locator.id("metric-type-field2");
        WebElement trailingLast = Locator.id("trailingRuns-inputEl").findWhenNeeded(this);

        ComboBox qcPlotTypeCombo = new ComboBox.ComboBoxFinder(getDriver()).withIdPrefix("qc-plot-type-with-y-options")
                .findWhenNeeded(this).setMatcher(Ext4Helper.TextMatchTechnique.CONTAINS).setMultiSelect(true);
        WebElement groupedXPerReplicate = Locator.css("#grouped-x-field input[value=replicate]").findWhenNeeded(this);

        RadioButton xAxisGroupingReplicateRadio = new RadioButton.RadioButtonFinder().withLabel("per replicate").findWhenNeeded(getDriver());
        RadioButton xAxisGroupingDateRadio = new RadioButton.RadioButtonFinder().withLabel("per date").findWhenNeeded(getDriver());

        RadioButton plotsCombinedRadio = new RadioButton.RadioButtonFinder().withLabel("combined").findWhenNeeded(getDriver());
        RadioButton plotsPerPrecursorRadio = new RadioButton.RadioButtonFinder().withLabel("per precursor").findWhenNeeded(getDriver());

        // These have the same label as another group, but are first in the page
        RadioButton excludedReplicatesShow = new RadioButton.RadioButtonFinder().withLabel("show").findWhenNeeded(getDriver());
        RadioButton excludedReplicatesHide = new RadioButton.RadioButtonFinder().withLabel("hide").findWhenNeeded(getDriver());

        // Note that these two won't work with the isChecked() call but they have the same labels as the ones above so we can't simply check by label
        RadioButton excludedPrecursorsShow = new RadioButton(Locator.id("excluded-precursors-show").findWhenNeeded(getDriver()));
        RadioButton excludedPrecursorsHide = new RadioButton(Locator.id("excluded-precursors-hide").findWhenNeeded(getDriver()));

        RadioButton referenceGuideSetShow = new RadioButton.RadioButtonFinder().withLabel("always show").findWhenNeeded(getDriver());
        RadioButton referenceGuideSetHide = new RadioButton.RadioButtonFinder().withLabel("when in date range").findWhenNeeded(getDriver());

        WebElement plotPanel = Locator.css("div.tiledPlotPanel").findWhenNeeded(this);
        WebElement paginationPanel = Locator.css("div.plotPaginationHeaderPanel").findWhenNeeded(this);
        Locator extFormDisplay = Locator.css("div.x4-form-display-field");
        Locator.CssLocator guideSetTrainingRect = Locator.css("svg rect.training");
        Locator.CssLocator experimentRangeRect = Locator.css("svg rect.expRange");
        Locator.CssLocator guideSetSvgButton = Locator.css("svg g.guideset-svg-button text");
        Locator.CssLocator svgPoint = Locator.css("svg g a.point");
        Locator.CssLocator svgPointPath = Locator.css("svg g a.point path");
        Locator.CssLocator legendItem = Locator.css("svg g.legend-item");
        Locator.CssLocator legendItemTitle = Locator.css("svg g.legend-item title");
        Locator.CssLocator legendItemPopup = Locator.css(".headerlegendpopup svg g.legend-item");
        Locator.CssLocator combinedTreeLegendItem = Locator.css(".qc-combined-tree-legend > div");
        Locator.CssLocator combinedTreeLegendPrecursor = Locator.css(".qc-tree-precursor > div");
        Locator.CssLocator paginationPrevBtn = Locator.css(".qc-paging-prev");
        Locator.CssLocator paginationNextBtn = Locator.css(".qc-paging-next");
        Locator.CssLocator svgBackgrounds = Locator.css("svg g.brush rect.background");
        Locator.XPathLocator tippyBubble = Locator.tagWithClass("div", "qc-plot-hover-panel");

        List<WebElement> findSeriesPanels()
        {
            return Locator.css("table.qc-plot-wp").findElements(plotPanel);
        }

        List<WebElement> findPlots()
        {
            return Locator.byClass("chart-render-div").findElements(plotPanel);
        }

        List<WebElement> findNoRecordsMessage()
        {
            return Locator.tagContainingText("span", "There were no records found.").findElements(plotPanel);
        }

        List<WebElement> findNoDataMessage()
        {
            return Locator.tagContainingText("span", "No data found.").findElements(plotPanel);
        }

        List<WebElement> findPlotErrors()
        {
            return Locators.labkeyError.findElements(plotPanel);
        }

        List<WebElement> logScaleInvalid()
        {
            return Locator.tagContainingText("span", "Log scale invalid for values").findElements(plotPanel);
        }

        List<WebElement> logScaleWarning()
        {
            return Locator.tagContainingText("span", "For log scale, standard deviations below the mean").findElements(plotPanel);
        }

        List<WebElement> logScaleEpsilonWarning()
        {
            return Locator.tagContainingText("span", "Values that are 0 have been replaced").findElements(plotPanel);
        }
    }
}
