/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.LeveyJenningsPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'Metric Value' : 'A metric value plot shows the raw value of the metric. It may be compared against fixed ' +
                    'upper and lower bounds to identify outliers, or use a Levey-Jennings-style comparison based on the ' +
                    'number of standard deviations (SD) it differs from the metric\'s mean value.'
        }
    },

    processLJGuideSetData : function(plotDataRows) {
        this.guideSetDataMap = {};
        this.defaultGuideSet = {};
        Ext4.each(plotDataRows, function(plotDataRow) {
            Ext4.each(plotDataRow.GuideSetStats, function(guideSetStat) {
                var guideSetId = guideSetStat['GuideSetId'];
                var seriesLabel = plotDataRow['SeriesLabel'];
                var seriesType = guideSetStat['SeriesType'] === 2 ? 'series2' : 'series1';

                if (guideSetId > 0) {
                    if (!this.guideSetDataMap[guideSetId]) {
                        this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(guideSetStat);
                    }

                    if (!this.guideSetDataMap[guideSetId].Series[seriesLabel]) {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel] = {};
                    }

                    this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType] = {
                        NumRecords: guideSetStat['NumRecords'],
                        Mean: guideSetStat['LJMean'],
                        StdDev: guideSetStat['LJStdDev']
                    };
                }
                else {
                    if (!this.defaultGuideSet) {
                        this.defaultGuideSet = {};
                    }

                    if (!this.defaultGuideSet[seriesLabel]) {
                        this.defaultGuideSet[seriesLabel] = {};
                    }

                    if (!this.defaultGuideSet[seriesLabel][seriesType]) {
                        this.defaultGuideSet[seriesLabel][seriesType] = {};
                    }

                    this.defaultGuideSet[seriesLabel][seriesType].LJ = {
                        NumRecords: guideSetStat['NumRecords'],
                        Mean: guideSetStat['LJMean'],
                        StdDev: guideSetStat['LJStdDev']
                    };
                }
            }, this);

        }, this);
    },

    setLJSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        var val = row['value'];
        if (LABKEY.vis.isValid(val)) {
            if (dataObject.min == null || val < dataObject.min) {
                dataObject.min = val;
            }
            if (dataObject.max == null || val > dataObject.max) {
                dataObject.max = val;
            }

            if (this.yAxisScale == 'log' && val <= 0) {
                dataObject.showLogInvalid = true;
            }

            var mean = row['mean'];
            var sd = LABKEY.vis.isValid(row['stdDev']) ? row['stdDev'] : 0;

            // Issue 28462: don't include the +/-3 stddev error bars in min/max calculation when it isn't being plotted
            if (!this.singlePlot && LABKEY.vis.isValid(mean)) {
                var minSd = (mean - (3 * sd));
                if (dataObject.showLogInvalid == undefined && this.yAxisScale == 'log' && minSd <= 0) {
                    // Avoid setting our scale to be negative based on the three standard deviations to avoid messing up log plots
                    dataObject.showLogWarning = true;
                    for (var i = 2; i >= 0; i--)
                    {
                        minSd = (mean - (i * sd));
                        if (minSd > 0) {
                            break;
                        }
                    }
                }
                if (dataObject.min == null || minSd < dataObject.min) {
                    dataObject.min = minSd;
                }

                if (dataObject.max == null || (mean + (3 * sd)) > dataObject.max) {
                    dataObject.max = (mean + (3 * sd));
                }
            }
        }
        else if (this.isMultiSeries()) {
            // check if either of the y-axis metric values are invalid for a log scale
            var val1 = row['value_series1'],
                    val2 = row['value_series2'];
            if (dataObject.showLogInvalid == undefined && this.yAxisScale == 'log') {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0)) {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },

    getLJPlotTypeProperties: function(precursorInfo, metricProps) {
        var plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries()) {
            plotProperties['value'] = 'value_series1';
            plotProperties['valueRight'] = 'value_series2';
        }
        else {
            plotProperties['value'] = 'value';
            plotProperties['mean'] = 'mean';
            plotProperties['stdDev'] = 'stdDev';
            plotProperties['yAxisDomain'] = [precursorInfo.min, precursorInfo.max];
        }

        plotProperties['lowerBound'] = metricProps.lowerBound;
        plotProperties['upperBound'] = metricProps.upperBound;
        if (metricProps.metricStatus === LABKEY.targetedms.MetricStatus.ValueCutoff) {
            plotProperties['boundType'] = LABKEY.vis.PlotProperties.BoundType.Absolute;
        }
        else if (metricProps.metricStatus === LABKEY.targetedms.MetricStatus.MeanDeviationCutoff) {
            plotProperties['boundType'] = LABKEY.vis.PlotProperties.BoundType.MeanDeviation;
        }
        else {
            plotProperties['boundType'] = LABKEY.vis.PlotProperties.BoundType.StandardDeviation;
        }
        return plotProperties;
    },

    getLJInitFragmentPlotData: function() {
        return {
            min: null,
            max: null
        }
    },

    processLJPlotDataRow: function(row, fragment, seriesType, metricProps)
    {
        var data = {};
        // if a guideSetId is defined for this row, include the guide set stats values in the data object
        if (Ext4.isDefined(row['GuideSetId']) && row['GuideSetId'] > 0) {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment]&& gs.Series[fragment][seriesType]) {
                data['mean'] = gs.Series[fragment][seriesType]['Mean'];
                data['stdDev'] = gs.Series[fragment][seriesType]['StdDev'];
            }
        }

        if (this.isMultiSeries()) {
            data['value_' + seriesType] = row['Value'];
            data['value_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
        }
        else {
            data['value'] = row['Value'];
        }

        data.LJShape = (row.IgnoreInQC ? 'Exclude' : 'Include') + (row.ValueOutlier ? '-Outlier' : '');

        return data;

    },

    processLJCombinedMinMax: function (combinePlotData, precursorInfo)
    {
        if (combinePlotData.min == null || combinePlotData.min > precursorInfo.min)
        {
            combinePlotData.min = precursorInfo.min;
        }
        if (combinePlotData.max == null || combinePlotData.max < precursorInfo.max)
        {
            combinePlotData.max = precursorInfo.max;
        }

        combinePlotData.fragment = precursorInfo.fragment;
    },

    getLJCombinedPlotLegendSeries: function()
    {
        return ['value_series1', 'value_series2'];
    },

    getLJLegend: function () {
        var ljLegend = [];

        if (!this.getMetricPropsById(this.metric).series2Label) {
            let metricInfo = this.getMetricPropsById(this.metric);

            if (metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.ValueCutoff || metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.MeanDeviationCutoff) {
                if (Number.isFinite(metricInfo.upperBound)) {
                    ljLegend.push({
                        text: 'Upper: ' + metricInfo.upperBound,
                        color: 'red',
                        shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                    });
                }
                if (Number.isFinite(metricInfo.lowerBound)) {
                    ljLegend.push({
                        text: 'Lower: ' + metricInfo.lowerBound,
                        color: 'red',
                        shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                    });
                }
            }

            if ( (metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.LeveyJennings || metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.PlotOnly) &&
                    (!this.singlePlot && this.yAxisScale === 'standardDeviation')) {

                let upper = Number.isFinite(metricInfo.upperBound) ? metricInfo.upperBound : 3;
                let lower = Number.isFinite(metricInfo.lowerBound) ? metricInfo.lowerBound : -3;

                if (lower === upper * -1) {
                    ljLegend.push({
                        text: '+/- ' + upper + ' Std Dev',
                        color: 'red',
                        shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                    });
                }
                else {
                    ljLegend.push({
                        text: (upper > 0 ? '+' : '') + upper + '/' + (lower > 0 ? '+' : '') + lower + ' Std Dev',
                        color: 'red',
                        shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                    });
                }
            }

            if (!this.singlePlot) {
                ljLegend.push({
                    text: 'Mean',
                    color: 'darkgrey',
                    shape: LABKEY.vis.TrendingLineShape.meanLJ
                });
            }
        }

        if (ljLegend.length > 0)
        {
            ljLegend.splice(0, 0, {
                text: '',
                separator: true
            });
        }
        return ljLegend;
    }

});