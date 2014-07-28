/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.chart;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.ui.Layer;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * User: vsharma
 * Date: 5/1/12
 * Time: 9:01 AM
 */
class ChromatogramChartMaker
{
    private static final Font TITLE_FONT = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12);
    private static final Font SMALL_LABEL_FONT = new Font("Tahoma", Font.BOLD, 11);

    private void addAnnotation(ChromatogramDataset.ChartAnnotation annotation, JFreeChart chart)
    {
        //TODO: Draw sublable under label
        String label = annotation.getLabels().get(0);
        String sublable = annotation.getLabels().get(1);

        XYPointerAnnotation pointer = new XYPointerAnnotation(label + sublable, annotation.getRetentionTime(), annotation.getIntensity(), Math.PI);
        pointer.setTipRadius(3.0);  // The radius from the (x, y) point to the tip of the arrow
        pointer.setBaseRadius(13.0); // The radius from the (x, y) point to the start of the arrow line
        pointer.setArrowLength(13.0);  //The length of the arrow head
        pointer.setLabelOffset(-5.0);
        pointer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        pointer.setPaint(annotation.getColor());
        pointer.setTextAnchor(TextAnchor.BOTTOM_LEFT);

        chart.getXYPlot().addAnnotation(pointer);
    }

    public JFreeChart make(final ChromatogramDataset chromatogramDataset)
    {
        chromatogramDataset.build();

        JFreeChart chart = ChartFactory.createXYLineChart(chromatogramDataset.getChartTitle(),
                "Retention Time", "Intensity " + getIntensityScaleString(chromatogramDataset),
                 chromatogramDataset.getJFreeDataset(),
                PlotOrientation.VERTICAL, true, false, false);

         setupSeriesColors(chart, chromatogramDataset);

        // Hide grid lines
        chart.getXYPlot().setRangeGridlinesVisible(false);
        chart.getXYPlot().setDomainGridlinesVisible(false);

        chart.getPlot().setBackgroundPaint(ChartColor.WHITE);

        Double peakStartTime = chromatogramDataset.getPeakStartTime();
        Double peakEndTime = chromatogramDataset.getPeakEndTime();

        if(peakStartTime != null && peakEndTime != null)
        {
            // Add peak integration boundaries
            IntervalMarker marker = new IntervalMarker(peakStartTime, peakEndTime);
            marker.setOutlinePaint(Color.BLACK);
            marker.setPaint(Color.WHITE);
            marker.setOutlineStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{2.0f, 4.0f}, 0.0f));
            chart.getXYPlot().addDomainMarker(marker, Layer.BACKGROUND);
        }

        // Add annotations
        for(ChromatogramDataset.ChartAnnotation annotation: chromatogramDataset.getChartAnnotations())
        {
            addAnnotation(annotation, chart);
        }

        // Limit labels to one decimal place on the x-axis (retention time).
        NumberAxis xAxis = (NumberAxis)chart.getXYPlot().getDomainAxis();
        xAxis.setNumberFormatOverride(new DecimalFormat("0.0"));

        // Display scaled values in the y-axis tick labels.  The scaling factor is displayed in the axis label.
        NumberAxis yAxis = (NumberAxis)chart.getXYPlot().getRangeAxis();
        yAxis.setNumberFormatOverride(new NumberFormat(){
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
            {
                // Display the scaled value in the tick label.
                return toAppendTo.append(NumberFormat.getIntegerInstance().format(number / chromatogramDataset.getIntensityScale()));
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
            {
                return format((double)number, toAppendTo, pos);
            }

            @Override
            public Number parse(String source, ParsePosition parsePosition)
            {
                throw new UnsupportedOperationException();
            }
        });


        // TODO Calculate margin amount based on peak apex annotations.
        chart.getXYPlot().getRangeAxis().setUpperMargin(0.15);

        if(chromatogramDataset.getMaxDisplayIntensity() != null)
        {
            double intensity = chromatogramDataset.getMaxDisplayIntensity();
            double margin = intensity * 0.15;
            chart.getXYPlot().getRangeAxis().setUpperBound(intensity + margin);
        }

        Double minRt = chromatogramDataset.getMinDisplayRetentionTime();
        Double maxRt = chromatogramDataset.getMaxDisplayRetentionTime();

        if(minRt != null && maxRt != null)
        {
            double range = maxRt - minRt;
            double margin = range * 3  * 0.05;
            chart.getXYPlot().getDomainAxis().setLowerBound(minRt  - margin);
            chart.getXYPlot().getDomainAxis().setUpperBound(maxRt  + margin);
        }

        chart.getTitle().setFont(TITLE_FONT);

        return chart;
    }

    public String getIntensityScaleString(ChromatogramDataset dataset)
    {
        int scale = dataset.getIntensityScale();
        return scale == 1 ? "" : (scale == 1000 ? "10^3" : "10^6");
    }

    private void setupSeriesColors(JFreeChart chart, ChromatogramDataset chromatogramDataset)
    {
        XYPlot plot = chart.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();

        for(int i = 0; i < plot.getSeriesCount(); i++)
        {
            renderer.setSeriesPaint(i, chromatogramDataset.getSeriesColor(i));
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
        }
    }

    public JFreeChart make(ChromatogramDataset dataset1, ChromatogramDataset dataset2)
    {
        JFreeChart precursorChart = make(dataset1);
        JFreeChart productChart = make(dataset2);
        if(precursorChart.getXYPlot().getDataset().getSeriesCount() == 0)
        {
            return productChart;
        }
        if(productChart.getXYPlot().getDataset().getSeriesCount() == 0)
        {
            return precursorChart;
        }
        NumberAxis domain = (NumberAxis) precursorChart.getXYPlot().getDomainAxis();
        domain.setVerticalTickLabels(true);

        // Make the y-axis label font smaller.
        // TODO: Can we have just one label for both plots?
        productChart.getXYPlot().getRangeAxis().setLabelFont(SMALL_LABEL_FONT);
        precursorChart.getXYPlot().getRangeAxis().setLabelFont(SMALL_LABEL_FONT);

        // Combine the two plots
        final CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
        plot.setGap(4.0);
        plot.setDomainAxis(domain);
        plot.add(precursorChart.getXYPlot(), 1);
        plot.add(productChart.getXYPlot(), 1);

        plot.setOrientation(PlotOrientation.VERTICAL);
        JFreeChart jchart = new JFreeChart(productChart.getTitle().getText(),
                                           TITLE_FONT, plot, true);
        jchart.setBackgroundPaint(Color.WHITE);
        return jchart;
    }
}
