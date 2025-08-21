/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.CUSUMPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'CUSUMm' : 'A CUSUM plot is a time-weighted control plot that displays the cumulative sums of the deviations of each sample value from the target value.' +
            ' CUSUMm (mean CUSUM) plots two types of CUSUM statistics: one for positive mean shifts and the other for negative mean shifts.',
            'CUSUMv' : 'A CUSUM plot is a time-weighted control plot that displays the cumulative sums of the deviations of each sample value from the target value. ' +
            'CUSUMv (variability or scale CUSUM) plots two types of CUSUM statistics: one for positive variability shifts and the other for negative variability shifts. ' +
            'Variability is a transformed standardized normal quantity which is sensitive to variability changes.'
        }
    },
    setCUSUMSeriesMinMax: function(dataObject, row, isCUSUMmean) {
        // track the min and max data so we can get the range for including the QC annotations
        var negative = 'CUSUMmN', positive = 'CUSUMmP';
        if (!isCUSUMmean)
        {
            negative = 'CUSUMvN'; positive = 'CUSUMvP';
        }
        var maxNegative = 'max' + negative, maxPositive = 'max' + positive, minNegative = 'min' + negative, minPositive = 'min' + positive;
        var valNegative = row[negative], valPositive = row[positive];
        if (LABKEY.vis.isValid(valNegative) && LABKEY.vis.isValid(valPositive))
        {
            if (this.yAxisScale === 'log' && (valNegative <= 0 || valPositive <= 0))
            {
                dataObject.showLogEpsilonWarning = true;
            }

            if (dataObject[minNegative] == null || valNegative < dataObject[minNegative]) {
                dataObject[minNegative] = valNegative;
            }
            if (dataObject[maxNegative] == null || valNegative > dataObject[maxNegative]) {
                dataObject[maxNegative] = valNegative;
            }

            if (dataObject[minPositive] == null || valPositive < dataObject[minPositive]) {
                dataObject[minPositive] = valPositive;
            }
            if (dataObject[maxPositive] == null || valPositive > dataObject[maxPositive]) {
                dataObject[maxPositive] = valPositive;
            }
        }
    },

    getCUSUMPlotTypeProperties: function(precursorInfo, isMean)
    {
        const plotProperties = {};
        // some properties are specific to whether we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            const prefix = isMean ? 'CUSUMm' : 'CUSUMv';
            plotProperties['positiveValue'] = prefix + 'P_' + this.metric;
            plotProperties['positiveValueRight'] = prefix + 'P_' + this.metric2;
            plotProperties['negativeValue'] = prefix + 'N_' + this.metric;
            plotProperties['negativeValueRight'] = prefix + 'N_' + this.metric2;
        }
        else
        {
            let lower, upper;
            if (isMean)
            {
                plotProperties['positiveValue'] = 'CUSUMmP';
                plotProperties['negativeValue'] = 'CUSUMmN';
                lower = Math.min(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT_LOWER, precursorInfo.minCUSUMmP, precursorInfo.minCUSUMmN);
                upper = Math.max(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT, precursorInfo.maxCUSUMmP, precursorInfo.maxCUSUMmN);
            }
            else
            {
                plotProperties['positiveValue'] = 'CUSUMvP';
                plotProperties['negativeValue'] = 'CUSUMvN';
                lower = Math.min(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT_LOWER, precursorInfo.minCUSUMvP, precursorInfo.minCUSUMvN);
                upper = Math.max(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT, precursorInfo.maxCUSUMvP, precursorInfo.maxCUSUMvN);
            }

            plotProperties['yAxisDomain'] = [lower, upper];

        }
        return plotProperties;
    },

    getCUSUMInitFragmentPlotData: function(isMeanCUSUM)
    {
        if (isMeanCUSUM)
        {
            return {
                minCUSUMmP: null,
                maxCUSUMmP: null,
                minCUSUMmN: null,
                maxCUSUMmN: null
            }
        }
        else {
            return {
                minCUSUMvP: null,
                maxCUSUMvP: null,
                minCUSUMvN: null,
                maxCUSUMvN: null
            }
        }
    },

    processCUSUMPlotDataRow: function(row, fragment, metricId, metricProps, isMeanCUSUM)
    {
        var data = {};

        if (isMeanCUSUM)
        {
            if (this.isMultiSeries())
            {
                data['CUSUMmN_' + metricId] = this.formatValue(row['CUSUMmN']);
                data['CUSUMmN_' + metricId + 'Title'] = metricProps['name'];
                data['CUSUMmP_' + metricId] = this.formatValue(row['CUSUMmP']);
                data['CUSUMmP_' + metricId + 'Title'] = metricProps['name'];
            }
            else
            {
                data['CUSUMmN'] = this.formatValue(row['CUSUMmN']);
                data['CUSUMmP'] = this.formatValue(row['CUSUMmP']);
            }
        }
        else
        {
            if (this.isMultiSeries())
            {
                data['CUSUMvP_' + metricId] = this.formatValue(row['CUSUMvP']);
                data['CUSUMvP_' + metricId + 'Title'] = metricProps['name'];
                data['CUSUMvN_' + metricId] = this.formatValue(row['CUSUMvN']);
                data['CUSUMvN_' + metricId + 'Title'] = metricProps['name'];
            }
            else
            {
                data['CUSUMvP'] = this.formatValue(row['CUSUMvP']);
                data['CUSUMvN'] = this.formatValue(row['CUSUMvN']);
            }
        }
        return data;
    },

    processCUSUMCombinedMinMax: function(combinePlotData, precursorInfo, isMeanCUSUM)
    {
        var negative = 'CUSUMmN', positive = 'CUSUMmP';
        if (!isMeanCUSUM)
        {
            negative = 'CUSUMvN'; positive = 'CUSUMvP';
        }
        var maxNegative = 'max' + negative, maxPositive = 'max' + positive, minNegative = 'min' + negative, minPositive = 'min' + positive;
        var valNegativeMin = precursorInfo[minNegative], valPositiveMin = precursorInfo[minPositive];
        var varNegativeMax = precursorInfo[maxNegative], valPositiveMax = precursorInfo[maxPositive];
        if (combinePlotData[minNegative] == null || valNegativeMin < combinePlotData[minNegative])
        {
            combinePlotData[minNegative] = valNegativeMin;
        }
        if (combinePlotData[maxNegative] == null || varNegativeMax > combinePlotData[maxNegative])
        {
            combinePlotData[maxNegative] = varNegativeMax;
        }

        if (combinePlotData[minPositive] == null || valPositiveMin < combinePlotData[minPositive])
        {
            combinePlotData[minPositive] = valPositiveMin;
        }
        if (combinePlotData[maxPositive] == null || valPositiveMax > combinePlotData[maxPositive])
        {
            combinePlotData[maxPositive] = valPositiveMax;
        }

        combinePlotData.fragment = precursorInfo.fragment;
    },

    getCUSUMCombinedPlotLegendSeries: function(isMeanCUSUM)
    {
        //positive or negative will use the same color, special casing done in plot.js
        //normalizedGroup = group.replace('CUSUMmN', 'CUSUMm').replace('CUSUMmP', 'CUSUMm');
        //normalizedGroup = group.replace('CUSUMvN', 'CUSUMv').replace('CUSUMvP', 'CUSUMv');
        if (isMeanCUSUM)
            return ['CUSUMm_' + this.metric, 'CUSUMm_' + this.metric2];
        return ['CUSUMv_' + this.metric, 'CUSUMv_' + this.metric2];
    },

    getCUSUMGroupLegend: function()
    {
        var cusumLegend = [];
        cusumLegend.push({
            text: 'CUSUM Group',
            separator: true
        });
        cusumLegend.push({
            text: 'CUSUM-',
            color: '#000000',
            shape: LABKEY.vis.TrendingLineShape.negativeCUSUM
        });
        cusumLegend.push({
            text: 'CUSUM+',
            color: '#000000',
            shape: LABKEY.vis.TrendingLineShape.positiveCUSUM
        });
        if (!this.metric2) {
            cusumLegend.push({
                text: 'Upper/Lower Limit',
                color: 'red',
                shape: LABKEY.vis.TrendingLineShape.limitMR
            });
        }
        return cusumLegend;
    }

});