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
    processTrailingCVPlotDataRow: function(row, fragment, seriesType, metricProps) {
        let data = {};

        if (this.isMultiSeries())
        {
            data['TrailingCV_' + seriesType] = row['TrailingCV'];
            data['TrailingCV_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
        }
        else
        {
            data['TrailingCV'] = row['TrailingCV'];
        }
        return data;

    },

    getTrailingCVPlotTypeProperties: function(precursorInfo) {
        let plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries()) {
            plotProperties['TrailingCV'] = 'value_series1';
            plotProperties['TrailingCVRight'] = 'value_series2';
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
        let val = row['TrailingCV'];
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
            let val1 = row['TrailingCV_series1'],
                    val2 = row['TrailingCV_series2'];
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