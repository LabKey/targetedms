/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.QCPlotHelperWrapper", {
    mixins: {
        leveyJennings: 'LABKEY.targetedms.LeveyJenningsPlotHelper',
        cusum: 'LABKEY.targetedms.CUSUMPlotHelper',
        movingRange: 'LABKEY.targetedms.MovingRangePlotHelper',
        trailingMean: 'LABKEY.targetedms.TrailingMeanPlotHelper',
        trailingCV: 'LABKEY.targetedms.TrailingCVPlotHelper'
    },

    statics: {
        getQCPlotTypeLabel: function(visPlotType, isCUSUMMean) {
            if (visPlotType === LABKEY.vis.TrendingLinePlotType.MovingRange)
                return 'Moving Range';
            else if (visPlotType === LABKEY.vis.TrendingLinePlotType.CUSUM) {
                if (isCUSUMMean)
                    return 'CUSUMm';
                else
                    return 'CUSUMv';
            }
            else if (visPlotType === LABKEY.vis.TrendingLinePlotType.TrailingMean) {
                return 'Trailing Mean';
            }
            else if (visPlotType === LABKEY.vis.TrendingLinePlotType.TrailingCV) {
                return 'Trailing CV';
            }
            else
                return 'Levey-Jennings';
        }
    },

    deepCloneArray: function (arr) {
        return arr.map(function(item) {
            // Check if the item is an array
            if (Array.isArray(item)) {
                // If it is, call deepCloneArray on it
                return this.deepCloneArray(item);
            } else if (typeof item === 'object' && item !== null) {
                // If it's an object, use deepCloneObject
                return this.deepCloneObject(item);
            } else {
                // Otherwise, it's a primitive value and we can just return it
                return item;
            }
        }, this);
    },

    deepCloneObject: function (obj) {
        // Check if the object is an array
        if (Array.isArray(obj)) {
            // If it is, call deepCloneArray (which we defined earlier) to deep clone it
            return this.deepCloneArray(obj);
        } else if (typeof obj === 'object' && obj !== null) {
            // Create an empty object to hold the cloned properties
            const clone = {};

            // Iterate over the object's own properties
            for (const key in obj) {
                if (obj.hasOwnProperty(key)) {
                    // If the value is an object or an array, call deepCloneObject recursively to deep clone it
                    if (typeof obj[key] === 'object' && obj[key] !== null) {
                        clone[key] = this.deepCloneObject(obj[key]);
                    } else {
                        // Otherwise, just copy the value directly
                        clone[key] = obj[key];
                    }
                }
            }

            return clone;
        } else {
            // If the object is not an object or an array, return it as is
            return obj;
        }
    },

    zoomDateRangeForTrailingGraphs: function (precursorInfo) {
        let precursors = this.deepCloneObject(precursorInfo);
        if (Object.keys(this.guideSetDataMap).length !== 0) {
            let firstGuideSetStartDate = new Date();
            // setting to tomorrow's date
            firstGuideSetStartDate.setDate(firstGuideSetStartDate.getDate() + 1);
            Ext4.Object.each(this.guideSetDataMap, function (id, gs) {
                if (new Date(gs.TrainingStart).getTime() < firstGuideSetStartDate.getTime()) {
                    firstGuideSetStartDate = new Date(gs.TrainingStart);
                }
            });

            for (let ind = 0 ; ind < precursors.data.length; ind++) {
                let data = precursors.data[ind];
                if (new Date(data.fullDate).getTime() < firstGuideSetStartDate.getTime()) {
                    precursors.data.splice(ind, 1);
                    ind--;
                }
            }
        }
        else {
            for (let ind = 0 ; ind < precursors.data.length; ind++) {
                let data = precursors.data[ind];
                if (!data.TrailingMean && !data.TrailingCV) {
                    precursors.data.splice(ind, 1);
                    ind--;
                }
            }
        }
        return precursors;
    },

    addIndividualPrecursorPlots : function() {
        var addedPlot = false,
                metricProps = this.getMetricPropsById(this.metric),
                me = this; // for plot brushing

        this.longestLegendText = 0;

        for (var i = 0; i < this.precursors.length; i++) {
            const precursorInfo = this.fragmentPlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo) {
                addedPlot = true;

                var id = this.plotDivId + "-precursorPlot" + i;
                var ids = [id];
                for (var j = 1; j < this.plotTypes.length; j++) {
                    ids.push(id + '_plotType_' + j);
                }

                this.addPlotsToPlotDiv(ids, this.precursors[i], this.plotDivId, 'qc-plot-wp');

                var plotIndex = 0;
                // add a new panel for each plot so we can add the title to the frame
                if (this.showLJPlot()) {
                    this.addEachIndividualPrecursorPlot(plotIndex, ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.LeveyJennings, undefined, me);
                }
                if (this.showMovingRangePlot()) {
                    this.addEachIndividualPrecursorPlot(plotIndex, ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.MovingRange, undefined, me);
                }
                if (this.showMeanCUSUMPlot()) {
                    this.addEachIndividualPrecursorPlot(plotIndex, ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, true, me);
                }
                if (this.showVariableCUSUMPlot()) {
                    this.addEachIndividualPrecursorPlot(plotIndex, ids[plotIndex], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, false, me);
                }
                if (this.showTrailingMeanPlot()) {
                    this.addEachIndividualPrecursorPlot(plotIndex, ids[plotIndex++], i, this.zoomDateRangeForTrailingGraphs(precursorInfo), metricProps, LABKEY.vis.TrendingLinePlotType.TrailingMean, undefined, me);
                }
                if (this.showTrailingCVPlot()) {
                    this.addEachIndividualPrecursorPlot(plotIndex, ids[plotIndex++], i, this.zoomDateRangeForTrailingGraphs(precursorInfo), metricProps, LABKEY.vis.TrendingLinePlotType.TrailingCV, undefined, me);
                }
            }
        }

        this.setPlotBrushingDisplayStyle();

        return addedPlot;
    },

    addCombinedPeptideSinglePlot : function() {
        var metricProps = this.getMetricPropsById(this.metric),
                yAxisCount = this.isMultiSeries() ? 2 : 1, //Will only have a right if there is already a left y-axis
                groupColors = this.getColorRange(),
                combinePlotData = this.getCombinedPlotInitData(),
                lengthOfLongestLegend = 8,  // At least length of label 'Peptides'
                lengthOfLongestAnnot = 1,
                showLogInvalid = false,
                precursorInfo,
                prefix, ellipCount, prefLength, ellipMatch = new RegExp(this.legendHelper.ellipsis, 'g');

        //Add series1 separator to Legend sections
        if (this.isMultiSeries()) {
            if (metricProps.series1Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series1Label.length;
        }

        // traverse the precursor list for: calculating the longest legend string and combine the plot data
        for (var i = 0; i < this.precursors.length; i++) {
            precursorInfo = this.fragmentPlotData[this.precursors[i]];
            // We may not have a match if it's been filtered out - see issue 38720
            if (precursorInfo) {
                prefix = this.legendHelper.getLegendItemText(precursorInfo);
                ellipCount = prefix.match(ellipMatch) ? prefix.match(ellipMatch).length : 0;
                prefLength = prefix.length + ellipCount;  // ellipsis count for two chars

                if (prefLength > lengthOfLongestLegend) {
                    lengthOfLongestLegend = prefLength;
                }

                // for combined plot, concat all data together into a single array and track min/max for all
                combinePlotData.data = combinePlotData.data.concat(precursorInfo.data);
                this.processCombinedPlotMinMax(combinePlotData, precursorInfo);

                showLogInvalid = showLogInvalid || precursorInfo.showLogInvalid;
            }
        }

        // Annotations
        Ext4.each(this.legendData, function(entry) {
            if (entry.text.length > lengthOfLongestLegend) {
                lengthOfLongestAnnot = entry.text.length;
            }
        }, this);

        if (this.isMultiSeries()) {
            if (metricProps.series2Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series2Label.length;
        }
        var id = 'combinedPlot';
        var ids = [id];
        for (var i = 1; i < this.plotTypes.length; i++) {
            ids.push(id + 'plotType' + i.toString());
        }
        this.addPlotsToPlotDiv(ids, 'All Series', this.plotDivId, 'qc-plot-wp');
        var plotIndex = 0;
        var legendMargin = 9 * lengthOfLongestLegend;
        var annotMargin = 9 * lengthOfLongestAnnot;

        if( annotMargin > legendMargin) {
            legendMargin = annotMargin;  // Give some extra space if annotations defined
        }

        // Annotations can still push legend too far so cap this
        if (legendMargin > 300)
            legendMargin = 300;

        if (this.showLJPlot()) {
            this.addEachCombinedPrecursorPlot(plotIndex, ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.LeveyJennings);
        }
        if (this.showMovingRangePlot()) {
            this.addEachCombinedPrecursorPlot(plotIndex, ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.MovingRange);
        }
        if (this.showMeanCUSUMPlot()) {
            this.addEachCombinedPrecursorPlot(plotIndex, ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.CUSUM, true);
        }
        if (this.showVariableCUSUMPlot()) {
            this.addEachCombinedPrecursorPlot(plotIndex, ids[plotIndex], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.CUSUM, false);
        }
        if (this.showTrailingMeanPlot()) {
            this.addEachCombinedPrecursorPlot(plotIndex, ids[plotIndex], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.TrailingMean, false);
        }
        if (this.showTrailingCVPlot()) {
            this.addEachCombinedPrecursorPlot(plotIndex, ids[plotIndex], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.TrailingCV, false);
        }


        return true;
    },

    setSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        if (this.showLJPlot)
            this.setLJSeriesMinMax(dataObject, row);
        if (this.showMovingRangePlot())
            this.setMovingRangeSeriesMinMax(dataObject, row);
        if (this.showMeanCUSUMPlot())
            this.setCUSUMSeriesMinMax(dataObject, row, true);
        if (this.showVariableCUSUMPlot())
            this.setCUSUMSeriesMinMax(dataObject, row, false);
        if (this.showTrailingMeanPlot())
            this.setTrailingMeanMinMax(dataObject, row);
        if (this.showTrailingCVPlot())
            this.setTrailingCVMinMax(dataObject, row);
    },

    getPlotTypeProperties: function(precursorInfo, plotType, isMean) {
        if (plotType === LABKEY.vis.TrendingLinePlotType.MovingRange)
            return this.getMovingRangePlotTypeProperties(precursorInfo);
        else if (plotType === LABKEY.vis.TrendingLinePlotType.CUSUM)
            return this.getCUSUMPlotTypeProperties(precursorInfo, isMean);
        else if (plotType === LABKEY.vis.TrendingLinePlotType.TrailingMean)
            return this.getTrailingMeanPlotTypeProperties(precursorInfo);
        else if (plotType === LABKEY.vis.TrendingLinePlotType.TrailingCV)
            return this.getTrailingCVPlotTypeProperties(precursorInfo);
        else
            return this.getLJPlotTypeProperties(precursorInfo);
    },

    getInitFragmentPlotData: function(fragment, dataType, mz, color)
    {
        var fragmentData = {
            fragment: fragment,
            dataType: dataType,
            data: [],
            mz: mz,
            color: color
        };

        Ext4.apply(fragmentData, this.getInitPlotMinMaxData());

        return fragmentData;
    },

    getInitPlotMinMaxData: function() {
        var plotData = {};
        if (this.showLJPlot()) {
            Ext4.apply(plotData, this.getLJInitFragmentPlotData());
        }
        if (this.showMovingRangePlot()) {
            Ext4.apply(plotData, this.getMRInitFragmentPlotData());
        }
        if (this.showMeanCUSUMPlot()) {
            Ext4.apply(plotData, this.getCUSUMInitFragmentPlotData(true));
        }
        if (this.showVariableCUSUMPlot()) {
            Ext4.apply(plotData, this.getCUSUMInitFragmentPlotData(false));
        }
        if (this.showTrailingMeanPlot()) {
            Ext4.apply(plotData, this.getTrailingMeanInitFragmentPlotData());
        }
        if (this.showTrailingCVPlot()) {
            Ext4.apply(plotData, this.getTrailingCVInitFragmentPlotData());
        }

        return plotData;
    },

    processPlotDataRow: function(row, plotDataRow, fragment, metricProps) {
        var dataType = plotDataRow['DataType'];
        var mz = Ext4.util.Format.number(plotDataRow['mz'], '0.0000');
        var color = plotDataRow['SeriesColor'];
        if (!this.fragmentPlotData[fragment])
        {
            this.fragmentPlotData[fragment] = this.getInitFragmentPlotData(fragment, dataType, mz, color);
        }

        var seriesType = row['SeriesType'] === 2 ? 'series2' : 'series1';

        var data = {
            type: 'data',
            fragment: fragment,
            mz: mz,
            SampleFileId: row['SampleFileId'], // keep in data for click handler
            ReplicateId: row['ReplicateId'], // keep in data for click handler
            ReplicateName: row['ReplicateName'], // keep in data for click handler
            PrecursorId: row['PrecursorId'], // keep in data for click handler
            PrecursorChromInfoId: row['PrecursorChromInfoId'], // keep in data for click handler
            FilePath: row['FilePath'], // keep in data for hover text display
            IgnoreInQC: row['IgnoreInQC'], // keep in data for hover text display
            fullDate: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime']), true) : null,
            date: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
            groupedXTick: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
            dataType: dataType, //needed for plot point click handler
            SeriesType: seriesType
        };

        // if a guideSetId is defined for this row, include the guide set stats values in the data object
        if (Ext4.isDefined(row['GuideSetId']) && row['GuideSetId'] > 0) {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment]) {
                data['guideSetId'] = row['GuideSetId'];
                data['inGuideSetTrainingRange'] = row['InGuideSetTrainingRange'];
                data['groupedXTick'] = data['groupedXTick'] + '|'
                        + (gs['TrainingStart'] ? gs['TrainingStart'] : '0') + '|'
                        + (row['InGuideSetTrainingRange'] ? 'include' : 'notinclude');
            }
        }

        if (this.showLJPlot()) {
            Ext4.apply(data, this.processLJPlotDataRow(row, fragment, seriesType, metricProps));
        }
        if (this.showMovingRangePlot()) {
            Ext4.apply(data, this.processMRPlotDataRow(row, fragment, seriesType, metricProps));
        }
        if (this.showMeanCUSUMPlot()) {
            Ext4.apply(data, this.processCUSUMPlotDataRow(row, fragment, seriesType, metricProps, true));
        }
        if (this.showVariableCUSUMPlot()) {
            Ext4.apply(data, this.processCUSUMPlotDataRow(row, fragment, seriesType, metricProps, false));
        }
        if (this.showTrailingMeanPlot()) {
            Ext4.apply(data, this.processTrailingMeanPlotDataRow(row, fragment, seriesType, metricProps));
        }
        if (this.showTrailingCVPlot()) {
            Ext4.apply(data, this.processTrailingCVPlotDataRow(row, fragment, seriesType, metricProps));
        }
        if (this.showTrailingMeanPlot() || this.showTrailingCVPlot()) {
            data['TrailingStartDate'] = row['TrailingStartDate'];
            data['TrailingEndDate'] = row['TrailingEndDate'];
        }

        return data;
    },

    getCombinedPlotInitData: function()
    {
        var combinePlotData = {data: []};
        Ext4.apply(combinePlotData, this.getInitPlotMinMaxData());
        return combinePlotData
    },

    processCombinedPlotMinMax: function(combinePlotData, precursorInfo)
    {
        if (this.showLJPlot())
        {
            this.processLJCombinedMinMax(combinePlotData, precursorInfo);
        }
        if (this.showMovingRangePlot())
        {
            this.processMRCombinedMinMax(combinePlotData, precursorInfo);
        }
        if (this.showMeanCUSUMPlot())
        {
            this.processCUSUMCombinedMinMax(combinePlotData, precursorInfo, true);
        }
        if (this.showVariableCUSUMPlot())
        {
            this.processCUSUMCombinedMinMax(combinePlotData, precursorInfo, false);
        }
    },

    getCombinedPlotLegendSeries: function(plotType, isCUSUMMean)
    {
        if (plotType == LABKEY.vis.TrendingLinePlotType.MovingRange)
            return this.getMRCombinedPlotLegendSeries();
        else if (plotType == LABKEY.vis.TrendingLinePlotType.CUSUM)
            return this.getCUSUMCombinedPlotLegendSeries(isCUSUMMean);
        else
            return this.getLJCombinedPlotLegendSeries();
    },

    getAdditionalPlotLegend: function(plotType) {
        if (plotType === LABKEY.vis.TrendingLinePlotType.CUSUM)
            return this.getCUSUMGroupLegend();
        if (plotType === LABKEY.vis.TrendingLinePlotType.MovingRange)
            return this.getMRLegend();
        if (plotType === LABKEY.vis.TrendingLinePlotType.LeveyJennings)
            return this.getLJLegend();
        if (this.showMeanCUSUMPlot() || this.showVariableCUSUMPlot() ||
                plotType === LABKEY.vis.TrendingLinePlotType.TrailingMean ||
                plotType === LABKEY.vis.TrendingLinePlotType.TrailingCV)
            return this.getEmptyLegend();
        return [];
    },
});