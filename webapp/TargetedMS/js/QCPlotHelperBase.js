/*
 * Copyright (c) 2016-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.QCPlotHelperBase", {

    statics: {
        qcPlotTypes : ['Metric Value', 'Moving Range', 'CUSUMm', 'CUSUMv', 'Trailing CV', 'Trailing Mean'],
        maxPointsPerSeries : 300,
        shapeDomain: ['Include', 'Exclude', 'Include-Outlier', 'Exclude-Outlier'],
        // Separates fragment from series name in legend item names (e.g. "PEPTIDE|Left"). Fragments are peptide
        // sequences or molecule names and are not expected to contain this character.
        SERIES_NAME_SEP: '|'
    },

    showMetricValuePlot: function() {
        return this.isPlotTypeSelected('Metric Value');
    },

    showMovingRangePlot: function() {
        return this.isPlotTypeSelected('Moving Range');
    },

    showMeanCUSUMPlot: function() {
        return this.isPlotTypeSelected('CUSUMm');
    },

    showVariableCUSUMPlot: function() {
        return this.isPlotTypeSelected('CUSUMv');
    },

    isPlotTypeSelected: function(plotType) {
        return this.plotTypes.indexOf(plotType) > -1;
    },

    showTrailingMeanPlot: function() {
        return this.isPlotTypeSelected('Trailing Mean');
    },

    showTrailingCVPlot: function() {
        return this.isPlotTypeSelected('Trailing CV');
    },

    getGuideSetDataObj : function(row) {
        return {
            ReferenceEnd: row['ReferenceEnd'],
            TrainingEnd: row['TrainingEnd'],
            TrainingStart: row['TrainingStart'],
            Comment: row['Comment'],
            Series: {}
        };
    },

    processRawGuideSetData: function (plotDataRows) {
        if (!this.guideSetDataMap)
            this.guideSetDataMap = {};

        Ext4.each(plotDataRows, function (plotDataRow) {
            Ext4.each(plotDataRow.GuideSetStats, function (guideSetStat) {
                const guideSetId = guideSetStat['GuideSetId'];
                const metricId = guideSetStat['MetricId'];
                const seriesLabel = plotDataRow['SeriesLabel'];

                if (!this.guideSetDataMap[guideSetId]) {
                    this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(guideSetStat);
                }
                if (!this.guideSetDataMap[guideSetId].Series[seriesLabel]) {
                    this.guideSetDataMap[guideSetId].Series[seriesLabel] = {};
                }

                if (!this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId]) {
                    this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId] = {}
                }
                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId].MeanMR = guideSetStat['MeanMR'];
                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId].StdDevMR = guideSetStat['StdDevMR'];
                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId].MeanTrailingMean = guideSetStat['MeanTrailingMean'];
                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId].StdDevTrailingMean = guideSetStat['StdDevTrailingMean'];
                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId].MeanTrailingCV = guideSetStat['MeanTrailingCV'];
                this.guideSetDataMap[guideSetId].Series[seriesLabel][metricId].StdDevTrailingCV = guideSetStat['StdDevTrailingCV'];
            }, this);

        }, this);

    },

    getPlotsData: function() {
        // get input number N
        // pass includeTrailingCV or includeTrailingMean in plotsConfig
        const plotsConfig = {};
        plotsConfig.metricId = this.metric;
        plotsConfig.metricId2 = this.metric2;
        plotsConfig.includeLJ = this.showMetricValuePlot();
        plotsConfig.includeMR = this.showMovingRangePlot();
        plotsConfig.includeMeanCusum = this.showMeanCUSUMPlot();
        plotsConfig.includeVariableCusum = this.showVariableCUSUMPlot();
        plotsConfig.showExcluded = this.showExcluded;
        // show reference guide set for custom date range
        plotsConfig.showReferenceGS = this.showReferenceGS && this.dateRangeOffset !== 0;
        plotsConfig.showExcludedPrecursors = this.showExcludedPrecursors;
        plotsConfig.trailingRuns = this.trailingRuns;
        plotsConfig.includeTrailingMeanPlot = this.showTrailingMeanPlot();
        plotsConfig.includeTrailingCVPlot = this.showTrailingCVPlot();

        let urlParams = LABKEY.ActionURL.getParameters();
        if (parseInt(urlParams['replicateId']) > 0) {
            plotsConfig.replicateId = parseInt(urlParams['replicateId']);
        }

        const config = this.getReportConfig()

        if (this.selectedAnnotations) {
            plotsConfig.selectedAnnotations = [];
            Ext4.Object.each(this.selectedAnnotations, function (name, values) {
                plotsConfig.selectedAnnotations.push({
                    name: name,
                    values: values
                })
            }, this);
        }

        if (config) {
            plotsConfig.startDate = config.StartDate;
            plotsConfig.endDate = config.EndDate;
        }

        // Track and cancel in-flight request; ensure only latest response is processed
        this._qcRequestSeq = (this._qcRequestSeq || 0) + 1;
        const requestSeq = this._qcRequestSeq;

        // Abort any previous in-flight request if possible
        if (this._qcActiveRequest && typeof this._qcActiveRequest.abort === 'function') {
            try { this._qcActiveRequest.abort(); } catch (e) { /* no-op */ }
        }

        const failureCb = LABKEY.Utils.getCallbackWrapper(this.failureHandler, this);

        this._qcActiveRequest = LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCPlotsData.api'),
            success: function(response) {
                // Ignore if not the most recent request
                if (requestSeq !== this._qcRequestSeq)
                    return;

                try {
                    this.lastParsedResponse = JSON.parse(response.responseText);
                    this.processPlotData();
                }
                finally {
                    // Clear active request handle
                    if (requestSeq === this._qcRequestSeq)
                        this._qcActiveRequest = null;
                }
            },
            failure: function(response) {
                // Ignore failures from stale/aborted requests
                if (requestSeq !== this._qcRequestSeq)
                    return;

                try { failureCb(response); } finally {
                    if (requestSeq === this._qcRequestSeq)
                        this._qcActiveRequest = null;
                }
            },
            scope: this,
            jsonData: plotsConfig
        });
    },

    processPlotData: function() {
        var parsed = this.lastParsedResponse;
        if (!parsed)
            return;

        var plotDataRows = parsed.plotDataRows;
        const metricProps = {};
        for (let x = 0; x < parsed.metricProps.length; x++) {
            metricProps[parsed.metricProps[x].id] = parsed.metricProps[x];
        }
        var sampleFiles = parsed.sampleFiles;
        this.filterQCPoints = parsed.filterQCPoints;

        var allPlotDateValues = [];

        this.setPrecursorsForPage(plotDataRows);

        // process the data to shape it for the JS LeveyJenningsPlot API call
        this.fragmentPlotData = {};

        if (this.showMetricValuePlot()) {
            this.processLJGuideSetData(plotDataRows);
        }
        if (this.showMovingRangePlot() || this.showMeanCUSUMPlot() || this.showVariableCUSUMPlot() || this.showTrailingMeanPlot() || this.showTrailingCVPlot()) {
            this.processRawGuideSetData(plotDataRows);
        }

        let sampleFilesById = {};
        Ext4.iterate(sampleFiles, function (sampleFile) {
            sampleFilesById[sampleFile['SampleId']] = sampleFile;
        }, this);


        let tempData; // temp variable to store data for setting the date
        let foundTrue = false
        let trainingSeqIdx = 1; // this index is used for displaying the average number of runs in tooltip (QCPlotHoverPanel.js L110)
        for (let i = this.pagingStartIndex; i < this.pagingEndIndex; i++) {
            const plotDataRow = plotDataRows[i];
            tempData = plotDataRow;
            const fragment = plotDataRow.SeriesLabel;
            Ext4.iterate(plotDataRow.data, function (plotData) {

                // Flatten the sample file data into each row
                let sampleFile = sampleFilesById[plotData['SampleFileId']];
                plotData['FilePath'] = sampleFile['FilePath'];
                plotData['ReplicateId'] = sampleFile['ReplicateId'];
                plotData['AcquiredTime'] = sampleFile['AcquiredTime'];
                plotData['GuideSetId'] = sampleFile['GuideSetId'];
                plotData['ReplicateName'] = sampleFile['ReplicateName'];
                plotData['InGuideSetTrainingRange'] = sampleFile['InGuideSetTrainingRange'];

                const gs = this.guideSetDataMap[plotData['GuideSetId']];

                if (Ext4.isDefined(gs) && gs.Series[fragment]) {
                    if (plotData['InsideGuideSet']) {
                        if (!foundTrue) {
                            foundTrue = true;
                            trainingSeqIdx = 1;
                        }
                    } else {
                        foundTrue = false;
                    }
                    plotData['TrainingSeqIdx'] = trainingSeqIdx;
                    trainingSeqIdx++
                }
                var data = this.processPlotDataRow(plotData, plotDataRow, fragment, metricProps);
                this.fragmentPlotData[fragment].data.push(data);
                this.fragmentPlotData[fragment].precursorScoped = metricProps[data.MetricId].precursorScoped;
                this.setSeriesMinMax(this.fragmentPlotData[fragment], data);
                allPlotDateValues.push(data.fullDate);

            }, this);


        }

        // Issue 31678: get the full set of dates values from the precursor data and from the annotations
        for (var j = 0; j < this.annotationData.length; j++) {
            allPlotDateValues.push(this.formatDate(new Date(this.annotationData[j].Date), true));
        }
        allPlotDateValues = Ext4.Array.unique(allPlotDateValues).sort();

        this.legendHelper = LABKEY.targetedms.QCPlotLegendHelper;
        this.legendHelper.setupLegendPrefixes(this.fragmentPlotData, 3);

        // merge in the annotation data to make room on the y axis
        for (var i = 0; i < this.precursors.length; i++) {
            let frag = this.precursors[i];
            var precursorInfo = this.fragmentPlotData[frag];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo) {
                // if the min and max are the same, or very close, increase the range
                if (precursorInfo.max == null && precursorInfo.min == null) {
                    precursorInfo.max = 1;
                    precursorInfo.min = 0;
                }
                else if (precursorInfo.max - precursorInfo.min < 0.0001) {
                    var factor = precursorInfo.max < 0.1 ? 0.1 : 1;
                    precursorInfo.max += factor;
                    precursorInfo.min -= factor;
                }

                // Issue 31678: add any missing dates from the other plots or from the annotations
                var dateProp = this.groupedX ? "date" : "fullDate";
                var precursorDates = Ext4.Array.pluck(precursorInfo.data, dateProp);
                var datesToAdd = [];
                for (var j = 0; j < allPlotDateValues.length; j++) {
                    var dateVal = this.formatDate(allPlotDateValues[j], !this.groupedX);
                    var dataIsMissingDate = precursorDates.indexOf(dateVal) === -1 && Ext4.Array.pluck(datesToAdd, dateProp).indexOf(dateVal) === -1;
                    if (dataIsMissingDate) {
                        datesToAdd.push({
                            type: 'missing',
                            fullDate: this.formatDate(allPlotDateValues[j], true),
                            date: this.formatDate(allPlotDateValues[j]),
                            groupedXTick: dateVal
                        });
                    }
                }
                if (datesToAdd.length > 0) {
                    var index = 0;
                    for (var k = 0; k < datesToAdd.length; k++) {
                        var added = false;
                        for (var l = index; l < precursorInfo.data.length; l++) {
                            if ((this.groupedX && precursorInfo.data[l].date > datesToAdd[k].date)
                                    || (!this.groupedX && precursorInfo.data[l].fullDate > datesToAdd[k].fullDate)) {
                                precursorInfo.data.splice(l, 0, datesToAdd[k]);
                                added = true;
                                index = l;
                                break;
                            }
                        }
                        // tack on any remaining dates to the end
                        if (!added) {
                            precursorInfo.data.push(datesToAdd[k]);
                        }
                    }
                }

                // this.filterPoints - object to store left and right indices to truncate for a series for custom date range
                // when showing reference guide set

                if (this.filterQCPoints) {
                    if (!this.filterPoints) {
                        this.filterPoints = {};
                    }
                    if (!this.filterPoints[frag]) {
                        this.filterPoints[frag] = {};
                    }

                    for (let j = 0; j < precursorInfo.data.length; j++) {
                        let plotData = precursorInfo.data[j];


                        if (!this.filterPoints[frag][plotData.MetricId]) {
                            this.filterPoints[frag][plotData.MetricId] = {}
                        }

                        if (plotData.type === "missing") {
                            continue;
                        }


                        // default "InRange"; promote to "GuideSet" on match so a later guide set can't clobber it
                        plotData['ReferenceRangeSeries'] = "InRange";
                        Ext4.Object.each(this.guideSetDataMap, function(guideSetId, guideSetData) {
                            // guideSetId (map key) is a String; plotData.guideSetId a Number - parse for ===
                            const guideSetIdInt = parseInt(guideSetId, 10);
                            if (plotData.guideSetId === guideSetIdInt && plotData.inGuideSetTrainingRange && guideSetData.TrainingEnd <= this.startDate) {
                                this.filterPoints[frag][plotData.MetricId]['filterPointsFirstIndex'] = j + 1;
                                plotData['ReferenceRangeSeries'] = "GuideSet";
                                return false; // stop once the matching guide set is found
                            }
                        }, this);

                        // for truncating out of range guideset data find last index of plotData starting from this.startDate
                        if (plotData.fullDate >= this.startDate) {
                            if (!this.filterPoints[frag][plotData.MetricId]['filterPointsLastIndex']) {
                                this.filterPoints[frag][plotData.MetricId]['filterPointsLastIndex'] = j;
                            }
                        }
                    }
                }
            }
        }

        var maxPointsPerSeries = 0;
        for (var i = 0; i < this.precursors.length; i++) {
            if (this.fragmentPlotData[this.precursors[i]]) {
                maxPointsPerSeries = Math.max(this.fragmentPlotData[this.precursors[i]].data.length, maxPointsPerSeries);
            }
        }
        this.showDataPoints = maxPointsPerSeries <= LABKEY.targetedms.QCPlotHelperBase.maxPointsPerSeries;

        if (this.showExpRunRange && this.filterPoints) {

            for (let i = 0; i < plotDataRows.length; i++) {
                const seriesPoints = this.filterPoints && this.filterPoints[plotDataRows[i].SeriesLabel];
                if (!seriesPoints) {
                    continue;
                }
                Ext4.Object.each(seriesPoints, function (metricId, filterPointsData) {
                    // no need to filter if less than 6 data points are present between reference end of guideset and startdate
                    if (filterPointsData['filterPointsFirstIndex'] && filterPointsData['filterPointsLastIndex']) {
                        if (filterPointsData['filterPointsLastIndex'] - filterPointsData['filterPointsFirstIndex'] < 6) {
                            // Fewer than 6 out-of-range points for this series/metric, so there is nothing to truncate
                            // for it. Flag only this entry rather than clearing the global this.filterQCPoints, so that
                            // other series still truncate and the separator / guide-set line break still render.
                            filterPointsData['skipTruncation'] = true;
                            // set the startDate field = acquired time of the point right before the experiment run range
                            this.setStartDateFromFilterIndex(plotDataRows[i], filterPointsData['filterPointsFirstIndex']);
                        }
                        else { // skip 5 points
                            filterPointsData['filterPointsLastIndex'] = filterPointsData['filterPointsLastIndex'] - 6;
                            // set the startDate field = acquired time of the point right after the new filter last index
                            this.setStartDateFromFilterIndex(plotDataRows[i], filterPointsData['filterPointsLastIndex'] + 1);
                        }
                    }
                }, this);
            }

        }

        this.renderPlots();
    },

    // filterPoints indices include injected 'missing' entries, but AcquiredTime only exists on raw
    // plotDataRow.data - translate to raw-space by counting non-missing entries, and guard the lookup.
    setStartDateFromFilterIndex: function(plotDataRow, fragIndex) {
        if (!plotDataRow || fragIndex == null) {
            return;
        }
        const fragData = this.fragmentPlotData[plotDataRow.SeriesLabel] && this.fragmentPlotData[plotDataRow.SeriesLabel].data;
        if (!fragData || fragData.length === 0) {
            return;
        }
        // back up to the nearest non-missing entry at or before the index
        let idx = Math.min(fragIndex, fragData.length - 1);
        while (idx >= 0 && fragData[idx] && fragData[idx].type === 'missing') {
            idx--;
        }
        if (idx < 0) {
            return;
        }
        let rawIndex = 0; // count of non-missing entries before idx

        for (let k = 0; k < idx; k++) {
            if (!fragData[k] || fragData[k].type !== 'missing') {
                rawIndex++;
            }
        }
        const rawPoint = plotDataRow.data[rawIndex];
        if (rawPoint && rawPoint.AcquiredTime) {
            this.getStartDateField().setValue(this.formatDate(rawPoint.AcquiredTime));
        }
    },

    renderPlots: function() {
        if (this.filterQCPoints) {
            this.truncateOutOfRangeQCPoints();
        }
        // do not persist plot options in qc folder if changed after coming through experimental folder link
        if (!this.showExpRunRange) {
            this.persistSelectedFormOptions();
        }

        if (this.precursors.length === 0) {
            this.failureHandler({message: "There were no records found. The date filter applied may be too restrictive."});
            return;
        }

        Ext4.get(this.plotDivId).update("");
        this.setBrushingEnabled(false);
        this.setPlotWidth(this.plotDivId);

        let addedPlot;
        const metricProps = {};
        metricProps[this.metric] = this.getMetricPropsById(this.metric);
        if (this.isMultiSeries()) {
            metricProps[this.metric2] = this.getMetricPropsById(this.metric2);
        }

        if (this.singlePlot && this.getMetricPropsById(this.metric).precursorScoped) {
            this.peptideGroups = this.buildPeptideGroups();
            addedPlot = this.addCombinedPeptideSinglePlot(metricProps);
        }
        else {
            this.peptideGroups = null;
            addedPlot = this.addIndividualPrecursorPlots(metricProps);
        }

        if (!addedPlot) {
            Ext4.get(this.plotDivId).insertHtml('beforeEnd', '<div>No data to plot</div>');
        }

        Ext4.get(this.plotDivId).unmask();
    },

    truncateOutOfRangeQCPoints: function() {
        Ext4.Object.each(this.fragmentPlotData, function(label, fragmentData) {
            if (this.filterQCPoints && this.filterPoints && this.filterPoints[label]) {

                // Points are date-sorted with both metrics interleaved, so the out-of-range block (guide set
                // training end -> start date) is one contiguous range spanning both metrics. Splicing the
                // per-metric ranges separately would overlap and corrupt indices, so combine them: start after
                // the last training point of any metric, end at the last "first in-range" point of any metric.
                let firstIndex, lastIndex;
                Ext4.Object.each(this.filterPoints[label], function(metricId, range) {
                    if (range['skipTruncation'] || range['filterPointsFirstIndex'] === undefined
                            || range['filterPointsLastIndex'] === undefined) {
                        return;
                    }
                    firstIndex = firstIndex === undefined ? range['filterPointsFirstIndex'] : Math.max(firstIndex, range['filterPointsFirstIndex']);
                    lastIndex = lastIndex === undefined ? range['filterPointsLastIndex'] : Math.max(lastIndex, range['filterPointsLastIndex']);
                }, this);

                if (firstIndex !== undefined && lastIndex !== undefined) {
                    for (let i = lastIndex; i >= firstIndex; i--) {
                        fragmentData.data.splice(i, 1);
                    }
                }
            }
        }, this);
    },

    getBasePlotConfig : function(id, data, legenddata) {
        return {
            rendererType : 'd3',
            renderTo : id,
            clipRect: true, // set this to true to prevent lines from running outside of the plot region
            data : Ext4.Array.clone(data),
            width : this.getPlotWidth(),
            height : this.singlePlot ? 500 : 300,
            gridLineColor : 'white',
            legendData : Ext4.Array.clone(legenddata),
            legendNoWrap: true
        };
    },

    getPlotWidth: function() {
        return this.plotWidth - 30;
    },

    calculatePlotIndicesBetweenDates: function (precursorInfo) {
        var startDate = new Date(this.expRunDetails.startDate);
        var endDate = new Date(this.expRunDetails.endDate);
        var startIndex;
        var endIndex;

        if (precursorInfo) {
            // fragmentPlotData has plot data separated by series labels
            const data = precursorInfo.data;

            for (let index = 0; index < data.length; index++) {
                const pointDate = new Date(data[index].fullDate)
                if (pointDate >= startDate && pointDate < endDate) {
                    if (startIndex === undefined) {
                        startIndex = data[index].seqValue;
                    }
                }

                if (pointDate >= endDate) {
                    if (!endIndex) {
                        endIndex = data[index].seqValue;
                    }
                }
                // this happens for custom date range shorter than exp date range
                else if (index === data.length - 1 && endIndex === undefined && startIndex !== undefined) {
                    endIndex = data[data.length - 1].seqValue;
                }

                const foundIndices = startIndex !== undefined && endIndex !== undefined;

                if (foundIndices) {
                    this.expRunDetails['startIndex'] = startIndex;
                    this.expRunDetails['endIndex'] = endIndex;
                    break;
                }
            }

        }
    },

    // TODO: Move this to tests
    testVals: {
        a: {fragment:'', dataType: 'Peptide', result: ''},
        b: {fragment:'A', dataType: 'Peptide', result: 'A'},
        c: {fragment:'A', dataType: 'Peptide', result: 'A'}, // duplicate
        d: {fragment:'AB', dataType: 'Peptide', result: 'AB'},
        e: {fragment:'ABC', dataType: 'Peptide', result: 'ABC'},
        f: {fragment:'ABCD', dataType: 'Peptide', result: 'ABCD'},
        g: {fragment:'ABCDE', dataType: 'Peptide', result: 'ABCDE'},
        h: {fragment:'ABCDEF', dataType: 'Peptide', result: 'ABCDEF'},
        i: {fragment:'ABCDEFG', dataType: 'Peptide', result: 'ABCDEFG'},
        j: {fragment:'ABCDEFGH', dataType: 'Peptide', result: 'ABC…FGH'},
        k: {fragment:'ABCDEFGHI', dataType: 'Peptide', result: 'ABC…GHI'},
        l: {fragment:'ABCE', dataType: 'Peptide', result: 'ABCE'},
        m: {fragment:'ABDEFGHI', dataType: 'Peptide', result: 'ABD…'},
        n: {fragment:'ABEFGHI', dataType: 'Peptide', result: 'ABEFGHI'},
        o: {fragment:'ABEFGHIJ', dataType: 'Peptide', result: 'ABE…HIJ'},
        p: {fragment:'ABEFHI', dataType: 'Peptide', result: 'ABEFHI'},
        q: {fragment:'ABFFFGHI', dataType: 'Peptide', result: 'ABF(5)'},
        r: {fragment:'ABFFFFGHI', dataType: 'Peptide', result: 'ABF(6)'},
        s: {fragment:'ABFFFFAFGHI', dataType: 'Peptide', result: 'ABF…FA…'},
        t: {fragment:'ABFFFAFFGHI', dataType: 'Peptide', result: 'ABF…A…'},
        u: {fragment:'ABGAABAABAGHI', dataType: 'Peptide', result: 'ABG…B…B…'},
        v: {fragment:'ABGAAbAABAGHI', dataType: 'Peptide', result: 'ABG…b…B…'},
        w: {fragment:'ABGAABAAbAGHI', dataType: 'Peptide', result: 'ABG…B…b…'},
        x: {fragment:'ABGAAB[80]AAB[99]AGHI', dataType: 'Peptide', result: 'ABG…b…b…'},
        y: {fragment:'C32:0', dataType: 'ion', result: 'C32:0'},
        z: {fragment:'C32:1', dataType: 'ion', result: 'C32:1'},
        aa: {fragment:'C32:2', dataType: 'ion', result: 'C32:2'},
        bb: {fragment:'C32:2', dataType: 'ion', result: 'C32:2'},
        cc: {fragment:'C30:0', dataType: 'ion', result: 'C30:0'},
        dd: {fragment:'C[30]:0', dataType: 'ion', result: 'C[30]:0'},
        ee: {fragment:'C[400]:0', dataType: 'ion', result: 'C[4…'},
        ff: {fragment:'C12:0 fish breath', dataType: 'ion', result: 'C12…'},
        gg: {fragment:'C15:0 fish breath', dataType: 'ion', result: 'C15(14)'},
        hh: {fragment:'C15:0 doggy breath', dataType: 'ion', result: 'C15(15)'},
        ii: {fragment:'C16:0 fishy breath', dataType: 'ion', result: 'C16…f…'},
        jj: {fragment:'C16:0 doggy breath', dataType: 'ion', result: 'C16…d…'},
        kk: {fragment:'C14', dataType: 'ion', result: 'C14'},
        ll: {fragment:'C14:1', dataType: 'ion', result: 'C14:1'},
        mm: {fragment:'C14:1-OH', dataType: 'ion', result: 'C14:1…'},
        nn: {fragment:'C14:2', dataType: 'ion', result: 'C14:2'},
        oo: {fragment:'C14:2-OH', dataType: 'ion', result: 'C14:2…'},
    },

    testLegends: function() {
        var legendHelper = LABKEY.targetedms.QCPlotLegendHelper;
        legendHelper.setupLegendPrefixes(this.testVals, 3);

        for (let key in this.testVals) {
            if (this.testVals.hasOwnProperty(key)) {
                const val = legendHelper.getUniquePrefix(this.testVals[key].fragment, (this.testVals[key].dataType === 'Peptide'));
                if (val !== this.testVals[key].result)
                    console.log("Incorrect result for " + this.testVals[key].fragment + ". Expected: " + this.testVals[key].result + ", Actual: " + val);
            }
        }
    },

    getCombinedPlotLegendData: function(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean) {
        let newLegendData = Ext4.Array.clone(this.legendData),
                proteomicsLegend = [{ //Temp holder for proteomics legend labels
                    text: 'Peptides',
                    separator: true
                }],
                ionLegend = [{ //Temp holder for small molecule legend labels
                    text: 'Ions',
                    separator: true
                }],
                precursorInfo;

        //Add series1 separator to Legend sections
        if (this.isMultiSeries()) {
            proteomicsLegend.push({
                text: metricProps[this.metric].name,
                separator: true
            });

            ionLegend.push({
                text: metricProps[this.metric].name,
                separator: true
            });
        }

        const legendSeries = this.getCombinedPlotLegendSeries(plotType, isCUSUMMean);

        // traverse the precursor list for: calculating the longest legend string and combine the plot data
        for (var i = 0; i < this.precursors.length; i++)
        {
            precursorInfo = this.fragmentPlotData[this.precursors[i]];
            // We may not have a match if it's been filtered out - see issue 38720
            if (precursorInfo) {
                const series1Legend = precursorInfo.dataType === 'Peptide' ? proteomicsLegend : ionLegend;

                series1Legend.push({
                    name: precursorInfo.fragment + (this.isMultiSeries() ? LABKEY.targetedms.QCPlotHelperBase.SERIES_NAME_SEP + legendSeries[0] : ''),
                    text: this.legendHelper.getLegendItemText(precursorInfo),
                    hoverText: precursorInfo.fragment,
                    color: groupColors[i % groupColors.length],
                });
            }
        }

        // add the fragment name for each group to the legend again for the series2 axis metric series
        if (this.isMultiSeries()) {
            proteomicsLegend.push({
                text: metricProps[this.metric2].name,
                separator: true
            });

            ionLegend.push({
                text: metricProps[this.metric2].name,
                separator: true
            });

            for (let i = 0; i < this.precursors.length; i++)
            {
                precursorInfo = this.fragmentPlotData[this.precursors[i]];
                const series2Legend = precursorInfo?.dataType === 'Peptide' ?  proteomicsLegend : ionLegend;

                series2Legend.push({
                    name: precursorInfo?.fragment + LABKEY.targetedms.QCPlotHelperBase.SERIES_NAME_SEP + legendSeries[1],
                    text: this.legendHelper.getLegendItemText(precursorInfo),
                    hoverText: precursorInfo?.fragment,
                    color: groupColors[(this.precursors.length + i) % groupColors.length]
                });
            }
        }

        //Add legends if there is at least one non-separator label
        if (proteomicsLegend.length > yAxisCount + 1) {
            newLegendData = newLegendData.concat(proteomicsLegend);
        }

        if (ionLegend.length > yAxisCount + 1) {
            newLegendData = newLegendData.concat(ionLegend);
        }

        var extraPlotLegendData = this.getAdditionalPlotLegend(plotType);
        newLegendData = newLegendData.concat(extraPlotLegendData);

        return newLegendData;
    },

    getCombinedPlotColorMap: function(metricProps, groupColors, plotType, isCUSUMMean) {
        const SEP = LABKEY.targetedms.QCPlotHelperBase.SERIES_NAME_SEP;
        const legendSeries = this.getCombinedPlotLegendSeries(plotType, isCUSUMMean);
        let colorMap = {};

        for (let i = 0; i < this.precursors.length; i++) {
            let precursorInfo = this.fragmentPlotData[this.precursors[i]];
            if (!precursorInfo) continue;

            let name1 = precursorInfo.fragment + (this.isMultiSeries() ? SEP + legendSeries[0] : '');
            colorMap[name1] = groupColors[i % groupColors.length];

            if (this.isMultiSeries()) {
                let name2 = precursorInfo.fragment + SEP + legendSeries[1];
                colorMap[name2] = groupColors[(this.precursors.length + i) % groupColors.length];
            }
        }
        return colorMap;
    },

    getYScaleLabel: function(plotType, conversion, metricProp) {
        const label = metricProp.yAxisLabel;

        let yScaleLabel;

        let conversionLabel = null;

        if (plotType !== LABKEY.vis.TrendingLinePlotType.MovingRange && plotType !== LABKEY.vis.TrendingLinePlotType.LeveyJennings) {
            yScaleLabel = 'Sum of Deviations'
        }
        if (plotType === LABKEY.vis.TrendingLinePlotType.TrailingMean) {
            yScaleLabel = label;
        }
        if (plotType === LABKEY.vis.TrendingLinePlotType.TrailingCV) {
            yScaleLabel = 'CV (%)';
        }
        else if (conversion) {
            var options = this.getYAxisOptions();
            for (var i = 0; i < options.data.length; i++) {
                if (options.data[i][0] === conversion)
                    conversionLabel = options.data[i][1];
            }
        }

        if (!yScaleLabel) {
            yScaleLabel = label;
            if (conversionLabel) {
                yScaleLabel = yScaleLabel ? (yScaleLabel + ' (' + conversionLabel + ')') : conversionLabel;
            }
        }
        if (this.isMultiSeries()) {
            yScaleLabel = metricProp.name + (yScaleLabel ? (' - ' + yScaleLabel) : '');
        }
        return yScaleLabel;
    },

    getSubtitle: function(precursor) {
        if (!this.isMultiSeries()) {
            return (precursor ? (precursor + ' - ') : '')  + this.getMetricPropsById(this.metric).name;
        }
        return precursor;
    },

    addEachCombinedPrecursorPlot: function(plotIndex, id, combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, plotType, isCUSUMMean, scope) {
        let plotLegendData, treeColorMap;
        if (this.hasPeptideGroupTree && this.hasPeptideGroupTree()) {
            plotLegendData = this.getCombinedPlotLegendData(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean)
                    .filter(function(d) {
                        return (!d.name && !d.separator) || (d.separator && (d.text === 'Annotations' || d.text === 'CUSUM Group'));
                    });
            treeColorMap = this.getCombinedPlotColorMap(metricProps, groupColors, plotType, isCUSUMMean);
        } else {
            plotLegendData = this.getCombinedPlotLegendData(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean);
        }

        if (plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM) {
            this.showInvalidLogMsg(id, showLogInvalid);
        }

        let showRange = false;
        if (plotType === LABKEY.vis.TrendingLinePlotType.CUSUM && !this.metric2) {
            showRange = true;
        }
        else if (this.yAxisScale === 'standardDeviation' && plotType === LABKEY.vis.TrendingLinePlotType.LeveyJennings) {
            showRange = true;
        }
        else if (plotType === LABKEY.vis.TrendingLinePlotType.LeveyJennings && (metricProps[this.metric].upperBound !== undefined || metricProps[this.metric].lowerBound !== undefined)) {
            showRange = true;
        }

        let shapeProp = 'IgnoreInQC';
        let shapeDomain = [undefined, true];
        if (plotType === 'Levey-Jennings') {
            shapeProp = 'LJShape';
            shapeDomain = this.statics().shapeDomain;
        }
        if (plotType === 'MovingRange') {
            shapeProp = 'MRShape';
            shapeDomain = this.statics().shapeDomain;
        }

        var trendLineProps = {
            disableRangeDisplay: !showRange,
            xTick: this.groupedX ? 'groupedXTick' : 'fullDate',
            xTickLabel: 'date',
            shape: shapeProp,
            combined: true,
            yAxisScale: (showLogInvalid ? 'linear' : (this.yAxisScale !== 'log' ? 'linear' : 'log')),
            valueConversion: (this.yAxisScale === LABKEY.vis.PlotProperties.ValueConversion.PercentDeviation ||
                                this.yAxisScale === LABKEY.vis.PlotProperties.ValueConversion.StandardDeviation ||
                                this.yAxisScale === LABKEY.vis.PlotProperties.ValueConversion.DeltaFromMean ? this.yAxisScale : undefined),
            groupBy: 'fragment',
            color: 'fragment',
            defaultGuideSetLabel: 'fragment',
            pointSize: 2,
            pointIdAttr: function(row) { return row['fullDate'] + row['fragment']; },
            shapeRange: [LABKEY.vis.Scale.Shape()[0] /* circle */, LABKEY.vis.Scale.DataspaceShape()[0] /* open circle */, LABKEY.vis.Scale.Shape()[1], LABKEY.vis.Scale.Shape()[2]],
            shapeDomain: shapeDomain,
            showTrendLine: true,
            showDataPoints: this.showDataPoints,
            mouseOverFn: this.plotPointMouseOver,
            mouseOverFnScope: this,
            mouseOutFn: this.plotPointMouseOut,
            mouseOutFnScope: this,
            position: this.groupedX ? 'sequential' : undefined,
            legendMouseOverFn: this.legendMouseOver,
            legendMouseOverFnScope: this,
            legendMouseOutFn: this.plotPointMouseOut,
            legendMouseOutFnScope: this,
            pathMouseOverFn: this.pathMouseOver,
            pathMouseOverFnScope: this,
            pathMouseOutFn: this.plotPointMouseOut,
            pathMouseOutFnScope: this,
            hoverTextFn: !this.showDataPoints ? function(pathData) {
                return Ext4.htmlEncode(pathData.group) + '\nNarrow the date range to show individual data points.'
            } : undefined,
            hideSDLines: true
        };


        if (treeColorMap) {
            trendLineProps.colorMap = treeColorMap;
        }

        if (plotType === 'Levey-Jennings') {
            trendLineProps.showBoundLines = false;
        }
        Ext4.apply(trendLineProps, this.getPlotTypeProperties(combinePlotData, plotType, isCUSUMMean, metricProps));

        let yZoomDomainCombined = this.getYZoomDomain ? this.getYZoomDomain(id) : null;
        if (yZoomDomainCombined) {
            if (yZoomDomainCombined.left) trendLineProps.yZoomDomain = yZoomDomainCombined.left;
            if (yZoomDomainCombined.right) trendLineProps.yZoomDomainRight = yZoomDomainCombined.right;
        }

        // Suppress the mean line for multi-series plots
        trendLineProps.mean = undefined;

        const mainTitle = LABKEY.targetedms.QCPlotHelperWrapper.getQCPlotTypeLabel(plotType, isCUSUMMean);

        const basePlotConfig = this.getBasePlotConfig(id, combinePlotData.data, plotLegendData);
        const plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                right: (this.showInPlotLegends() ? legendMargin : 30 ) + (this.isMultiSeries() ? 60 : 10),
                left: 75,
                bottom: 75
            },
            labels : {
                main: {
                    value: mainTitle
                },
                subtitle: {
                    value: this.getSubtitle(''),
                    visibility: 'hidden',  // Set as hidden so it doesn't clutter the web UI. It'll get set to visible during export, where it's useful context.
                    color: '#555555'
                },
                yLeft: {
                    value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps[this.metric])
                },
                yRight: {
                    value: this.isMultiSeries() ? this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps[this.metric2]) : undefined,
                    visibility: this.isMultiSeries() ? undefined : 'hidden'
                }
            },
            brushing: !this.allowGuideSetBrushing() ? undefined : {
                dimension: 'x',
                fillOpacity: 0.4,
                fillColor: 'rgba(20, 204, 201, 1)',
                strokeColor: 'rgba(20, 204, 201, 1)',
                brushstart: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushStartEvent(plot);
                },
                brush: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushEvent(extent, plot, layerSelections);
                },
                brushend: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushEndEvent(data[data.length - 1], extent, plot);
                },
                brushclear: function(event, data, plot, layerSelections) {
                    scope.plotBrushClearEvent(data[data.length - 1], plot);
                }
            },
            properties: trendLineProps
        });

        plotConfig.qcPlotType = plotType;
        const plot = LABKEY.vis.TrendingLinePlot(plotConfig);
        plot.render();

        this.addYZoomInteraction(plot, id);
        this.attachCombinedLegendClickHandlers();

        this.addAnnotationsToPlot(plot, combinePlotData);

        this.addGuideSetTrainingRangeToPlot(plot, combinePlotData);

        let urlParams = LABKEY.ActionURL.getParameters();
        if (parseInt(urlParams['replicateId']) > 0) {
            this.highlightOutliersForClickedReplicate(plot, combinePlotData, parseInt(urlParams['replicateId']));
        }

        this.attachPlotExportIcons(id, mainTitle + '- All Series', plotIndex, this.getPlotWidth(), this.showInPlotLegends() ? 0 : legendMargin);
    },

    addEachIndividualPrecursorPlot: function(plotIndex, id, precursorIndex, precursorInfo, metricProps, plotType, isCUSUMMean, scope) {
        let trailingMeanOrCVPlot = plotType === LABKEY.vis.TrendingLinePlotType.TrailingMean ||
                plotType === LABKEY.vis.TrendingLinePlotType.TrailingCV;
        if (trailingMeanOrCVPlot) {
            if (this.trailingRuns >= this.runs) {
                Ext4.get(id).update("<span class='labkey-error'> " + plotType + " - The number you entered is larger than the number of available runs. Only " + this.runs + " runs are used for calculation</span>");
                return;
            }
            else if (this.trailingRuns <= 2) {
                Ext4.get(id).update("<span class='labkey-error'> " + plotType + " - Please enter a positive integer (>2) that is less than or equal to the total number of available runs - " + this.runs + " </span>");
                return;
            }
        }
        else if (this.yAxisScale === 'log' && plotType !== LABKEY.vis.TrendingLinePlotType.LeveyJennings && plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM) {
            Ext4.get(id).update("<span style='font-style: italic;'>Values that are 0 have been replaced with 0.0000001 for log scale plot.</span>");
        }
        else if (precursorInfo.showLogInvalid && plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM) {
            this.showInvalidLogMsg(id, true);
        }
        else if (precursorInfo.showLogWarning && plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM) {
            Ext4.get(id).update("<span style='font-style: italic;'>For log scale, standard deviations below "
                    + "the mean with negative values have been omitted.</span>");
        }

        var showDataPoints = precursorInfo.data ? precursorInfo.data.length <= LABKEY.targetedms.QCPlotHelperBase.maxPointsPerSeries : true;

        let shapeProp = 'IgnoreInQC';
        let shapeDomain = [undefined, true];
        if (plotType === 'Levey-Jennings') {
            shapeProp = 'LJShape';
            shapeDomain = this.statics().shapeDomain;
        }
        if (plotType === 'MovingRange') {
            shapeProp = 'MRShape';
            shapeDomain = this.statics().shapeDomain;
        }

        var trendLineProps = {
            xTick: this.groupedX ? 'groupedXTick' : 'fullDate',
            xTickLabel: 'date',
            yAxisScale: (precursorInfo.showLogInvalid ? 'linear' : (this.yAxisScale !== 'log' ? 'linear' : 'log')),
            valueConversion: (this.yAxisScale === LABKEY.vis.PlotProperties.ValueConversion.PercentDeviation ||
                                this.yAxisScale === LABKEY.vis.PlotProperties.ValueConversion.StandardDeviation ||
                                this.yAxisScale === LABKEY.vis.PlotProperties.ValueConversion.DeltaFromMean ? this.yAxisScale : undefined),
            shape: shapeProp,
            combined: false,
            pointSize: 2,
            pointIdAttr: function(row) { return row['fullDate']; },
            shapeRange: [LABKEY.vis.Scale.Shape()[0] /* circle */, LABKEY.vis.Scale.DataspaceShape()[0] /* open circle */, LABKEY.vis.Scale.Shape()[1], LABKEY.vis.Scale.Shape()[2]],
            shapeDomain: shapeDomain,
            showTrendLine: true,
            showDataPoints: showDataPoints,
            defaultGuideSetLabel: 'fragment',
            defaultGuideSets: this.defaultGuideSet,
            mouseOverFn: this.plotPointMouseOver,
            mouseOverFnScope: this,
            position: this.groupedX ? 'sequential' : undefined,
            disableRangeDisplay: this.isMultiSeries(),
            hoverTextFn: !showDataPoints ? function() { return 'Narrow the date range to show individual data points.' } : undefined,
            hideSDLines: true,
            showBoundLines: metricProps.metricStatus !== LABKEY.targetedms.MetricStatus.PlotOnly
        };

        // lines are not separated when indices are not present
        if (this.filterQCPoints && this.filterPoints) {
            trendLineProps.lineColor = '#000000';
            trendLineProps.groupBy = "ReferenceRangeSeries";
        }

        Ext4.apply(trendLineProps, this.getPlotTypeProperties(precursorInfo, plotType, isCUSUMMean, metricProps));

        let yZoomDomain = this.getYZoomDomain ? this.getYZoomDomain(id) : null;
        if (yZoomDomain) {
            if (yZoomDomain.left) trendLineProps.yZoomDomain = yZoomDomain.left;
            if (yZoomDomain.right) trendLineProps.yZoomDomainRight = yZoomDomain.right;
        }

        var plotLegendData = this.getAdditionalPlotLegend(plotType);
        if (Ext4.isArray(this.legendData)) {
            plotLegendData = plotLegendData.concat(this.legendData);
        }

        if (plotLegendData && plotLegendData.length > 0) {
            Ext4.each(plotLegendData, function(legend) {
                if (legend.text && legend.text.length > 0) {
                    if ( !this.longestLegendText || (this.longestLegendText && legend.text.length > this.longestLegendText))
                        this.longestLegendText = legend.text.length;
                }
            }, this);
        }

        const mainTitle = LABKEY.targetedms.QCPlotHelperWrapper.getQCPlotTypeLabel(plotType, isCUSUMMean);

        const leftMargin = 75;
        const leftMarginOffset = this.getYAxisLeftMarginOffset(precursorInfo) + leftMargin;

        const labels = {
            main: {
                value: mainTitle
            },
            subtitle: {
                value: this.getSubtitle(this.precursors[precursorIndex]),
                visibility: 'hidden',  // Set as hidden so it doesn't clutter the web UI. It'll get set to visible during export, where it's useful context.
                color: '#555555'
            },
            yLeft: {
                value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps[this.metric]),
                        position: leftMarginOffset > 0 ? leftMarginOffset - 15 : undefined
            }
        };
        if (this.isMultiSeries()) {
            const defaultColors = LABKEY.vis.Scale.ColorDiscrete();
            labels.yLeft.color = defaultColors[0];
            labels.yRight = {
                value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps[this.metric2]),
                color: defaultColors[1]
            }
        }

        const basePlotConfig = this.getBasePlotConfig(id, precursorInfo.data, plotLegendData);
        const plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                left: leftMarginOffset,
                bottom: 75,
                right: (this.showInPlotLegends() ? 0 : 30) // if in plot, set to 0 to auto calculate margin; otherwise, set to small value to cut off legend
            },
            labels: labels,
            properties: trendLineProps,
            brushing: !this.allowGuideSetBrushing() ? undefined : {
                dimension: 'x',
                fillOpacity: 0.4,
                fillColor: 'rgba(20, 204, 201, 1)',
                strokeColor: 'rgba(20, 204, 201, 1)',
                brushstart: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushStartEvent(plot);
                },
                brush: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushEvent(extent, plot, layerSelections);
                },
                brushend: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushEndEvent(data[data.length - 1], extent, plot);
                },
                brushclear: function(event, data, plot, layerSelections) {
                    scope.plotBrushClearEvent(data[data.length - 1], plot);
                }
            }
        });

        // create plot using the JS Vis API
        plotConfig.qcPlotType = plotType;
        const plot = LABKEY.vis.TrendingLinePlot(plotConfig);
        plot.render();

        this.addYZoomInteraction(plot, id);
        this.addAnnotationsToPlot(plot, precursorInfo);
        this.addGuideSetTrainingRangeToPlot(plot, precursorInfo);

        let urlParams = LABKEY.ActionURL.getParameters();
        if (parseInt(urlParams['replicateId']) > 0) {
            this.highlightOutliersForClickedReplicate(plot, precursorInfo, parseInt(urlParams['replicateId']));
        }

        const extraMargin = this.showInPlotLegends() ? 0 : 10 * this.longestLegendText;
        this.attachPlotExportIcons(id, mainTitle + '-' + this.precursors[precursorIndex] + '-' + this.getMetricPropsById(this.metric).name, plotIndex, this.getPlotWidth(), extraMargin);
    },

    getYAxisLeftMarginOffset: function(precursorInfo) {
        if (precursorInfo.min === undefined || precursorInfo.max === undefined) {
            return 0;
        }

        var maxLength = Math.max(precursorInfo.min.toString().length, precursorInfo.max.toString().length);

        // maxLength of yAxis value
        // if less than 10 then the current left margin works fine
        // else add 2 pixels per digit/character
        if (maxLength < 10) {
            return 0;
        } else {
            return (maxLength - 10) * 2;
        }
    },

    // empty legend to reserve plot space for plot alignment
    getEmptyLegend: function() {
        var empty = [];
        empty.push({
            text: '',
            shape: function(){
                return 'M0,0L0,0Z';
            }
        });
        return empty;
    },

    showInPlotLegends: function () {
        return true;
    },

    addYZoomInteraction: function(plot, plotId) {
        let me = this;
        let svg = this.getSvgElForPlot(plot);
        let grid = plot.grid;

        if (!plot.scales.yLeft || !plot.scales.yLeft.scale || !plot.scales.yLeft.scale.invert) {
            return;
        }

        let gridTop = grid.topEdge;
        let gridBottom = grid.bottomEdge;
        let gridLeft = grid.leftEdge;
        let gridRight = grid.rightEdge;

        let clampY = function(y) {
            return Math.max(gridTop, Math.min(gridBottom, y));
        };

        let zoomEntry = this.getYZoomDomain ? this.getYZoomDomain(plotId) : null;

        // Creates an independent drag/click overlay for one y-axis (left or right).
        // overlayX/overlayW define where the invisible hit area sits.
        // btnAnchorX is the left edge of the Zoom button.
        let setupAxisOverlay = function(axis, yScale, overlayX, overlayW, btnAnchorX) {
            let isZoomed = !!(zoomEntry && zoomEntry[axis]);

            let overlayEl = svg.append('rect')
                .attr('class', 'y-zoom-overlay' + (isZoomed ? ' zoomed' : ''))
                .attr('x', overlayX)
                .attr('y', gridTop)
                .attr('width', overlayW)
                .attr('height', gridBottom - gridTop)
                .style({'fill': '#ffffff', 'fill-opacity': 0, 'pointer-events': 'all'});

            if (isZoomed) {
                overlayEl.on('click', function() { me.resetYZoom(plotId, axis); });
                return;
            }

            let dragStartY = null, dragCurrentY = null;
            let selectionRect = null, zoomButtonGroup = null, pendingLine = null, pendingStartY = null;
            let interactionMask = null, plotClickCapture = null;
            let moveNs = 'mousemove.yzoom-' + axis;
            let keyNs = 'keydown.yzoom-' + axis;

            let removeOverlays = function() {
                if (selectionRect) { selectionRect.remove(); selectionRect = null; }
                if (zoomButtonGroup) { zoomButtonGroup.remove(); zoomButtonGroup = null; }
                if (pendingLine) { pendingLine.remove(); pendingLine = null; }
                if (interactionMask) { interactionMask.remove(); interactionMask = null; }
                if (plotClickCapture) { plotClickCapture.remove(); plotClickCapture = null; }
            };

            let showZoomButtons = function(y1, y2) {
                let domainMax = yScale.invert(y1);
                let domainMin = yScale.invert(y2);
                let yMid = y1 + (y2 - y1) / 2;

                // Block all plot interactions while zoom buttons are visible
                interactionMask = svg.append('rect')
                    .attr('x', 0).attr('y', 0)
                    .attr('width', parseFloat(svg.attr('width')) || (gridRight + 80))
                    .attr('height', parseFloat(svg.attr('height')) || (gridBottom + 50))
                    .style({'fill': '#ffffff', 'fill-opacity': 0, 'pointer-events': 'all', 'cursor': 'default'});

                zoomButtonGroup = svg.append('g').attr('class', 'y-zoom-buttons');

                let makeBtn = function(text, xLeft, width, onClick) {
                    let btnG = zoomButtonGroup.append('g').attr('class', 'y-zoom-btn-' + text.toLowerCase());
                    btnG.append('rect')
                        .attr('x', xLeft).attr('y', yMid - 10).attr('rx', 5).attr('ry', 5)
                        .attr('width', width).attr('height', 20)
                        .style({'fill': '#ffffff', 'stroke': '#b4b4b4'});
                    btnG.append('text')
                        .text(text)
                        .attr('x', xLeft + width / 2).attr('y', yMid + 4)
                        .style({'fill': '#126495', 'font-size': '10px', 'font-weight': 'bold',
                                'text-anchor': 'middle', 'text-transform': 'uppercase', 'pointer-events': 'none'});
                    btnG.on('click', onClick);
                    return btnG;
                };

                makeBtn('Zoom', btnAnchorX, 50, function() {
                    removeOverlays();
                    me.applyYZoom(plotId, domainMin, domainMax, axis);
                });

                makeBtn('Cancel', btnAnchorX + 60, 55, function() {
                    removeOverlays();
                });
            };

            let cancelPendingClick = function() {
                pendingStartY = null;
                svg.on(moveNs, null);
                d3.select(document).on(keyNs, null);
                removeOverlays();
            };

            let startClickModeTracking = function(startY) {
                pendingStartY = startY;

                pendingLine = svg.append('line')
                    .attr('class', 'y-zoom-pending-line')
                    .attr('x1', gridLeft).attr('y1', startY)
                    .attr('x2', gridRight).attr('y2', startY)
                    .style('pointer-events', 'none');

                svg.on(moveNs, function() {
                    let currentY = clampY(d3.mouse(svg.node())[1]);
                    let y1 = Math.min(pendingStartY, currentY);
                    let y2 = Math.max(pendingStartY, currentY);
                    let h = y2 - y1;

                    if (selectionRect) {
                        selectionRect.attr('x', gridLeft).attr('y', y1)
                            .attr('width', gridRight - gridLeft).attr('height', Math.max(1, h));
                    } else {
                        selectionRect = svg.append('rect')
                            .attr('class', 'y-zoom-selection')
                            .attr('x', gridLeft).attr('y', y1)
                            .attr('width', gridRight - gridLeft)
                            .attr('height', Math.max(1, h))
                            .style('pointer-events', 'none');
                    }
                });

                d3.select(document).on(keyNs, function() {
                    if (d3.event.key === 'Escape' || d3.event.keyCode === 27) {
                        cancelPendingClick();
                    }
                });

                plotClickCapture = svg.append('rect')
                    .attr('x', gridLeft).attr('y', gridTop)
                    .attr('width', gridRight - gridLeft).attr('height', gridBottom - gridTop)
                    .style({'fill': '#ffffff', 'fill-opacity': 0, 'pointer-events': 'all', 'cursor': 'crosshair'})
                    .on('click', function() {
                        let clickY = clampY(d3.mouse(svg.node())[1]);
                        let firstY = pendingStartY;
                        cancelPendingClick();
                        let finalY1 = Math.min(firstY, clickY);
                        let finalY2 = Math.max(firstY, clickY);
                        if (finalY2 - finalY1 < 5) { return; }
                        showZoomButtons(finalY1, finalY2);
                    });
            };

            let drag = d3.behavior.drag()
                .on('dragstart', function() {
                    dragStartY = clampY(d3.mouse(svg.node())[1]);
                    dragCurrentY = dragStartY;
                    if (zoomButtonGroup) { zoomButtonGroup.remove(); zoomButtonGroup = null; }
                })
                .on('drag', function() {
                    dragCurrentY = clampY(d3.mouse(svg.node())[1]);

                    if (pendingStartY !== null && Math.abs(dragCurrentY - dragStartY) >= 5) {
                        cancelPendingClick();
                    }

                    let y1 = Math.min(dragStartY, dragCurrentY);
                    let y2 = Math.max(dragStartY, dragCurrentY);
                    let h = y2 - y1;

                    if (h < 1) { return; }

                    if (selectionRect) {
                        selectionRect.attr('x', gridLeft).attr('y', y1)
                            .attr('width', gridRight - gridLeft).attr('height', h);
                    } else {
                        selectionRect = svg.append('rect')
                            .attr('class', 'y-zoom-selection')
                            .attr('x', gridLeft).attr('y', y1)
                            .attr('width', gridRight - gridLeft)
                            .attr('height', h)
                            .style('pointer-events', 'none');
                    }
                })
                .on('dragend', function() {
                    let y1 = Math.min(dragStartY, dragCurrentY);
                    let y2 = Math.max(dragStartY, dragCurrentY);

                    if (y2 - y1 < 5) { return; }

                    if (pendingStartY !== null) { cancelPendingClick(); }
                    showZoomButtons(y1, y2);
                });

            overlayEl.call(drag);

            overlayEl.on('click', function() {
                let clickY = clampY(d3.mouse(svg.node())[1]);

                if (pendingStartY === null) {
                    removeOverlays();
                    startClickModeTracking(clickY);
                } else {
                    let firstY = pendingStartY;
                    cancelPendingClick();

                    let finalY1 = Math.min(firstY, clickY);
                    let finalY2 = Math.max(firstY, clickY);
                    if (finalY2 - finalY1 < 5) { return; }

                    showZoomButtons(finalY1, finalY2);
                }
            });
        };

        // Left axis overlay
        setupAxisOverlay('left', plot.scales.yLeft.scale, 0, gridLeft - 2, gridLeft + 5);

        // Right axis overlay — only when a right scale exists
        if (plot.scales.yRight && plot.scales.yRight.scale && plot.scales.yRight.scale.invert) {
            let svgWidth = parseFloat(svg.attr('width')) || (gridRight + 80);
            let rightOverlayX = gridRight + 2;
            let rightOverlayW = Math.max(1, svgWidth - rightOverlayX);
            // Buttons sit just inside the plot to the left of the right axis (Zoom 50px + gap 10px + Cancel 55px = 115px)
            setupAxisOverlay('right', plot.scales.yRight.scale, rightOverlayX, rightOverlayW, gridRight - 120);
        }

        if (zoomEntry) {
            let gridWidth = gridRight - gridLeft;
            let gridHeight = gridBottom - gridTop;
            let clipId = (plot.renderTo || plotId) + '-yzoom-clip';

            let svgDefs = svg.select('defs');
            if (svgDefs.empty()) {
                svgDefs = svg.insert('defs', ':first-child');
            }
            svgDefs.append('clipPath')
                .attr('id', clipId)
                .append('rect')
                .attr('x', gridLeft).attr('y', gridTop)
                .attr('width', gridWidth).attr('height', gridHeight);

            svg.selectAll('g.layer').attr('clip-path', 'url(#' + clipId + ')');

            svg.append('rect')
                .attr('class', 'y-zoom-border')
                .attr('x', gridLeft).attr('y', gridTop)
                .attr('width', gridWidth).attr('height', gridHeight)
                .style({'fill': 'none', 'stroke': '#888', 'stroke-width': '1px', 'pointer-events': 'none'});
        }
    }
});