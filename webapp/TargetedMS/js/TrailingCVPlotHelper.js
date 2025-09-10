/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.TrailingCVPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'Trailing CV' : 'A Trailing Coefficient of Variation plot shows the moving average of percent coefficient of variation for the previous N runs, as defined by the user.  It is useful for finding long-term trends otherwise disguised by fluctuations caused by outliers.'
        }
    },
    processTrailingCVPlotDataRow: function(row, fragment, metricId, metricProps) {
        const data = {};
        if (Ext4.isDefined(row['GuideSetId']))
        {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment] && gs.Series[fragment][metricId])
            {
                data['meanTrailingCV'] = gs.Series[fragment][metricId]['MeanTrailingCV'];
                data['stddevTrailingCV'] = gs.Series[fragment][metricId]['StdDevTrailingCV'];
            }
        }

        if (this.isMultiSeries())
        {
            data['TrailingCV_' + metricId] = row['TrailingCV'];
            data['TrailingCV_' + metricId + 'Title'] = metricProps['name'];
        }
        else
        {
            data['TrailingCV'] = row['TrailingCV'];
        }
        return data;

    },

    getTrailingCVCombinedPlotLegendSeries: function()
    {
        return ['TrailingCV_' + this.metric, 'TrailingCV_' + this.metric2];
    },

    getTrailingCVPlotTypeProperties: function(precursorInfo) {
        let plotProperties = {};
        // some properties are specific to whether we are showing multiple y-axis series
        if (this.isMultiSeries()) {
            plotProperties['TrailingCV'] = 'TrailingCV_' + this.metric;
            plotProperties['TrailingCVRight'] = 'TrailingCV_' + this.metric2;
        }
        else {
            plotProperties['TrailingCV'] = 'TrailingCV';
            let min = Math.min(...precursorInfo.data.map(function(object) {
                return object.TrailingCV;
            }));
            let max = Math.max(...precursorInfo.data.map(function(object) {
                return object.TrailingCV;
            }));

            // Since 20 is a common limit for acceptable %CV, use that as the default range
            if (min < 20 && max < 20) {
                min = 0;
                max = 20;
            }

            // expand range if we have values bigger than 20
            if (min > 20 || max > 20) {
                min = 0;
                max = Math.round(Math.ceil(max / 10) * 10);
            }

            plotProperties['yAxisDomain'] = [min, max];
        }
        return plotProperties;
    },

    setTrailingCVMinMax: function (dataObject, row) {
        // track the min and max data, so we can get the range for including the QC annotations
        let val = row['TrailingCV' + (this.isMultiSeries() ? ('_' + row.MetricId) : '')];
        if (LABKEY.vis.isValid(val)) {
            if (dataObject.TrailingCVMin == null || val < dataObject.TrailingCVMin) {
                dataObject.TrailingCVMin = val;
            }
            if (dataObject.TrailingCVMax == null || val > dataObject.TrailingCVMax) {
                dataObject.TrailingCVMax = val;
            }

            if (this.yAxisScale === 'log' && val <= 0) {
                dataObject.showLogInvalid = true;
            }

        }
        else if (this.isMultiSeries()) {
            // check if either of the y-axis metric values are invalid for a log scale
            let val1 = row['TrailingCV_' + this.metric],
                    val2 = row['TrailingCV_' + this.metric2];
            if (dataObject.showLogInvalid === undefined && this.yAxisScale === 'log') {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0)) {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },

    getTrailingCVInitFragmentPlotData: function() {
        return {
            TrailingCVMin: null,
            TrailingCVMax: null
        }
    },
});