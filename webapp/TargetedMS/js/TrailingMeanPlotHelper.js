/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.TrailingMeanPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'Trailing Mean' : 'Blah '
        }
    },

    processTrailingMeanPlotDataRow: function(row, fragment, seriesType, metricProps) {
        let data = {};

        if (this.isMultiSeries()) {
            data['TrailingMean_' + seriesType] = row['TrailingMean'];
            data['TrailingMean_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
        }
        else {
            data['TrailingMean'] = row['TrailingMean'];
        }
        return data;

    },

    getTrailingMeanPlotTypeProperties: function(precursorInfo) {
        let plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries()) {
            plotProperties['TrailingMean'] = 'TrailingMean_series1';
            plotProperties['TrailingMeanRight'] = 'TrailingMean_series2';
        }
        else {
            plotProperties['TrailingMean'] = 'TrailingMean';
            plotProperties['yAxisDomain'] = [precursorInfo.min, precursorInfo.max];
        }
        return plotProperties;
    },

    setTrailingMeanMinMax: function (dataObject, row) {
        // track the min and max data, so we can get the range for including the QC annotations
        let val = row['TrailingMean'];
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

        }
        else if (this.isMultiSeries()) {
            // check if either of the y-axis metric values are invalid for a log scale
            let val1 = row['TrailingMean_series1'],
                    val2 = row['TrailingMean_series2'];
            if (dataObject.showLogInvalid === undefined && this.yAxisScale === 'log') {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0)) {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },

    getTrailingMeanInitFragmentPlotData: function() {
        return {
            min: null,
            max: null
        }
    },
});