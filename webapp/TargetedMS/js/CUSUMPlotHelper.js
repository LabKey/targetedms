Ext4.define("LABKEY.targetedms.CUSUMPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    setCUSUMSeriesMinMax: function(dataObject, row, isCUSUMmean) {
        dataObject.showLogInvalid = true; //CUSUM- is always negative

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
        var plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            if (isMean)
            {
                plotProperties['positiveValue'] = 'CUSUMmP_series1';
                plotProperties['positiveValueRight'] = 'CUSUMmP_series2';
                plotProperties['negativeValue'] = 'CUSUMmN_series1';
                plotProperties['negativeValueRight'] = 'CUSUMmN_series2';

            }
            else
            {
                plotProperties['positiveValue'] = 'CUSUMvP_series1';
                plotProperties['positiveValueRight'] = 'CUSUMvP_series2';
                plotProperties['negativeValue'] = 'CUSUMvN_series1';
                plotProperties['negativeValueRight'] = 'CUSUMvN_series2';

            }

        }
        else
        {
            var lower, upper;
            if (isMean)
            {
                plotProperties['positiveValue'] = 'CUSUMmP';
                plotProperties['negativeValue'] = 'CUSUMmN';
                lower = Math.min(-1 * LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT - 1, precursorInfo.minCUSUMmP, precursorInfo.minCUSUMmN);
                upper = Math.max(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT + 1, precursorInfo.maxCUSUMmP, precursorInfo.maxCUSUMmN);
            }
            else
            {
                plotProperties['positiveValue'] = 'CUSUMvP';
                plotProperties['negativeValue'] = 'CUSUMvN';
                lower = Math.min(-1 * LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT - 1, precursorInfo.minCUSUMvP, precursorInfo.minCUSUMvN);
                upper = Math.max(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT + 1, precursorInfo.maxCUSUMvP, precursorInfo.maxCUSUMvN);
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

    processCUSUMPlotDataRow: function(row, fragment, seriesType, metricProps, isMeanCUSUM)
    {
        var data = {};

        if (isMeanCUSUM)
        {
            if (this.isMultiSeries())
            {
                data['CUSUMmN_' + seriesType] = row['CUSUMmN'];
                data['CUSUMmN_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
                data['CUSUMmP_' + seriesType] = row['CUSUMmP'];
                data['CUSUMmP_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
            }
            else
            {
                data['CUSUMmN'] = row['CUSUMmN'];
                data['CUSUMmP'] = row['CUSUMmP'];
            }
        }
        else
        {
            if (this.isMultiSeries())
            {
                data['CUSUMvP_' + seriesType] = row['CUSUMvP'];
                data['CUSUMvP_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
                data['CUSUMvN_' + seriesType] = row['CUSUMvN'];
                data['CUSUMvN_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
            }
            else
            {
                data['CUSUMvP'] = row['CUSUMvP'];
                data['CUSUMvN'] = row['CUSUMvN'];
            }
        }
        return data;
    }

});