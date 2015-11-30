/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;

import java.util.LinkedList;
import java.util.List;

public class ParetoPlotsWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Pareto Plots";

    public ParetoPlotsWebPart(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    public enum ChartTypeTicks
    {
        RETENTION("RT"),
        PEAK("PA"),
        FWHM("FWHM)"),
        FWB("FWB"),
        LHRATIO("Light/Heavy Ratio"),
        TPAREARATIO("T/P Ratio"),
        PAREA("P Area"),
        TAREA("T Area"),
        MASSACCURACY("MA");

        private String _text;

        ChartTypeTicks(String text)
        {
            _text = text;
        }

        public String toString()
        {
            return _text;
        }

        public static String getChartTypeTick(String value)
        {
            String chartTypeTick = null;

            for(ChartTypeTicks v : values())
            {
                if (value.contains(v.toString()))
                {
                    chartTypeTick = v.toString();
                    break;
                }
            }

            return chartTypeTick;
        }
    }
    public List<String> getTicks(int guideSetNum)
    {
        List<String> ticks = new LinkedList<>();
        int maxIndex = 5;
        int minIndex = 1;

        while(minIndex <= maxIndex)
        {
            String tickText = _test.getElement(Locator.css("#paretoPlot-GuideSet-"+ guideSetNum +
                    " > svg > g:nth-child(1) > g.tick-text > a:nth-child("+ minIndex+")")).getText();
            ticks.add(tickText);
            minIndex++;
        }
        return ticks;
    }

    public int getNumOfParetoPlots()
    {
        return _test.getElementCount(Locator.xpath("//div[contains(@id, 'tiledPlotPanel')]/table[contains(@class, 'labkey-wp pareto-plot-wp')]"));
    }

    public boolean isChartTypeTickValid(String chartType)
    {
        return ChartTypeTicks.getChartTypeTick(chartType) != null;

    }

    public int getPlotBarHeight(int guideSetId, int barPlotNum)
    {
       return Integer.parseInt(_test.getText(Locator.css("#paretoPlot-GuideSet-" + guideSetId + "-0 > a:nth-child(" + (barPlotNum+1) + ")")));
    }

    public void clickLeveyJenningsLink(BaseWebDriverTest test)
    {
        test.waitForElement(elements().notFound); //Check for no guide sets
        test.clickAndWait(elements().leveyJenningsLink); //click on the link to take user to Levey-Jennings plot
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        Locator.XPathLocator notFound = webPart.append(Locator.tagWithClass("div", "tiledPlotPanel").startsWith("Guide Sets not found."));
        Locator.XPathLocator leveyJenningsLink = webPart.append(Locator.linkWithText("Levey-Jennings QC Plots"));
    }
}
