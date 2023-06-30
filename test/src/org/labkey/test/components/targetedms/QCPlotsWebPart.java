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
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
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
                                els.addAll(elementCache().findNoRecordsMessage()),
                "QC Plots Webpart load", 10_000);
        return els.get(0);
    }

    private void doAndWaitForUpdate(Runnable action)
    {
        WebElement plot = waitForPlotPanel();

        closeBubble();
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

    @LogMethod
    public void setMetricType(@LoggedParam MetricType metricType)
    {
        if (getCurrentMetricType() != metricType)
        {
            doAndWaitForUpdate(() ->
            {
                // scroll to prevent inadvertent hover over QC Summary webpart items that show hopscotch tooltips
                getWrapper().scrollIntoView(elementCache().metricTypeCombo, true);

                getWrapper()._ext4Helper.selectComboBoxItem(elementCache().metricTypeCombo, metricType.toString());
            });
        }
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

    public List<String> getMetricTypeOptions()
    {
        return getWrapper()._ext4Helper.getComboBoxOptions(elementCache().metricTypeCombo);
    }

    public List<String> getQCPlotTypeOptions()
    {
        return elementCache().qcPlotTypeCombo.getComboBoxOptions();
    }

    public MetricType getCurrentMetricType()
    {
        WebElement typeInput = elementCache().metricTypeCombo.append(Locator.tag("input")).waitForElement(this, 1000);
        return MetricType.getEnum(typeInput.getDomProperty("value"));
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
        if (elementCache().groupedXCheckbox.get() != check)
        {
            doAndWaitForUpdate(() -> elementCache().groupedXCheckbox.set(check));
        }
    }

    public boolean isGroupXAxisValuesByDateChecked()
    {
        return elementCache().groupedXCheckbox.isChecked();
    }

    public void setShowAllPeptidesInSinglePlot(boolean check)
    {
        if (elementCache().singlePlotCheckbox.get() != check)
        {
            doAndWaitForUpdate(() -> elementCache().singlePlotCheckbox.set(check));
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
        elementCache().showExcludedCheckbox.set(check);
    }

    public boolean isShowExcludedPointsChecked()
    {
        return elementCache().showExcludedCheckbox.isChecked();
    }

    public void setShowReferenceGuideSet(boolean check)
    {
        elementCache().showReferenceGuideSet.set(check);
    }

    public void setShowExcludedPrecursors(boolean check)
    {
        elementCache().showExcludedPrecursors.set(check);
    }
    public boolean isShowReferenceGuideSetChecked()
    {
        return elementCache().showReferenceGuideSet.isChecked();
    }

    public boolean isShowAllPeptidesInSinglePlotChecked()
    {
        return elementCache().singlePlotCheckbox.isChecked();
    }

    public QCPlotsWebPart saveAsDefaultView()
    {
        clickMenuItem(false ,"Save as Default View");
        getWrapper().acceptAlert();
        return this;
    }

    public QCPlotsWebPart revertToDefaultView()
    {
        clickMenuItem("Revert to Default View");
        return this;
    }

    public void applyRange()
    {
        doAndWaitForUpdate(() -> elementCache().applyRangeButton.click());
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

        filterQCPlots("2013-08-09", "2013-08-27", expectedPlotCount);
    }

    @LogMethod
    public void resetInitialQCPlotFields()
    {
        // revert to the initial form values if any of them have changed
        setMetricType(MetricType.RETENTION);
        setDateRangeOffset(DateRangeOffset.ALL);
        setQCPlotTypes(QCPlotsWebPart.QCPlotType.LeveyJennings);
        setScale(QCPlotsWebPart.Scale.LINEAR);
        setGroupXAxisValuesByDate(false);
        setShowAllPeptidesInSinglePlot(false);

    }

    @LogMethod
    public void filterQCPlots(@LoggedParam String startDate, @LoggedParam String endDate, int expectedPlotCount)
    {
        setDateRangeOffset(DateRangeOffset.CUSTOM);
        setStartDate(startDate);
        setEndDate(endDate);
        applyRange();
        waitForPlots(expectedPlotCount);
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
                    getWrapper().mouseOver(getPointByAcquiredDate(acquiredDate));
                    return getWrapper().isElementPresent(Locator.tagWithClass("div", "x4-form-display-field").withText(acquiredDate));
                });
        return elementCache().hopscotchBubble.findElement(getDriver());
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

        getWrapper().scrollIntoView(startPoint);

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
            Window warning = Window(getDriver()).withTitle("Create Guide Set Warning").waitFor();
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
            Window error = Window(getDriver()).withTitle("Error Creating Guide Set").waitFor();
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

    private void dismissTooltip()
    {
        int halfWidth = elementCache().webPartTitle.getSize().getWidth() / 2;
        int xOffset = elementCache().webPartTitle.getLocation().getX() + halfWidth; // distance to edge of window from center of element
        getWrapper().scrollIntoView(elementCache().webPartTitle);
        new Actions(getDriver())
                .moveToElement(elementCache().webPartTitle) // Start at the center of the title
                .moveByOffset(-xOffset, 0) // Move all the way to the left edge of the window
                .perform(); // Should dismiss hover tooltips
        WebElement closeHopscotch = Locator.byClass("hopscotch-close").findElementOrNull(getDriver());
        if (closeHopscotch != null && closeHopscotch.isDisplayed())
            closeHopscotch.click();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfElementLocated(Locator.byClass("hopscotch-callout")));
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

    public void closeBubble()
    {
        Optional<WebElement> optCloseButton = elementCache().hopscotchBubbleClose.findOptionalElement(getDriver());
        optCloseButton.ifPresent(closeButton -> {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(2));
            wait.until(ExpectedConditions.elementToBeClickable(closeButton)).click();
            wait.until(ExpectedConditions.stalenessOf(closeButton));
        });
    }

    public void goToPreviousPage()
    {
        closeBubble();
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().paginationPrevBtn.findElement(this).click());
    }

    public void goToNextPage()
    {
        closeBubble();
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().paginationNextBtn.findElement(this).click());
    }

    public Locator.XPathLocator getBubble()
    {
        return Locator.byClass("hopscotch-bubble-container");
    }

    public Locator.XPathLocator getBubbleContent()
    {
        Locator.XPathLocator hopscotchBubble = Locator.byClass("hopscotch-bubble-container");
        return hopscotchBubble.append(Locator.byClass("hopscotch-bubble-content").append(Locator.byClass("hopscotch-content").withText()));
    }

    public enum Scale
    {
        LINEAR("Linear"),
        LOG("Log"),
        PERCENT_OF_MEAN("Percent of Mean"),
        STANDARD_DEVIATIONS("Standard Deviations");

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
        LeveyJennings("Levey-Jennings", "", true),
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
        TPAREAS("Transition & Precursor Areas"),
        TRANSITION_AREA("Transition Area"),
        MASSACCURACY("Mass Accuracy"),
        IRTINTERCEPT("iRT Intercept"),
        IRTSLOPE("iRT Slope"),
        IRTCORRELATION("iRT Correlation"),
        TICAREA("TIC Area");

        private final String _text;

        MetricType(String text)
        {
            _text = text;
        }

        public static MetricType getEnum(String value)
        {
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
        Locator.XPathLocator metricTypeCombo = Locator.id("metric-type-field");
        WebElement trailingLast = Locator.id("trailingRuns-inputEl").findWhenNeeded(this);

        ComboBox qcPlotTypeCombo = new ComboBox.ComboBoxFinder(getDriver()).withIdPrefix("qc-plot-type-with-y-options")
                .findWhenNeeded(this).setMatcher(Ext4Helper.TextMatchTechnique.CONTAINS).setMultiSelect(true);
        Checkbox groupedXCheckbox = new Checkbox(Locator.css("#grouped-x-field input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
        Checkbox singlePlotCheckbox = new Checkbox(Locator.css("#peptides-single-plot input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
        Checkbox showExcludedCheckbox = new Checkbox(Locator.css("#show-excluded-points input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
        Checkbox showReferenceGuideSet = new Checkbox(Locator.css("#show-oorange-gs input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
        Checkbox showExcludedPrecursors = new Checkbox(Locator.css("#show-excluded-precursors input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));


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
        Locator.CssLocator paginationPrevBtn = Locator.css(".qc-paging-prev");
        Locator.CssLocator paginationNextBtn = Locator.css(".qc-paging-next");
        Locator.CssLocator svgBackgrounds = Locator.css("svg g.brush rect.background");
        Locator.XPathLocator hopscotchBubble = Locator.byClass("hopscotch-bubble-container");
        Locator.XPathLocator hopscotchBubbleClose = Locator.byClass("hopscotch-bubble-close");

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
