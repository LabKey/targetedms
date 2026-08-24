/**
 *
 * Copyright (c) 2015-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 7/9/15.
 *
 * Plain JS/HTML implementation (no ExtJS). Renders one set of Pareto plots per guide set
 * into the element identified by config.plotDivId.
 */
if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

(function() {

    const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

    function ParetoPlotPanel(config) {
        this.plotDivId = config.plotDivId;
        this.metricPropArr = [];
        this.plotWidth = null;
        this._maskEl = null;

        this.mask('Loading...');

        const me = this;
        LABKEY.targetedms.QCMetricConfigLoader.getMetrics(this.initPlot, this, function() {
            me.unmask();
            const el = me.getPlotDiv();
            if (el) {
                el.innerHTML = 'Failed to load';
            }
        });
    }

    ParetoPlotPanel.prototype = {

        getPlotDiv: function() {
            return document.getElementById(this.plotDivId);
        },

        mask: function(text) {
            const el = this.getPlotDiv();
            if (!el) {
                return;
            }
            this.unmask();
            const m = document.createElement('div');
            m.className = 'lk-pareto-loading';
            m.style.padding = '10px';
            m.textContent = text || 'Loading...';
            el.appendChild(m);
            this._maskEl = m;
        },

        unmask: function() {
            if (this._maskEl && this._maskEl.parentNode) {
                this._maskEl.parentNode.removeChild(this._maskEl);
            }
            this._maskEl = null;
        },

        initPlot: function(metrics) {
            this.metricPropArr = metrics;

            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api'),
                success: this.processResponse,
                failure: LABKEY.Utils.getCallbackWrapper(this.failureHandler, this),
                scope: this
            });
        },

        processResponse: function(response) {
            this.unmask();

            const parsed = JSON.parse(response.responseText);
            const el = this.getPlotDiv();

            if (!parsed.sampleFiles || Object.keys(parsed.sampleFiles).length === 0) {
                if (el) {
                    el.innerHTML = '<div class="tiledPlotPanel">No sample files loaded yet. Import some via Skyline, AutoQC, or the Data Pipeline tab here in Panorama.</div>';
                }
                return;
            }

            const guideSets = parsed.guideSets;
            const me = this;

            guideSets.forEach(function(guideSet) {
                guideSet.stats = {
                    CUSUMm: {count: 0, data: []},
                    CUSUMv: {count: 0, data: []},
                    mR: {count: 0, data: []},
                    Value: {count: 0, data: []}
                };

                Object.keys(guideSet.MetricCounts).forEach(function(metricName) {
                    const data = guideSet.MetricCounts[metricName];
                    me.addOutlierToCounts(guideSet, data, metricName, 'CUSUMm', 'CUSUMm', true);
                    me.addOutlierToCounts(guideSet, data, metricName, 'CUSUMv', 'CUSUMv', true);
                    me.addOutlierToCounts(guideSet, data, metricName, 'mR', 'Moving Range', true);
                    me.addOutlierToCounts(guideSet, data, metricName, 'Value', 'Metric Value', true);
                });

                Object.keys(guideSet.stats).forEach(function(outlierType) {
                    const data = guideSet.stats[outlierType];
                    const dataSet = data.data;

                    let totalCount = 0;
                    let maxOutliers = 0;

                    // find total count per guidesetID
                    for (let i = 0; i < dataSet.length; i++) {
                        totalCount += dataSet[i]['count'];

                        if (maxOutliers < dataSet[i]['count']) {
                            maxOutliers = dataSet[i]['count'];
                        }
                    }

                    // sort by count in descending order
                    const sortedDataset = dataSet.sort(function(a, b) {
                        const order = b.count - a.count;
                        if (order !== 0) {
                            return order;
                        }
                        return a.metricLabel.localeCompare(b.metricLabel);
                    });

                    // calculate cumulative percentage on sorted data
                    for (let j = 0; j < sortedDataset.length; j++) {
                        sortedDataset[j].percent = (j === 0 ? 0 : sortedDataset[j - 1].percent) + ((sortedDataset[j].count / totalCount) * 100);
                    }
                    data.maxOutliers = maxOutliers;
                });
            });

            let guideSetCount = 1;
            guideSets.forEach(function(guideSetData) {
                const id = "paretoPlot-GuideSet-" + guideSetCount;
                const dateFormat = LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i';
                const title = "Training Start: " + me.formatDate(new Date(guideSetData.TrainingStart), dateFormat)
                        + (guideSetData.ReferenceEnd ? " - Reference End: " + me.formatDate(new Date(guideSetData.ReferenceEnd), dateFormat) : " - Training End: " + me.formatDate(new Date(guideSetData.TrainingEnd), dateFormat));

                const webpartTitleBase = "Guide Set " + guideSetCount + ' ';
                const wp = 'pareto-plot-wp';
                const fileBase = "ParetoPlot-Guide Set " + guideSetCount;

                me.addEachParetoPlot(id, webpartTitleBase, "Metric Value", wp, title, fileBase, guideSetData.stats.Value.data, guideSetData.stats.Value.maxOutliers);
                me.addEachParetoPlot(id + '_mR', webpartTitleBase, "Moving Range", wp, title, fileBase + '_mR', guideSetData.stats.mR.data, guideSetData.stats.mR.maxOutliers);
                me.addEachParetoPlot(id + '_CUSUMm', webpartTitleBase, "Mean CUSUM", wp, title, fileBase + '_CUSUMm', guideSetData.stats.CUSUMm.data, guideSetData.stats.CUSUMm.maxOutliers);
                me.addEachParetoPlot(id + '_CUSUMv', webpartTitleBase, "Variability CUSUM", wp, title, fileBase + '_CUSUMv', guideSetData.stats.CUSUMv.data, guideSetData.stats.CUSUMv.maxOutliers);

                guideSetCount++;
            });
        },

        addOutlierToCounts: function(guideSet, data, metricName, propertyName, plotTypeParamValue, isCusum) {
            const count = data[propertyName];
            guideSet.stats[propertyName].count += count;
            const newData = {
                metricLabel: metricName,
                count: count,
                metricId: data.MetricId,
                TrainingStart: guideSet.TrainingStart,
                ReferenceEnd: guideSet.ReferenceEnd,
                plotType: plotTypeParamValue
            };
            if (isCusum) {
                newData.CUSUMNegative = data[propertyName + 'N'];
                newData.CUSUMPositive = data[propertyName + 'P'];
            }

            guideSet.stats[propertyName].data.push(newData);
        },

        addEachParetoPlot: function(id, wpTitle, plotType, wp, plotTitle, fileName, plotData, yAxisMax) {
            this.addPlotWebPartToPlotDiv(id, wpTitle, wp);
            this.setPlotWidth();
            this.plotPareto(id, plotData, plotTitle, yAxisMax, plotType);
            this.attachPlotExportIcons(id, id, 0, this.plotWidth - 30, 0);
        },

        plotPareto: function(id, data, title, yAxisMax, plotType) {
            let tickValues;
            if (yAxisMax < 10) {
                tickValues = [];
                for (let i = 0; i <= yAxisMax; i++) {
                    tickValues.push(i);
                }
            }
            const hoverFn = plotType.indexOf('CUSUM') > -1 ? this.plotBarHoverEvent : undefined;
            const barChart = new LABKEY.vis.Plot({
                renderTo: id,
                rendererType: 'd3',
                width: this.plotWidth - 30,
                height: 500,
                data: data.slice(),
                labels: {
                    main: {value: "Pareto Plot - " + plotType},
                    subtitle: {value: title, color: '#555555'},
                    yLeft: {value: '# Outliers'},
                    yRight: {value: 'Cumulative Percentage'}
                },
                layers: [
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.BarPlot({clickFn: this.plotBarClickEvent, hoverFn: hoverFn})
                    }),
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.Path({color: 'steelblue'}),
                        aes: {x: 'metricLabel', yRight: 'percent'}
                    }),
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.Point({color: 'steelblue'}),
                        aes: {x: 'metricLabel', yRight: 'percent', hoverText: function(val) {return val.percent.toPrecision(4) + "%"}}
                    })
                ],
                aes: {
                    x: 'metricLabel',
                    y: 'count'
                },
                scales: {
                    x: {
                        scaleType: 'discrete',
                        tickHoverText: function(val) {
                            return val;
                        }
                    },
                    yLeft: {
                        domain: [0, (yAxisMax === 0 ? 1 : yAxisMax)],
                        tickValues: tickValues
                    },
                    yRight: {
                        domain: [0, 100]
                    }
                },
                margins: {
                    bottom: 75
                }
            });
            barChart.render();
        },

        plotBarClickEvent: function(event, row) {
            const params = {startDate: row.TrainingStart, metric: row.metricId, plotTypes: row.plotType};
            if (row.ReferenceEnd) {
                params.endDate = row.ReferenceEnd;
            }
            window.location = LABKEY.ActionURL.buildURL('project', 'begin', null, params);
        },

        plotBarHoverEvent: function(row) {
            const CUSUMN = row.CUSUMNegative ? row.CUSUMNegative : 0, CUSUMP = row.CUSUMPositive ? row.CUSUMPositive : 0;
            return 'CUSUM-:' + ' ' + CUSUMN + '\nCUSUM+:' + ' ' + CUSUMP + '\nTotal: ' + row.count;
        },

        // ---- helpers previously inherited from the ExtJS BaseQCPlotPanel ----

        getPlotWebPartHeader: function(wp, title) {
            return '<br/>' +
                    '<table class="labkey-wp ' + LABKEY.Utils.encodeHtml(wp) + '">' +
                    ' <tr class="labkey-wp-header">' +
                    '     <th class="labkey-wp-title-left">' +
                    '        <span class="labkey-wp-title-text ' + LABKEY.Utils.encodeHtml(wp) + '-title">' + LABKEY.Utils.encodeHtml(title) + '</span>' +
                    '     </th>' +
                    ' </tr>';
        },

        addPlotWebPartToPlotDiv: function(id, title, wp) {
            let html = this.getPlotWebPartHeader(wp, title);
            html += '<tr>' +
                    '     <td class="labkey-wp-body">' +
                    '        <div id="' + LABKEY.Utils.encodeHtml(id) + '" class="chart-render-div"></div>' +
                    '     </td>' +
                    ' </tr>' +
                    '</table>';
            const div = this.getPlotDiv();
            if (div) {
                div.insertAdjacentHTML('beforeend', html);
            }
        },

        setPlotWidth: function() {
            if (this.plotWidth == null) {
                // set the width of the plot webparts based on the first labkey-wp-body element
                this.plotWidth = 900;
                const spacer = 33;
                const wp = document.querySelector('.panel.panel-portal');
                if (wp && (wp.clientWidth - spacer) > this.plotWidth) {
                    this.plotWidth = wp.clientWidth - spacer;
                }

                const div = this.getPlotDiv();
                if (div) {
                    div.style.width = this.plotWidth + 'px';
                }
            }
        },

        attachPlotExportIcons: function(id, plotTitle, plotIndex, plotWidth, extraMargin) {
            const me = this;
            this.createExportIcon(id, 'fa-file-pdf-o', 'Export to PDF', 0, plotIndex, plotWidth, function() {
                me.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PDF, plotTitle);
            });

            this.createExportIcon(id, 'fa-file-image-o', 'Export to PNG', 1, plotIndex, plotWidth, function() {
                me.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PNG, plotTitle);
            });
        },

        createExportIcon: function(divId, iconCls, tooltip, indexFromLeft, plotIndex, plotWidth, callbackFn) {
            const leftPositionPx = (indexFromLeft * 30) + 60,
                    exportIconDivId = divId + iconCls,
                    html = '<div id="' + exportIconDivId + '" class="export-icon" title="' + LABKEY.Utils.encodeHtml(tooltip) + '" style="left: ' + leftPositionPx + 'px;">'
                            + '<i class="fa ' + iconCls + '"></i></div>';

            const container = document.getElementById(divId);
            if (container) {
                container.insertAdjacentHTML('afterbegin', html);
                const iconEl = document.getElementById(exportIconDivId);
                if (iconEl) {
                    iconEl.addEventListener('click', callbackFn);
                }
            }
        },

        exportChartToImage: function(svgDivId, extraMargin, type, fileName) {
            const svgStr = this.getExportSVGStr(svgDivId, extraMargin),
                    exportType = type || LABKEY.vis.SVGConverter.FORMAT_PDF;
            LABKEY.vis.SVGConverter.convert(svgStr, exportType, fileName);
        },

        getExportSVGStr: function(svgDivId, extraWidth) {
            const container = document.getElementById(svgDivId);
            const targetSvg = container.querySelector('svg');
            const oldWidth = targetSvg.getBoundingClientRect().width;
            // temporarily increase svg size to allow exporting of legends that's outside svg
            if (extraWidth) {
                targetSvg.setAttribute('width', oldWidth + extraWidth);
            }
            let svgStr = LABKEY.vis.SVGConverter.svgToStr(targetSvg);
            if (extraWidth) {
                targetSvg.setAttribute('width', oldWidth);
            }
            svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
            return svgStr;
        },

        failureHandler: function(response) {
            const plotDiv = this.getPlotDiv();
            if (plotDiv) {
                this.unmask();
                if (!response) {
                    plotDiv.innerHTML = "<span>Failure loading data</span>";
                }
                else if (response.message) {
                    plotDiv.innerHTML = "<span>" + LABKEY.Utils.encodeHtml(response.message) + "</span>";
                }
                else {
                    plotDiv.innerHTML = "<span class='labkey-error'>Error: " + LABKEY.Utils.encodeHtml(response.exception) + "</span>";
                }
            }
        },

        /**
         * Minimal replacement for Ext4.util.Format.date, supporting the PHP/Ext-style tokens used by
         * LABKEY.extDefaultDateTimeFormat (default 'Y-m-d H:i').
         */
        formatDate: function(date, format) {
            if (!date || isNaN(date.getTime())) {
                return '';
            }
            const pad = function(n) { return (n < 10 ? '0' : '') + n; };
            const year = date.getFullYear();
            const month = date.getMonth();
            const day = date.getDate();
            const dow = date.getDay();
            const hours = date.getHours();
            const minutes = date.getMinutes();
            const seconds = date.getSeconds();
            let h12 = hours % 12;
            if (h12 === 0) {
                h12 = 12;
            }
            const pad3 = function(n) { return (n < 10 ? '00' : n < 100 ? '0' : '') + n; };

            // Timezone abbreviation, derived the way Ext4 derived its 'T' token
            const tzMatch = date.toString().match(/^.* (?:\((.*)\)|([A-Z]{1,5})(?:\s|$))/);
            const tzOffset = date.getTimezoneOffset();
            const absOffset = Math.abs(tzOffset);

            // ISO-8601 week number: a week belongs to the year that its Thursday falls in
            const thursday = new Date(year, month, day);
            thursday.setDate(thursday.getDate() - ((dow + 6) % 7) + 3);
            const firstThursday = new Date(thursday.getFullYear(), 0, 4);
            firstThursday.setDate(firstThursday.getDate() - ((firstThursday.getDay() + 6) % 7) + 3);

            const tokens = {
                Y: '' + year,
                y: ('' + year).slice(-2),
                m: pad(month + 1),
                n: '' + (month + 1),
                F: MONTHS[month],
                M: MONTHS[month].slice(0, 3),
                d: pad(day),
                j: '' + day,
                l: DAYS[dow],
                D: DAYS[dow].slice(0, 3),
                N: '' + (dow === 0 ? 7 : dow),
                w: '' + dow,
                H: pad(hours),
                G: '' + hours,
                h: pad(h12),
                g: '' + h12,
                i: pad(minutes),
                s: pad(seconds),
                u: pad3(date.getMilliseconds()),
                A: hours < 12 ? 'AM' : 'PM',
                a: hours < 12 ? 'am' : 'pm',
                // toExtDateFormat emits these from the Java z, Z, w and D patterns
                T: tzMatch ? (tzMatch[1] || tzMatch[2]) : '',
                O: (tzOffset > 0 ? '-' : '+') + pad(Math.floor(absOffset / 60)) + pad(absOffset % 60),
                W: pad(1 + Math.round((thursday - firstThursday) / 604800000)),
                z: '' + Math.round((new Date(year, month, day) - new Date(year, 0, 1)) / 86400000)
            };

            let out = '';
            for (let i = 0; i < format.length; i++) {
                const c = format[i];
                if (c === '\\' && i + 1 < format.length) {
                    out += format[++i];
                }
                else if (Object.prototype.hasOwnProperty.call(tokens, c)) {
                    out += tokens[c];
                }
                else {
                    out += c;
                }
            }
            return out;
        }
    };

    LABKEY.targetedms.ParetoPlotPanel = ParetoPlotPanel;
})();
