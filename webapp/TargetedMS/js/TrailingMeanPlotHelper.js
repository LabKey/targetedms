/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.TrailingMeanPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'Trailing Mean' : 'A Trailing Mean plot shows the moving average of the previous N runs, as defined by the user.  It is useful for finding long-term trends otherwise disguised by fluctuations caused by outliers.'
        }
    },

    processTrailingMeanPlotDataRow: function(row, fragment, metricId, metricProps) {
        const data = {};

        if (Ext4.isDefined(row['GuideSetId']))
        {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment] && gs.Series[fragment][metricId])
            {
                data['meanTrailingMean'] = gs.Series[fragment][metricId]['MeanTrailingMean'];
                data['stddevTrailingMean'] = gs.Series[fragment][metricId]['StdDevTrailingMean'];
            }
        }

        if (this.isMultiSeries()) {
            data['TrailingMean_' + metricId] = row['TrailingMean'];
            data['TrailingMean_' + metricId + 'Title'] = metricProps['name'];
        }
        else {
            data['TrailingMean'] = row['TrailingMean'];
        }
        return data;
    },

    getTrailingMeanPlotTypeProperties: function(precursorInfo) {
        let plotProperties = {};
        // some properties are specific to whether we are showing multiple y-axis series
        if (this.isMultiSeries()) {
            plotProperties['TrailingMean'] = 'TrailingMean_' + this.metric;
            plotProperties['TrailingMeanRight'] = 'TrailingMean_' + this.metric2;
        }
        else {
            plotProperties['TrailingMean'] = 'TrailingMean';
        }

        if (this.yAxisScale !== LABKEY.vis.PlotProperties.ValueConversion.PercentDeviation && this.yAxisScale !== LABKEY.vis.PlotProperties.ValueConversion.StandardDeviation) {
            let min = Math.min(...precursorInfo.data.map(function (object) {
                return object.TrailingMean;
            }));
            let max = Math.max(...precursorInfo.data.map(function (object) {
                return object.TrailingMean;
            }));

            plotProperties['yAxisDomain'] = [min, max];
        }

        return plotProperties;
    },

    getTrailingMeanCombinedPlotLegendSeries: function()
    {
        return ['TrailingMean_' + this.metric, 'TrailingMean_' + this.metric2];
    },

    setTrailingMeanMinMax: function (dataObject, row) {
        // track the min and max data, so we can get the range for including the QC annotations
        let val = row['TrailingMean' + (this.isMultiSeries() ? ('_' + row.MetricId) : '')];
        if (LABKEY.vis.isValid(val)) {
            if (dataObject.minTrailingMean == null || val < dataObject.minTrailingMean) {
                dataObject.minTrailingMean = val;
            }
            if (dataObject.maxTrailingMean == null || val > dataObject.maxTrailingMean) {
                dataObject.maxTrailingMean = val;
            }

            if (this.yAxisScale === 'log' && val <= 0) {
                dataObject.showLogInvalid = true;
            }
        }
        else if (this.isMultiSeries()) {
            // check if either of the y-axis metric values are invalid for a log scale
            let val1 = row['TrailingMean_' + this.metric],
                    val2 = row['TrailingMean_' + this.metric2];
            if (dataObject.showLogInvalid === undefined && this.yAxisScale === 'log') {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0)) {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },

    getTrailingMeanInitFragmentPlotData: function() {
        return {
            minTrailingMean: null,
            maxTrailingMean: null
        }
    },
});