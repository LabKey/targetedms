/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.BaseQCPlotPanel', {

    extend: 'Ext.panel.Panel',

    // properties used for the various data queries based on chart metric type
    chartTypePropArr: [{
        name: 'retentionTime',
        title: 'Retention Time',
        series1Label: 'Retention Time',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_retentionTime'
    },
    {
        name: 'peakArea',
        title: 'Peak Area',
        series1Label: 'Peak Area',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_peakArea'
    },
    {
        name: 'fwhm',
        title: 'Full Width at Half Maximum (FWHM)',
        series1Label: 'Full Width at Half Maximum (FWHM)',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_fwhm'
    },
    {
        name: 'fwb',
        title: 'Full Width at Base (FWB)',
        series1Label: 'Full Width at Base (FWB)',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_fwb'
    },
    {
        name: 'ratio',
        title: 'Light/Heavy Ratio',
        series1Label: 'Light/Heavy Ratio',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_lhRatio'
    },
    {
        name: 'transitionPrecursorRatio',
        title: 'Transition/Precursor Area Ratio',
        series1Label: 'Transition/Precursor Area Ratio',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_transitionPrecursorRatio'
    },
    {
        name: 'transitionAndPrecursorArea',
        title: 'Transition/Precursor Areas',
        series1Label: 'Transition Area',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_transitionArea',
        series2Label: 'Precursor Area',
        series2SchemaName: 'targetedms',
        series2QueryName: 'QCMetric_precursorArea'
    },
    {
        name: 'massAccuracy',
        title: 'Mass Accuracy',
        series1Label: 'Mass Accuracy',
        series1SchemaName: 'targetedms',
        series1QueryName: 'QCMetric_massAccuracy'
    }],

    metricGuideSetSql : function(schema1Name, query1Name, schema2Name, query2Name)
    {
        var includeCalc = !Ext4.isDefined(schema2Name) && !Ext4.isDefined(query2Name),
            selectCols = 'SampleFileId, SampleFileId.AcquiredTime, SeriesLabel' + (includeCalc ? ', MetricValue' : ''),
            series1SQL = 'SELECT ' + selectCols + ' FROM '+ schema1Name + '.' + query1Name,
            series2SQL = includeCalc ? '' : ' UNION SELECT ' + selectCols + ' FROM '+ schema2Name + '.' + query2Name;

        return 'SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, '
            + '\nCOUNT(p.SampleFileId) AS NumRecords, '
            + '\n' + (includeCalc ? 'AVG(p.MetricValue)' : 'NULL') + ' AS Mean, '
            + '\n' + (includeCalc ? 'STDDEV(p.MetricValue)' : 'NULL') + ' AS StandardDev '
            + '\nFROM guideset gs'
            + '\nLEFT JOIN (' + series1SQL + series2SQL + ') as p'
            + '\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd'
            + '\nGROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel';
    },

    addPlotWebPartToPlotDiv: function (id, title, div, wp)
    {
        Ext4.get(div).insertHtml('beforeEnd', '<br/>' +
                '<table class="labkey-wp ' + wp + '">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left">' +
                '        <span class="labkey-wp-title-text ' +  wp + '-title">'+ Ext4.util.Format.htmlEncode(title) +
                '           <div class="plot-export-btn" id="' + id + '-exportToPDFbutton"></div>' +
                '        </span>' +
                '     </th>' +
                ' </tr><tr>' +
                '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
                ' </tr>' +
                '</table>'
        );
    },

    setPlotWidth: function (div)
    {
        if (this.plotWidth == null)
        {
            // set the width of the plot webparts based on the first labkey-wp-body element
            this.plotWidth = 900;
            var wp = document.querySelector('.labkey-wp-body');
            if (wp && (wp.clientWidth - 20) > this.plotWidth)
            {
                this.plotWidth = wp.clientWidth - 20;
            }

            Ext4.get(div).setWidth(this.plotWidth);
        }
    },

    createExportToPDFButton: function (id, title, filename)
    {
        new Ext4.Button({
            renderTo: id + "-exportToPDFbutton",
            svgDivId: id,
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: "Export PDF of this plot",
            handler: function (btn)
            {
                LABKEY.vis.SVGConverter.convert(this.getExportSVGStr(btn), LABKEY.vis.SVGConverter.FORMAT_PDF, filename);
            },
            scope: this
        });
    },

    getExportSVGStr: function(btn)
    {
        var svgEls = Ext4.get(btn.svgDivId).select('svg');
        var svgStr = LABKEY.vis.SVGConverter.svgToStr(svgEls.elements[0]);
        svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
        return svgStr;
    },

    failureHandler: function(response) {
        if (response.message) {
            Ext4.get(this.plotDivId).update("<span>" + response.message +"</span>");
        }
        else {
            Ext4.get(this.plotDivId).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
        }

        Ext4.get(this.plotDivId).unmask();
    }
});





