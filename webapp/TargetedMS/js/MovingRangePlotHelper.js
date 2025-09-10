/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.MovingRangePlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'Moving Range' : 'Moving range plots show the variation for individual observations ' +
            'by using the sequential differences between two successive values as a measure of dispersion.'
        }
    },
    setMovingRangeSeriesMinMax: function(dataObject, row) {
        var val = row['MR'];
        if (LABKEY.vis.isValid(val))
        {
            if (dataObject.minMR == null || val < dataObject.minMR) {
                dataObject.minMR = val;
            }
            if (dataObject.maxMR == null || val > dataObject.maxMR) {
                dataObject.maxMR = val;
            }

            if (this.yAxisScale === 'log' && val <= 0)
            {
                dataObject.showLogEpsilonWarning = true;
            }

            var mean = row['meanMR'];

            // Issue 28462: don't include the mean in the calculation of the max when plotting all on one plot
            if (!this.singlePlot && LABKEY.vis.isValid(mean))
            {
                if (dataObject.maxMRMean == null || mean > dataObject.maxMRMean) {
                    dataObject.maxMRMean = mean;
                }
            }
        }
        else if (this.isMultiSeries())
        {
            // check if either of the y-axis metric values are invalid for a log scale
            var val1 = row['valueMR_' + this.metric],
                    val2 = row['valueMR_' + this.metric2];
            if (dataObject.showLogInvalid === undefined && this.yAxisScale === 'log')
            {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0))
                {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },
    getMovingRangePlotTypeProperties: function(precursorInfo)
    {
        var plotProperties = {};
        // some properties are specific to whether we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            plotProperties['valueMR'] = 'MR_' + this.metric;
            plotProperties['valueRightMR'] = 'MR_' + this.metric2;
        }
        else
        {
            plotProperties['valueMR'] = 'MR';
            plotProperties['meanMR'] = 'meanMR';
            var lower = Math.min(LABKEY.vis.Stat.MOVING_RANGE_LOWER_LIMIT, precursorInfo.minMR);
            var upper = Math.max(precursorInfo.maxMRMean * LABKEY.vis.Stat.MOVING_RANGE_UPPER_LIMIT_WEIGHT, precursorInfo.maxMR);
            plotProperties['yAxisDomain'] = [lower, upper];
        }
        return plotProperties;
    },
    getMRInitFragmentPlotData: function()
    {
        return {
            minMR: null,
            maxMR: null,
            maxMRMean: null
        }
    },
    processMRPlotDataRow: function(row, fragment, metricId, metricProps)
    {
        const data = {};
        // if a guideSetId is defined for this row, include the guide set stats values in the data object
        if (Ext4.isDefined(row['GuideSetId']))
        {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment] && gs.Series[fragment][metricId])
            {
                data['meanMR'] = gs.Series[fragment][metricId]['MeanMR'];
                data['stddevMR'] = gs.Series[fragment][metricId]['StdDevMR'];
            }
        }

        if (this.isMultiSeries())
        {
            data['MR_' + metricId] = this.formatValue(row['MR']);
            data['MR_' + metricId + 'Title'] = metricProps['name'];
        }
        else
        {
            data['MR'] = this.formatValue(row['MR']);
        }

        data.MRShape = (row.IgnoreInQC ? 'Exclude' : 'Include') + (row.MROutlier ? '-Outlier' : '');

        return data;
    },

    processMRCombinedMinMax: function(combinePlotData, precursorInfo)
    {
        if (combinePlotData.minMR == null || combinePlotData.minMR > precursorInfo.minMR)
        {
            combinePlotData.minMR = precursorInfo.minMR;
        }
        if (combinePlotData.maxMR == null || combinePlotData.maxMR < precursorInfo.maxMR)
        {
            combinePlotData.maxMR = precursorInfo.maxMR;
        }
    },

    getMRCombinedPlotLegendSeries: function()
    {
        return ['MR_' + this.metric, 'MR_' + this.metric2];
    },

    getMRLegend: function () {
        var mrLegend = [];

        // Not showing limits for standard deviation
        if (this.yAxisScale !== 'standardDeviation' && !this.singlePlot && !this.metric2) {
            mrLegend.push({
                text: 'Moving Range',
                separator: true
            });
            mrLegend.push({
                text: 'Upper/Lower Limit',
                color: 'red',
                shape: LABKEY.vis.TrendingLineShape.limitMR
            });
        }
        return mrLegend;
    }
});