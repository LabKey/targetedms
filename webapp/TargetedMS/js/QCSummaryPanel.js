/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.QCSummary', {
    extend: 'Ext.panel.Panel',

    border: false,

    numSampleFileStats: null,

    initComponent: function (config)
    {
        this.qcPlotPanel = Ext4.create('LABKEY.targetedms.BaseQCPlotPanel');

        this.callParent();

        this.add({
            xtype: 'label',
            text: 'Loading...'
        })

        this.qcPlotPanel.queryQCInstruments(this.getQCSummary, this);
        this.numSampleFileStats = config ? config.sampleLimit : 3;
    },

    formatInstruments: function(container) {
        if (container.distinctInstruments) {
            if (container.distinctInstruments.length > 1) {
                container.instrument = ' for multiple instruments: <ul>';
                for (let index = 0; index < container.distinctInstruments.length; index++) {
                    container.instrument += '<li>' + this.formatInstrument(container.distinctInstruments[index], container.path) + '</li>';
                }
                container.instrument += '</ul> We recommend that each instrument use its own QC folder.';
            }
            else if (container.distinctInstruments.length === 1 && container.distinctInstruments[0]) {
                container.instrument = ' for ' + this.formatInstrument(container.distinctInstruments[0], container.path);
            }
        }
    },

    formatInstrument: function(name, containerPath) {
        let result = Ext4.util.Format.htmlEncode(name ? name : 'unknown instrument');
        if (name)
            result = '<a href="' +
                    LABKEY.ActionURL.buildURL('targetedms', 'showInstrument', containerPath, {name: name}) +
                    '">' + result + '</a>'
        return result;
    },

    getQCSummary: function () {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'getQCSummary.api'),
            params: {
                includeSubfolders: true
            },
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function (response) {
                this.removeAll();
                var containers = response['containers'],
                        container,
                        childPanelItems = [],
                        hasChildren = containers.length > 1;

                // determine the summaryView width
                var portalWebpart = document.querySelector('.panel.panel-portal'),
                        minWidth = 750,
                        width = portalWebpart ? Math.max(portalWebpart.clientWidth - 50, minWidth) : minWidth;
                if (hasChildren && containers.length > 1 && (width/2) > minWidth) {
                    width = (width / 2) - 5;
                }

                // Add the current (root) container to the QC Summary display
                container = containers[0];
                container.showName = hasChildren;
                container.isParent = true;
                container.parentOnly = containers.length === 1;
                this.formatInstruments(container);
                this.add(this.getContainerSummaryView(container, hasChildren, width));

                // Add the set of child containers in an hbox layout
                if (hasChildren) {
                    for (var i = 1; i < containers.length; i++) {
                        container = containers[i];
                        this.formatInstruments(container);
                        container.showName = true;
                        container.parentOnly = false;
                        container.isParent = false;
                        childPanelItems.push(this.getContainerSummaryView(container, undefined, width));
                    }

                    this.add(Ext4.create('Ext.panel.Panel', {
                        border: false,
                        items: childPanelItems
                    }));
                }

            }, this, false),
            failure: LABKEY.Utils.getCallbackWrapper(function (response) {
                this.removeAll();
                this.add(Ext4.create('Ext.Component', {
                    autoEl: 'span',
                    cls: 'labkey-error',
                    html: 'Error: ' + response.exception
                }));
            }, this, true)
        });
    },

    getContainerSummaryView: function (container, hasChildren, width) {
        container.viewCmpId = Ext4.id();
        container.autoQcCalloutId = Ext4.id();
        container.width = width;

        var config = {
            id: container.viewCmpId,
            data: container,
            tpl: this.getSummaryDisplayTpl(),
            listeners: {
                scope: this,
                render: function () {
                    this.queryContainerSampleFileStats(container);

                    // add hover event listeners for showing AutoQC message
                    this.showAutoQCMessage(container.autoQcCalloutId, container.autoQCPing, hasChildren);
                }
            }
        };

        if (Ext4.isDefined(hasChildren)) {
            config.cls = hasChildren ? 'summary-view' : '';
            config.width = hasChildren ? width : undefined;
            config.minHeight = 21;
        }
        else {
            config.cls = 'summary-view subfolder-view';
            config.width = width;
            config.minHeight = 136;
        }

        config.cls += ' summary-tile'; // For tests

        return Ext4.create('Ext.view.View', config);
    },

    getSummaryDisplayTpl: function () {
        return new Ext4.XTemplate(
            '<tpl if="showName !== undefined">',
                '<tpl if="showName === true &amp;&amp; (isParent !== true || docCount &gt; 0)">',
                    '<div class="folder-name">',
                        '<a href="{path:this.getContainerLink}">{name:htmlEncode}</a>',
                    '</div>',
                '</tpl>',
                '<tpl if="docCount == 0 && isParent !== true">',
                    '<div class="qc-summary-text">No sample files imported</div>',
                    '<div class="auto-qc-ping" id="{autoQcCalloutId}">AutoQC <span class="{autoQCPing:this.getAutoQCPingClass}"></span></div>',
                '<tpl elseif="docCount == 0 && parentOnly">',
                    '<div class="qc-summary-text">No data found.</div><div>&nbsp;</div>' + this.getAutoQCSetupInfo(),
                '<tpl elseif="docCount &gt; 0 && LABKEY.user.isAdmin">',
                    '<div class="qc-summary-text">',
                        '<a href="{path:this.getSampleFileLink}">{fileCount} replicate{fileCount:this.pluralize}</a> ' +
                            'tracking <a href="{path:this.getPrecursorConfigLink}">{precursorCount} precursor{precursorCount:this.pluralize}</a> ' +
                            'with <a href="{path:this.getMetricConfigLink}">{metricCount} metric{metricCount:this.pluralize}</a> {instrument}',
                    '</div>',
                '<tpl elseif="docCount &gt; 0">',
                    '<div class="qc-summary-text">',
                        '<a href="{path:this.getSampleFileLink}">{fileCount} replicate{fileCount:this.pluralize}</a> ' +
                        'tracking {precursorCount} precursor{precursorCount:this.pluralize} with {metricCount} metric{metricCount:this.pluralize} {instrument}',
                    '</div>',
                '</tpl>',
                '<tpl if="docCount &gt; 0">',
                    '<div class="item-text sample-file-details sample-file-details-loading" id="qc-summary-samplefiles-{id}">Loading...</div>',
                    '<div class="auto-qc-ping" id="{autoQcCalloutId}">AutoQC <span class="{autoQCPing:this.getAutoQCPingClass}"></span></div>',
                '</tpl>',
                '<tpl if="!LABKEY.user.isGuest">',
                    '<div class="email-notifications" id="{autoQcCalloutId}"><a href="{path:this.getEmailNotificationLink}">Notifications <span class="fa fa-envelope"></span></a></div>',
                '</tpl>',
            '</tpl>',
            {
                pluralize: function (val)
                {
                    return val === 1 ? '' : 's';
                },
                getContainerLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('project', 'begin', path);
                },
                getSampleFileLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('query', 'executeQuery', path,
                            {schemaName: 'targetedms', 'query.queryName': 'SampleFile'});
                },
                getMetricConfigLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('targetedms', 'configureQCMetric', path);
                },
                getPrecursorConfigLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('targetedms', 'configureQCGroups', path);
                },
                getEmailNotificationLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('targetedms', 'subscribeOutlierNotifications', path);
                },
                getFullHistoryLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('targetedms', 'qcSummaryHistory', path);
                },
                getAutoQCPingClass: function (val)
                {
                    if (val == null)
                        return 'qc-none fa fa-circle-o';
                    return val.isRecent ? 'qc-correct fa fa-check-circle' : 'qc-error fa fa-circle';
                }
            }
        );
    },

    getAutoQCSetupInfo: function() {
        return '<div><a href="https://panoramaweb.org/home/wiki-page.view?name=autoqc_loader" target="_blank" rel="noopener noreferrer">AutoQC</a>' +
                ' can automically analyze and import system suitability data into this folder using a Skyline template document.</div><br/>' +
                '<div>After installing AutoQC, create a configuration for this folder. Within its Panorama setting tab, ' +
                'check the Publish to Panorama checkbox. Use <span style="font-weight: bold; white-space: nowrap;">' + LABKEY.Utils.encodeHtml(LABKEY.ActionURL.getBaseURL()) +
                '</span> as the URL and <span style="font-weight: bold; white-space: nowrap;">' + LABKEY.Utils.encodeHtml(LABKEY.ActionURL.getContainer()) + '</span>' +
                ' as the folder path.</div>'
    },

    showAutoQCMessage : function(divId, autoQC, hasChildren) {

        var divEl = Ext4.get(divId),
            content = '',
            me = this;

        if (!divEl)
            return;

        if (autoQC == null) {
            content = 'AutoQC has never pinged this folder';
        }
        else
        {
            var modifiedFormatted = Ext4.util.Format.date(new Date(autoQC.modified), LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i:s');
            content = autoQC.isRecent ? 'AutoQC pinged recently on ' + modifiedFormatted : 'AutoQC last pinged on ' + modifiedFormatted;
            if (autoQC.softwareVersion)
            {
                content += "<br/>Version: " + LABKEY.Utils.encodeHtml(autoQC.softwareVersion);
            }
        }

        content = '<div>' + content + '</div><br/>' + this.getAutoQCSetupInfo();

        tippy(divEl.dom, {
            content: content,
            allowHTML: true,
            placement: 'right',
            arrow: true,
            maxWidth: 300,
            trigger: 'mouseenter',
            hideOnClick: false,
            appendTo: document.body,
            followCursor: 'initial',
            onShow(instance) {
                // Hide any previously open tooltip
                if (me.currentTippy && me.currentTippy !== instance) {
                    me.currentTippy.hide();
                }
                me.currentTippy = instance;

                const tippyBox = instance.popper.querySelector('.tippy-box');
                const tippyContent = instance.popper.querySelector('.tippy-content');
                const tippyArrow = instance.popper.querySelector('.tippy-arrow');

                if (tippyBox) {
                    tippyBox.style.backgroundColor = 'white';
                    tippyBox.style.color = 'black';
                    tippyBox.style.border = '2px solid #808080';
                    tippyBox.style.boxShadow = '0 0 10px rgba(0,0,0,0.2)';
                }
                if (tippyContent) {
                    tippyContent.style.padding = '10px';
                    tippyContent.style.maxHeight = 'none';
                    tippyContent.style.overflow = 'visible';
                    tippyContent.style.height = 'auto';
                    tippyContent.style.width = 'auto';
                }
                if (tippyArrow) {
                    tippyArrow.style.color = 'white';
                    // Create border using multiple 1px drop shadows
                    tippyArrow.style.filter = 'drop-shadow(0 0 0 #808080) drop-shadow(0 1px 0 #808080) drop-shadow(0 -1px 0 #808080) drop-shadow(1px 0 0 #808080) drop-shadow(-1px 0 0 #808080)';
                    // Adjust positioning to account for the border
                    const placement = instance.props.placement;
                    if (placement.startsWith('bottom')) {
                        tippyArrow.style.top = '-1px';
                    } else if (placement.startsWith('top')) {
                        tippyArrow.style.bottom = '-1px';
                    } else if (placement.startsWith('left')) {
                        tippyArrow.style.right = '-1px';
                    } else if (placement.startsWith('right')) {
                        tippyArrow.style.left = '-1px';
                    }
                }
            }
        });
    },

    queryContainerSampleFileStats: function (container) {
        if (container.fileCount > 0) {
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api', container.path),
                params: {sampleLimit: this.sampleLimit},
                success: function(response) {
                    var parsed = JSON.parse(response.responseText);
                    if(parsed.sampleFiles) {
                        this.renderContainerSampleFileStats({
                            container: container,
                            limitedSampleFiles: true,
                            sampleFiles: parsed.sampleFiles
                        })
                    } else {
                        this.removeSampleFilesDetailsDiv(container);
                    }
                },
                failure: LABKEY.Utils.getCallbackWrapper(function(response) {
                    var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);

                    if (response.message) {
                        sampleFilesDiv.update("<span>" + Ext4.util.Format.htmlEncode(response.message) + "</span>");
                    }
                    else {
                        sampleFilesDiv.update("<span class='labkey-error'>Error: " + Ext4.util.Format.htmlEncode(response.exception) + "</span>");
                    }
                    sampleFilesDiv.removeCls('sample-file-details-loading');
                }, null, true),
                scope: this
            });
        }
        else if (container.docCount > 0) {
           this.removeSampleFilesDetailsDiv(container);
        }
    },

    removeSampleFilesDetailsDiv: function (container) {
        var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);
        sampleFilesDiv.update('');
        sampleFilesDiv.removeCls('sample-file-details-loading');
    },

    renderContainerSampleFileStats: function (params) {
        let container = params.container;
        let sampleFiles = params.sampleFiles;
        let metrics = [];
        let seenMetrics = {};
        Ext4.iterate(sampleFiles, function (sampleFile) {
            Ext4.iterate(sampleFile.Metrics, function (metric) {
                if (!seenMetrics[metric.MetricId]) {
                    metrics.push(metric);
                    seenMetrics[metric.MetricId] = true;
                }
            });
        });

        let showMetrics = LABKEY.ActionURL.getAction().toLowerCase() === 'qcSummaryHistory'.toLowerCase();
        let tableWidth = container.width - 100;
        let html = '';
        let thead = '';
        if (showMetrics) {
            html = '<table class="table-condensed labkey-data-region-legacy labkey-show-borders" style="width: ' + tableWidth + 'px">';
            thead = '<thead><tr><td></td></td><td class="labkey-column-header">Replicate Name</td><td class="labkey-column-header">Acquired</td>';
            Ext4.each(metrics, function (item) {
                thead += '<td class="labkey-column-header">' + Ext4.util.Format.htmlEncode(item.MetricLabel) + '</td>';
            });
            thead += '<td class="labkey-column-header"><b>Total</b></td>';
        }
        else {
            html = '<table class="table-condensed labkey-data-region-legacy labkey-show-borders">';
            thead = '<thead><tr><td></td></td><td class="labkey-column-header">Replicate Name</td><td class="labkey-column-header">Acquired</td><td class="labkey-column-header">Outliers</td>';
        }

        thead += '</tr></thead>';
        html += thead;

        Ext4.iterate(sampleFiles, function (sampleFile) {
            // create a new div id for each sampleFile to use for the hover details callout
            sampleFile.calloutId = Ext4.id();

            var totalOutliers = sampleFile.Value;

            var iconCls;
            if (sampleFile.IgnoreForAllMetric)
                iconCls = 'fa-ban qc-none';
            else if (totalOutliers > 0)
                iconCls = 'fa-times-rectangle qc-error';
            else
                iconCls = 'fa-check qc-correct';
            let acqDate = Ext4.util.Format.htmlEncode(Ext4.util.Format.date(sampleFile.AcquiredTime ? new Date(sampleFile.AcquiredTime) : null, LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i:s'));
            html += '<tr id="' + sampleFile.calloutId + '"><td>'
                    + '<span class="fa ' + iconCls + '" style="width: 1em; text-align: center"></span></td><td><div class="sample-file-item">' + Ext4.util.Format.htmlEncode(sampleFile.ReplicateName) + '</div></td><td><div class="sample-file-item-acquired" style="text-wrap: nowrap">' + acqDate + '</div></td>';



            if (showMetrics) {
                Ext4.each(metrics, function (metric) {
                    let isMetricPresent = false;
                    Ext4.each(sampleFile.Metrics, function (item) {
                        if (metric.MetricId === item.MetricId) {
                            isMetricPresent = true;
                            html += '<td><div class="sample-file-item" style="text-align: right">' + Ext4.util.Format.htmlEncode(item.Value) + '</div></td>';
                        }
                    });
                    if (!isMetricPresent) {
                        html += '<td><div class="sample-file-item" style="text-align: right">N/A</div></td>';
                    }
                });
            }
            if (sampleFile.IgnoreForAllMetric) {
                html += '<td><div class="sample-file-item-total-outliers" style="text-align: center"><b>excluded</b></div></td>';
            }
            else {
                html += '<td><div class="sample-file-item-total-outliers" style="text-align: right"><b>' + totalOutliers + "</b></div></td>"
            }
            html += '</tr>';
        });
        html += '</table>';
        if (container.fileCount > sampleFiles.length) {
            html += '<div class="qc-summary-text"><a href="' + LABKEY.ActionURL.buildURL('targetedms', 'qcSummaryHistory.view', container.path) + '">View all ' + container.fileCount + ' replicates and utilization calendar <span class="fa fa-calendar"></span></a></div>';
        }
        var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);
        sampleFilesDiv.update(html);
        sampleFilesDiv.removeCls('sample-file-details-loading');

        // since the height of the panel will change from adding up to three lines of text, need to reset the size of the view
        this.doLayout();

        // add a hover listener for each of the sample file divs
        Ext4.iterate(sampleFiles, function (sampleFile) {
            this.showSampleFileStatsDetails(sampleFile.calloutId, sampleFile);
        }, this);
    },

    showSampleFileStatsDetails : function(divId, sampleFile) {
        var task = new Ext4.util.DelayedTask(),
            divEl = Ext4.get(divId),
            content = '',
            me = this;

        var sampleHREF = LABKEY.ActionURL.buildURL('targetedms', 'showSampleFile', LABKEY.ActionURL.getContainer(), {id: sampleFile.SampleId});

        content += '<h3 title="' + Ext4.util.Format.htmlEncode(sampleFile.FilePath) + '"><a href="' + sampleHREF + '">' +
                Ext4.util.Format.htmlEncode(sampleFile.ReplicateName) +
                '</a>' + (sampleFile.AcquiredTime ? (', acquired ' + Ext4.util.Format.date(sampleFile.AcquiredTime ? new Date(sampleFile.AcquiredTime) : null, LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i:s')) : '' ) +
                '</h3><br/>';

        // generate the HTML content for the sample file display details
        if (sampleFile.IgnoreForAllMetric) {
            content += '<div>Not included in QC</div>';
        }
        else if (!sampleFile.LeveyJennings && !sampleFile.mR && !sampleFile.CUSUMm && !sampleFile.CUSUMv) {
            content += '<div>No outliers</div>';
        }
        else {
            content += '<table class="labkey-data-region-legacy labkey-show-borders">';
            content += '<thead><tr>' +
                                '<td class="labkey-column-header outlier-column-header" rowspan="3" style="vertical-align: bottom">Metric</td>' +
                                '<td class="labkey-column-header" colspan="8" style="text-align: center">Outliers</td>' +
                            '</tr>' +
                            '<tr>' +
                                '<td class="labkey-column-header outlier-column-header" rowspan="2" style="vertical-align: bottom">Value</td>' +
                                '<td class="labkey-column-header outlier-column-header" rowspan="2" style="vertical-align: bottom">&nbsp;&nbsp;&nbsp;&nbsp;</td>' +
                                '<td class="labkey-column-header outlier-column-header" rowspan="2" style="vertical-align: bottom">Moving Range</td>' +
                                '<td class="labkey-column-header" colspan="4" style="text-align: center">CUSUM</td>' +
                            '</tr>' +
                            '<tr>' +
                                '<td class="labkey-column-header outlier-column-header" title="Mean CUSUM-">Mean-</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Mean CUSUM+">Mean+</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Variability CUSUM-">Variability-</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Variability CUSUM+">Variability+</td>' +
                            '</tr>' +
                        '</thead><tbody>';

            var rowCount = 0;
            Ext4.each(sampleFile.Metrics, function (item)
            {
                // pass replicate id here so that the outlier details can be displayed in the context of the replicate
                let urlParams = {};
                urlParams['replicateId'] = sampleFile.ReplicateId;
                urlParams['metric'] = item.MetricId;
                let href = LABKEY.ActionURL.buildURL('project', 'begin', item.ContainerPath, urlParams);
                content += '<tr class="' + (rowCount % 2 === 0 ? 'labkey-alternate-row' : 'labkey-row') + '">';
                content += '<td class="outlier-metric-label"><a href="' + href + '">' + Ext4.util.Format.htmlEncode(item.MetricLabel) + '</a></td>';
                if (item.IgnoreInQC) {
                    content += '<td style="text-align: center" colspan="7"><em>not included in QC</em></td>';
                }
                else {
                    content += '<td style="text-align: right">' +
                            (item.MetricStatus === LABKEY.targetedms.MetricStatus.PlotOnly ? 'N/A' : this.getSampleDetailOutlierDisplayValue(item, 'Value')) +
                            '</td>';
                    content += '<td></td>';
                    content += '<td style="text-align: right">' + this.getSampleDetailOutlierDisplayValue(item, 'mR') + '</td>';
                    content += '<td style="text-align: right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMmN') + '</td>';
                    content += '<td style="text-align: right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMmP') + '</td>';
                    content += '<td style="text-align: right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMvN') + '</td>';
                    content += '<td style="text-align: right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMvP') + '</td>';
                }
                content += '</tr>';
                rowCount++;
            }, this);

            content += '<tr class="' + (rowCount % 2 === 0 ? 'labkey-alternate-row' : 'labkey-row') + '">';
            content += '<td class="outlier-metric-label">Total</td>';
            content += '<td style="text-align: right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'Value') + '</td>';
            content += '</tr>';

            content += '</tbody>';
            content += '</table>';
        }

        tippy(divEl.dom, {
            content: content,
            allowHTML: true,
            placement: 'bottom',
            arrow: true,
            maxWidth: sampleFile.Metrics.length > 0 ? 800 : 300,
            trigger: 'mouseenter',
            delay: [500, 0],
            interactive: true,
            hideOnClick: false,
            appendTo: document.body,
            followCursor: 'initial',
            onShow(instance) {
                // Hide any previously open tooltip
                if (me.currentTippy && me.currentTippy !== instance) {
                    me.currentTippy.hide();
                }
                me.currentTippy = instance;

                // Apply light background styling
                const tippyBox = instance.popper.querySelector('.tippy-box');
                const tippyContent = instance.popper.querySelector('.tippy-content');
                const tippyArrow = instance.popper.querySelector('.tippy-arrow');

                if (tippyBox) {
                    tippyBox.style.backgroundColor = 'white';
                    tippyBox.style.color = 'black';
                    tippyBox.style.border = '2px solid #808080';
                    tippyBox.style.boxShadow = '0 0 10px rgba(0,0,0,0.2)';
                }
                if (tippyContent) {
                    tippyContent.style.padding = '10px';
                    tippyContent.style.maxHeight = 'none';
                    tippyContent.style.overflow = 'visible';
                    tippyContent.style.height = 'auto';
                    tippyContent.style.width = 'auto';
                }
                if (tippyArrow) {
                    tippyArrow.style.color = 'white';
                    // Create border using multiple 1px drop shadows
                    tippyArrow.style.filter = 'drop-shadow(0 0 0 #808080) drop-shadow(0 1px 0 #808080) drop-shadow(0 -1px 0 #808080) drop-shadow(1px 0 0 #808080) drop-shadow(-1px 0 0 #808080)';
                    // Adjust positioning to account for the border
                    const placement = instance.props.placement;
                    if (placement.startsWith('bottom')) {
                        tippyArrow.style.top = '-1px';
                    } else if (placement.startsWith('top')) {
                        tippyArrow.style.bottom = '-1px';
                    } else if (placement.startsWith('left')) {
                        tippyArrow.style.right = '-1px';
                    } else if (placement.startsWith('right')) {
                        tippyArrow.style.left = '-1px';
                    }
                }

                // Add a delay before hiding when mouse leaves
                instance.popper.addEventListener('mouseleave', function() {
                    setTimeout(function() {
                        if (!instance.state.isVisible) return;
                        instance.hide();
                    }, 1000);
                });
            }
        });
    },

    getSampleDetailOutlierDisplayValue : function(item, variable) {
        var value = item[variable];
        return value || 0
    },
    
    sortObjectOfObjects: function (data, attr) {
        var arr = [];
        for (var prop in data) {
            if (data.hasOwnProperty(prop)) {
                var obj = {};
                obj[prop] = data[prop];
                obj.tempSortName = data[prop][attr];
                arr.push(obj);
            }
        }

        arr.sort(function(a, b) {
            var at = a.tempSortName,
                    bt = b.tempSortName;
            return at > bt ? 1 : ( at < bt ? -1 : 0 );
        });

        var result = [];
        for (var i=0, l=arr.length; i<l; i++) {
            var obj = arr[i];
            delete obj.tempSortName;
            for (var prop in obj) {
                if (obj.hasOwnProperty(prop)) {
                    var id = prop;
                }
            }
            var item = obj[id];
            result.push(item);
        }
        return result;
    }
});
