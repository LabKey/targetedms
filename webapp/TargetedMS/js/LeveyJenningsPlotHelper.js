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
                const guideSetId = guideSetStat['GuideSetId'];
                const seriesLabel = plotDataRow['SeriesLabel'];
                const metricId = guideSetStat['MetricId'];

                if (!this.guideSetDataMap[guideSetId]) {
                    this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(guideSetStat);
                }

                if (!this.guideSetDataMap[guideSetId].Series[seriesLabel]) {
                    this.guideSetDataMap[guideSetId].Series[seriesLabel] = {};
                }

                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId] = {
                    NumRecords: guideSetStat['NumRecords'],
                    Mean: guideSetStat['LJMean'],
                    StdDev: guideSetStat['LJStdDev']
                };
            }, this);
        }, this);
    },

    setLJSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        const val = row['value'];
        if (LABKEY.vis.isValid(val)) {
            if (dataObject.min == null || val < dataObject.min) {
                dataObject.min = val;
            }
            if (dataObject.max == null || val > dataObject.max) {
                dataObject.max = val;
            }

            if (this.yAxisScale === 'log' && val <= 0) {
                dataObject.showLogInvalid = true;
            }

            var mean = row['mean'];
            var sd = LABKEY.vis.isValid(row['stdDev']) ? row['stdDev'] : 0;

            // Issue 28462: don't include the +/-3 stddev error bars in min/max calculation when it isn't being plotted
            if (!this.singlePlot && LABKEY.vis.isValid(mean)) {
                var minSd = (mean - (3 * sd));
                if (dataObject.showLogInvalid === undefined && this.yAxisScale === 'log' && minSd <= 0) {
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
            var val1 = row['value_' + this.metric],
                    val2 = row['value_' + this.metric2];
            if (dataObject.showLogInvalid === undefined && this.yAxisScale === 'log') {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0)) {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },

    getLJPlotTypeProperties: function(precursorInfo, metricProps) {
        var plotProperties = {};
        // some properties are specific to whether we are showing multiple y-axis series
        if (this.isMultiSeries()) {
            plotProperties['value'] = 'value_' + this.metric;
            plotProperties['valueRight'] = 'value_' + this.metric2;
        }
        else {
            plotProperties['value'] = 'value';
            plotProperties['mean'] = 'mean';
            plotProperties['stdDev'] = 'stdDev';
            plotProperties['yAxisDomain'] = [precursorInfo.min, precursorInfo.max];
        }

        plotProperties['lowerBound'] = metricProps[this.metric].lowerBound;
        plotProperties['upperBound'] = metricProps[this.metric].upperBound;
        if (metricProps[this.metric].metricStatus === LABKEY.targetedms.MetricStatus.ValueCutoff) {
            plotProperties['boundType'] = LABKEY.vis.PlotProperties.BoundType.Absolute;
        }
        else if (metricProps[this.metric].metricStatus === LABKEY.targetedms.MetricStatus.MeanDeviationCutoff) {
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

    processLJPlotDataRow: function(row, fragment, metricId, metricProps)
    {
        const data = {};
        // if a guideSetId is defined for this row, include the guide set stats values in the data object
        if (Ext4.isDefined(row['GuideSetId'])) {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment]&& gs.Series[fragment][metricId]) {
                data['mean'] = gs.Series[fragment][metricId]['Mean'];
                data['stdDev'] = gs.Series[fragment][metricId]['StdDev'];
            }
        }

        if (this.isMultiSeries()) {
            data['value_' + metricId] = row['Value'];
            data['value_' + metricId + 'Title'] = metricProps['name'];
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
    },

    getLJCombinedPlotLegendSeries: function()
    {
        const result = ['value_' + this.metric];
        if (this.isMultiSeries()) {
            result.push('value_' + this.metric2);
        }
        return result;
    },

    getLJLegend: function () {
        const ljLegend = [];

        if (!this.metric2) {
            let metricInfo = this.getMetricPropsById(this.metric);

            if (metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.ValueCutoff) {
                const isYAxisScaleLinearOrLog = this.yAxisScale === 'linear' || this.yAxisScale === 'log';
                if (isYAxisScaleLinearOrLog) {
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
            } else {
                let upper = Number.isFinite(metricInfo.upperBound) ? metricInfo.upperBound : 3;
                let lower = Number.isFinite(metricInfo.lowerBound) ? metricInfo.lowerBound : -3;
                if ((metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.LeveyJennings ||
                                metricInfo.metricStatus === LABKEY.targetedms.MetricStatus.PlotOnly) &&
                        (this.yAxisScale === 'standardDeviation' || !this.singlePlot)) {

                    if (lower === upper * -1) {
                        ljLegend.push({
                            text: '+/- ' + upper + ' Std Dev',
                            color: 'red',
                            shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                        });
                    } else {
                        ljLegend.push({
                            text: (upper > 0 ? '+' : '') + upper + '/' + (lower > 0 ? '+' : '') + lower + ' Std Dev',
                            color: 'red',
                            shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                        });
                    }
                }

                if (!this.singlePlot) {
                    if (ljLegend.length === 0) {
                        ljLegend.push({
                            text: 'Outlier bounds',
                            color: 'red',
                            shape: LABKEY.vis.TrendingLineShape.stdDevLJ
                        });
                    }

                    ljLegend.push({
                        text: 'Mean',
                        color: 'darkgrey',
                        shape: LABKEY.vis.TrendingLineShape.meanLJ
                    });
                }
            }
        }

        if (ljLegend.length > 0) {
            ljLegend.splice(0, 0, {
                text: '',
                separator: true
            });
        }

        return ljLegend;
    }

});