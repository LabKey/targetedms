/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

if (!LABKEY.targetedms.PlotSettingsUtil) {
    LABKEY.targetedms.PlotSettingsUtil = {
        saveAsDefault: function () {
            LABKEY.Ajax.request({
                method: 'POST',
                url: LABKEY.ActionURL.buildURL('targetedms', 'saveQCPlotSettingsAsDefault'),
                success: function() { alert('Defaults saved successfully'); },
                failure: LABKEY.Utils.getCallbackWrapper(function(error) { alert('Failed to save defaults'); console.log(error); }, this, true)
            });
        },

        revertToDefault: function () {
            LABKEY.Ajax.request({
                method: 'POST',
                url: LABKEY.ActionURL.buildURL('targetedms', 'revertToDefaultQCPlotSettings'),
                success: function() { window.location.reload(); },
                failure: LABKEY.Utils.getCallbackWrapper(function(error) { alert('Failed to revert to defaults'); console.log(error); }, this, true)
            });
        },

        formatNumeric: function(val) {
            if (LABKEY.vis.isValid(val)) {
                if (val > 100000 || val < -100000) {
                    return val.toExponential(3);
                }
                return parseFloat(val.toFixed(3));
            }
            return "N/A";
        }
    }
}

/**
 * Class to create a panel for displaying the R plot for the trending of retention times, peak areas, and other
 * values for the selected graph parameters.
 */
Ext4.define('LABKEY.targetedms.QCTrendPlotPanel', {

    extend: 'LABKEY.targetedms.BaseQCPlotPanel',
    mixins: {helper: 'LABKEY.targetedms.QCPlotHelperWrapper'},
    header: false,
    border: false,
    labelAlign: 'left',
    items: [],
    defaults: {
        xtype: 'panel',
        border: false
    },

    // properties specific to this TargetedMS QC plot implementation
    yAxisScale: 'linear',
    metric: null,
    plotTypes: ['Metric Value'],
    dateRangeOffset: 0,
    minAcquiredTime: null,
    maxAcquiredTime: null,
    startDate: null,
    endDate: null,
    groupedX: false,
    singlePlot: false,
    showDataPoints: false,
    showExpRunRange: false,
    showExcluded: false,
    showExcludedPrecursors: false,
    showReferenceGS: true,
    plotWidth: null,
    enableBrushing: false,
    havePlotOptionsChanged: false,
    selectedAnnotations: {},
    runs: null,
    trailingRuns: null,
    minWidth: 1250, // Keep in sync with the width defined in qcTrendPlot.jsp
    width: '100%',

    SHOW_ALL_IN_A_SINGLE_PLOT: 'Show all series in a single plot',
    LABEL_WIDTH: 115,

    // Max number of plots/series to show per page
    maxCount: 50,

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        this.callParent();

        // min and max acquired date must be provided
        if (this.minAcquiredTime == null || this.maxAcquiredTime == null)
            Ext4.get(this.plotDivId).update("<span class='labkey-error'>Unable to render report. Missing min and max AcquiredTime from data query.</span>");
        else {
            Ext4.get(this.plotDivId).update("Loading...");
            // Load replicate annotations in the callback.
            LABKEY.targetedms.QCMetricConfigLoader.getMetrics(this.queryContainerReplicateAnnotations, this, function() {
                Ext4.get(this.plotDivId).update('Failed to load');
            });
        }
    },

    queryInitialPlotOptions : function() {
        // If there are URL parameters (i.e. from Pareto Plot click), set those as initial values as well.
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
            method: 'POST',
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response) {
                // convert the boolean and integer values from strings
                var initValues = {};
                Ext4.iterate(response.properties, function(key, value)
                {
                    if (value === "true" || value === "false") {
                        value = value === "true";
                    }
                    else if (value && value.length > 0 && !isNaN(Number(value))) {
                        value = +value;
                    }
                    else if (key === 'plotTypes') { // convert string to array
                        // We've renamed this plot type in the UI and need to map previously saved values
                        value = value.replace('Levey-Jennings', 'Metric Value');
                        value = value.split(',');
                    }
                    if(key === 'selectedAnnotations' && value) {
                        const annotations = {};

                        const a = value.split(',');
                        for (let i = 0; i < a.length; i++)
                        {
                            const b = a[i].split(":");
                            const name = b[0];
                            const val = b[1];
                            let selected = annotations[name];
                            if(!selected)
                            {
                                selected = [];
                                annotations[name] = selected;
                            }

                            selected.push(val);
                        }
                        initValues[key] = annotations;
                    }
                    else {
                        initValues[key] = value;
                    }

                });

                // apply any URL parameters to the initial values
                Ext4.apply(initValues, this.getInitialValuesFromUrlParams());

                // Initialize the form
                this.initPlotForm(initValues);
            }, this, false)
        });
    },

    queryContainerReplicateAnnotations : function(metrics) {
        this.metricPropArr = metrics.sort(function(a, b) {
            return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
        });
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetContainerReplicateAnnotations.api'),
            method: 'GET',
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response) {
                var annotationNodes = [];
                Ext4.iterate(response.replicateAnnotations, function(annotation)
                {
                    var annotValueNodes = [];
                    annotation.values.forEach(function(value){
                        var valueNode = {text: value, leaf: true, iconCls: 'tree-node-noicon', checked: false};
                        annotValueNodes.push(valueNode);
                    });
                    var annotNode = {text: annotation.name, expanded: true, iconCls: 'tree-node-noicon', children: annotValueNodes};
                    annotationNodes.push(annotNode);
                });
                this.replicateAnnotationsNodes = annotationNodes;

                // Load persisted plot options for logged in users.
                this.queryInitialPlotOptions();

            }, this, false),
            failure: LABKEY.Utils.getCallbackWrapper(function (response) {
                this.failureHandler(response);
            }, null, true)
        });
    },

    calculateStartDateByOffset : function() {
        if (this.dateRangeOffset > 0) {
            var startDateByOffset = this.maxAcquiredTime ? new Date(this.maxAcquiredTime) : new Date();
            startDateByOffset.setDate(startDateByOffset.getDate() - this.dateRangeOffset);
            return startDateByOffset;
        }

        return this.minAcquiredTime;
    },

    calculateEndDateByOffset : function() {
        if (this.dateRangeOffset > 0)
            return this.maxAcquiredTime ? this.maxAcquiredTime : new Date();

        return this.maxAcquiredTime;
    },

    initPlotForm : function(initValues) {
        // apply the initial values to the panel object so they are used in form field initialization
        Ext4.apply(this, initValues);

        // if we have a dateRangeOffset, we need to calculate the start and end date
        if (this.dateRangeOffset > -1) {
            this.startDate = this.formatDate(this.calculateStartDateByOffset());
            this.endDate = this.formatDate(this.calculateEndDateByOffset());
        }

        this.getExpRunRangeDetails();
        // We just finished loading the previously set options so clear any dirty state
        this.havePlotOptionsChanged = false;
    },

    getExpRunRangeDetails: function() {
        var urlParams = LABKEY.ActionURL.getParameters();
        this.showExpRunRange = parseInt(urlParams['RunId']) > 0;

        if (this.showExpRunRange) {
            this.getExperimentRunDetails(urlParams['RunId'])
        }
        else {
            // initialize the form panel toolbars and display the plot
            this.add(this.initPlotFormToolbars());
            this.displayTrendPlot();
        }
    },

    getExperimentRunDetails: function (runId) {
        var sql = "Select MIN(sf.AcquiredTime) AS StartDate,\n" +
                "       MAX(sf.AcquiredTime) AS EndDate,\n" +
                "       sf.ReplicateId.RunId.FileName\n" +
                "FROM targetedms.SampleFile sf\n" +
                "WHERE sf.ReplicateId.RunId ='" + runId + "'\n" +
                "GROUP BY sf.ReplicateId.RunId.FileName";

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            scope: this,
            success: function (response) {

                if (response.rows.length === 0) {
                    this.failureHandler({message: 'Could not find run ' + runId});
                }
                else {
                    let runDetails = response.rows[0];
                    this.expRunDetails = {};
                    this.expRunDetails['fileName'] = runDetails.FileName;
                    this.expRunDetails['startDate'] = runDetails.StartDate;
                    this.expRunDetails['endDate'] = runDetails.EndDate;

                    Ext4.apply(this, {
                        startDate: this.formatDate(this.expRunDetails['startDate']),
                        endDate: this.formatDate(this.expRunDetails['endDate']),
                        dateRangeOffset: -1
                    });

                    // initialize the form panel toolbars and display the plot
                    this.add(this.initPlotFormToolbars());
                    this.displayTrendPlot();
                }
            },
            failure: this.failureHandler
        });
    },

    initPlotFormToolbars: function () {
        // Build a single top region with three columns of controls for a more compact layout.
        const columnDefaults = {
            xtype: 'container',
            layout: { type: 'vbox', align: 'center' },
            defaults: {
                width: 380,
                // Add a little vertical spacing between stacked controls
                margin: '0 10 10 0'
            }
        };

        const col1 = Ext4.create('Ext.container.Container', Ext4.apply({ itemId: 'qc-controls-col-1' }, columnDefaults));
        // Primary controls: metrics and date range
        col1.add(this.getMetricCombo1());
        col1.add(this.getMetricCombo2());
        col1.add(this.getScaleCombo());

        if (this.metric) {
            //hiding the show All series in a single plot checkbox for run scoped metrics
            this.getPlotGroupRadioGroup().setVisible(this.getMetricPropsById(this.metric).precursorScoped);
        }

        // Create vertical line separator component
        const separator = {
            xtype: 'component',
            autoEl: {
                tag: 'div',
                style: 'width: 1px; background-color: #e0e0e0; height: 100%;'
            },
            width: 1,
            margin: '0 12px',
            // Ensure this separator does not expand like other flexed columns
            flex: 0
        };

        const col2 = Ext4.create('Ext.container.Container', Ext4.apply({ itemId: 'qc-controls-col-2' }, columnDefaults));
        // Plot configuration controls
        col2.add(this.getDateRangeCombo());
        // Show custom date range inline when selected  
        col2.add(this.getCustomDateRangeToolbar());
        col2.add(this.getGroupedXRadioGroup());
        col2.add(this.getExcludedReplicatesRadioGroup());

        const col3 = Ext4.create('Ext.container.Container', Ext4.apply({ itemId: 'qc-controls-col-3' }, columnDefaults));
        // Display toggles and filters
        col3.add(this.getPlotTypeOptions());
        col3.add(this.getTrailingRunsField());
        col3.add(this.getPlotGroupRadioGroup());
        col3.add(this.getExcludedPrecursorsRadioGroup());
        col3.add(this.getReferenceGuideSetRadioGroup());
        if (this.canUserEdit()) {
            const buttonContainer = Ext4.create('Ext.form.FieldContainer', {
                hideEmptyLabel: false,
                labelWidth: this.LABEL_WIDTH,
                items: [this.getGuideSetCreateButton()]
            });

            col3.add(buttonContainer);
        }
        if (this.replicateAnnotationsNodes.length > 0) {
            col2.add(this.getAnnotationFiltersToolbar());
            col2.add(this.getSelectedAnnotationsToolbar());
        }

        // Ensure single-plot checkbox visibility mirrors previous logic when metrics are run-scoped
        if (this.metric) {
            var showSinglePlot = this.getMetricPropsById(this.metric).precursorScoped;
            this.getPlotGroupRadioGroup().setVisible(showSinglePlot);
        }

        const columnsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
            ui: 'footer',
            cls: 'levey-jennings-toolbar',
            padding: 10,
            layout: { type: 'hbox', align: 'stretch' }, // Changed to stretch to make separators full height
            defaults: { flex: 1 },
            items: [col1, separator, col2, separator, col3] // Added separators between columns
        });
    
        return [
            { tbar: columnsToolbar },
            { tbar: this.getGuideSetMessageToolbar() },
            { tbar: this.getDateRangeErrorToolbar() },
            { tbar: this.getExperimentRunDateRangeToolbar() }
        ];
    },

    getTrailingRunsField: function () {
        if (!this.trailingRunsField) {
            this.trailingRunsField = Ext4.create('Ext.form.field.Number', {
                xtype : 'numberfield',
                fieldLabel: 'Trailing last',
                labelWidth: this.LABEL_WIDTH,
                enableKeyEvents: true,
                id : 'trailingRuns',
                value: this.trailingRuns ? this.trailingRuns : this.runs > 10 ? 10 : this.runs,
                hidden: true,
                activeError: '',
                allowDecimals: false,
                minValue: 2,
                listeners: {
                    scope: this,
                    change: function (cmp, newVal) {
                        this.trailingRuns = newVal;
                        this.havePlotOptionsChanged = true;
                        this.displayTrendPlot();
                    }
                }
            });
        }
        return this.trailingRunsField;
    },

    getPlotTypeOptions: function() {
        let plotTypeCheckBoxes = [];
        let selectedPlotTypes = [];

        Ext4.each(LABKEY.targetedms.QCPlotHelperBase.qcPlotTypes, function(plotType){
            plotTypeCheckBoxes.push({
                inputValue: plotType,
            });
            if (this.isPlotTypeSelected(plotType))  {
                selectedPlotTypes.push(plotType);
                this.getTrailingRunsField().hidden = !((plotType.indexOf("Trailing Mean") > -1 || plotType.indexOf("Trailing CV") > -1));
            }

        }, this);

        return {
            xtype: 'plottype-checkcombo',
            labelWidth: this.LABEL_WIDTH,
            id: 'qc-plot-type-with-y-options',
            fieldLabel: 'Plot types',
            expandToFitContent: true,
            addAllSelector: false,
            queryMode: 'local',
            store: Ext4.create('Ext.data.Store', {
                fields: ['inputValue'],
                data: plotTypeCheckBoxes,
            }),
            displayField: 'inputValue',
            valueField: 'inputValue',
            value: selectedPlotTypes,
            listeners: {
                scope: this,
                change: function(cmp, newVal) {
                    var newValues = newVal;
                    this.plotTypes = newValues ? Ext4.isArray(newValues) ? newValues : [newValues] : [];

                    if (this.trailingRuns === undefined || this.trailingRuns === null) {
                        this.trailingRuns = 10;
                    }
                    this.getTrailingRunsField().setVisible(newValues.indexOf("Trailing Mean") > -1 || newValues.indexOf("Trailing CV") > -1);
                    this.havePlotOptionsChanged = true;
                    this.displayTrendPlot();
                }
            }
        }
    },

    getAnnotationFiltersToolbar : function() {
        if (!this.annotationFiltersToolbar) {
            this.annotationFiltersToolbar = Ext4.create('Ext.form.FieldContainer', {
                fieldLabel: 'Replicate filter',
                labelWidth: this.LABEL_WIDTH,
                items: [
                    this.getAnnotationListTree(),
                    {
                        xtype: 'container',
                        layout: {
                            type: 'hbox',
                            pack: 'end' // Right-align the buttons
                        },
                        items: [
                            this.getApplyAnnotationFiltersButton(),
                            {xtype: 'tbspacer', width: 5},
                            this.getClearAnnotationFiltersButton()
                        ]
                    }
                ]
            });

            if(this.replicateAnnotationsNodes.length > 0) {
                var annotationsTree = this.getAnnotationListTree();
                var rootNode = annotationsTree.getRootNode();
                var annotations = this.selectedAnnotations;
                if(Object.keys(annotations).length > 0) {
                    rootNode.cascadeBy(function (node) {
                        if (!node.isRoot() && !node.isLeaf()) {
                            var annotationName = node.get('text');
                            var selected = annotations[annotationName];
                            if (selected) {
                                for (var i = 0; i < selected.length; i++) {
                                    var child = node.findChild('text', selected[i]);
                                    if (child) {
                                        child.set('checked', true);
                                    }
                                }
                            }
                        }
                    });
                    this.clearAnnotationFiltersButton.show();
                    // If the tree is currently collapsed, keep buttons hidden
                    if (this.getAnnotationListTree().collapsed) {
                        this.clearAnnotationFiltersButton.hide();
                        this.getApplyAnnotationFiltersButton().hide();
                    }
                }
            }
            this.annotationFiltersToolbar.setVisible(this.replicateAnnotationsNodes.length > 0);
        }

        return this.annotationFiltersToolbar;
    },

    getSelectedAnnotationsToolbar: function() {
        if (!this.selectedAnnotationsToolbar) {
            this.selectedAnnotationsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: '0 0 0 0',
                layout: {pack: 'center'},
                hidden: true,
                items: []
            });

            if(Object.keys(this.selectedAnnotations).length > 0) {
                this.updateSelectedAnnotationsToolbar();
            }
        }
        return this.selectedAnnotationsToolbar;
    },

    getCustomDateRangeToolbar : function() {
        if (!this.customDateRangeToolbar) {
            // Render the custom range controls inline beneath the date range combo,
            // left-aligned so they stay within the same column and don't overlap
            // with the label column.
            this.customDateRangeToolbar = Ext4.create('Ext.form.FieldContainer', {
                fieldLabel: 'Custom dates',
                labelWidth: this.LABEL_WIDTH,
                hidden: this.dateRangeOffset > -1,
                layout: { type: 'hbox', align: 'middle' },
                items: [
                    this.getStartDateField(),
                    {xtype: 'tbspacer', width: '3%'},
                    {xtype: 'label', text: ' to ', width: '3%'},
                    {xtype: 'tbspacer', width: '3%'},
                    this.getEndDateField()
                ]
            });
        }

        return this.customDateRangeToolbar;
    },

    getGuideSetMessageToolbar : function() {
        if (!this.guideSetMessageToolbar) {
            this.guideSetMessageToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'guideset-toolbar-msg',
                hidden: true,
                layout: { pack: 'center' },
                items: [{
                    xtype: 'box',
                    itemId: 'GuideSetMessageToolBar',
                    html: 'Please click and drag in the plot to select the guide set training date range.'
                }]
            });
        }

        return this.guideSetMessageToolbar;
    },

    getDateRangeErrorToolbar : function() {
        if (!this.dateRangeErrorToolbar) {
            this.dateRangeErrorToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                hidden: true,
                layout: { pack: 'center' },
                items: [{
                    xtype: 'label',
                    id: 'DateRangeErrorBar',
                    cls: 'labkey-error',
                    text: 'Please correct the date range. Check the start and end dates are valid.'
                }]
            });
        }

        return this.dateRangeErrorToolbar;
    },

    getExperimentRunDateRangeToolbar : function() {
        if (!this.experimentRunDateRangeToolbar) {
            var hidden = !this.showExpRunRange;
            var returnUrl = LABKEY.ActionURL.getReturnUrl();
            var htmlStr = this.showExpRunRange
                    ? "<a href=" + Ext4.String.htmlEncode(returnUrl) + ">"
                    + Ext4.String.htmlEncode(this.expRunDetails.fileName) + " : "
                    + Ext4.String.htmlEncode(this.formatDate(this.expRunDetails.startDate, false)) + " through "
                    + Ext4.String.htmlEncode(this.formatDate(this.expRunDetails.endDate, false))
                    + "</a>"
                    : "";
            this.experimentRunDateRangeToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'expDateRange-toolbar-msg',
                hidden: hidden,
                layout: { pack: 'center' },
                items: [{
                    xtype: 'box',
                    html: htmlStr
                }]
            });
        }

        return this.experimentRunDateRangeToolbar;
    },

    isValidQCPlotType: function(plotType) {
        var valid = false;
        Ext4.each(LABKEY.targetedms.QCPlotHelperBase.qcPlotTypes, function(type){
            if (plotType === type) {
                valid = true;
            }
        });
        return valid;
    },

    getInitialValuesFromUrlParams : function() {
        var urlParams = LABKEY.ActionURL.getParameters(),
            paramValues = {},
            alertMessage = '', sep = '',
            paramValue,
            metric;

        paramValue = urlParams['metric'];
        if (paramValue !== undefined) {
            metric = this.validateMetricId(paramValue);
            if(metric == null) {
                alertMessage += "Invalid metric, reverting to default metric.";
                sep = ' ';
            }
            else {
                paramValues['metric'] = metric;
            }
        }

        if (urlParams['startDate'] !== undefined) {
            paramValue = new Date(urlParams['startDate']);
            if(paramValue === "Invalid Date") {
                alertMessage += sep + "Invalid Start Date, reverting to default start date.";
                sep = ' ';
            }
            else {
                paramValues['dateRangeOffset'] = -1; // force to custom date range selection
                paramValues['startDate'] = this.formatDate(new Date(urlParams['startDate']));
            }
        }

        if (urlParams['endDate'] !== undefined) {
            paramValue = new Date(urlParams['endDate']);
            if(paramValue === "Invalid Date") {
                alertMessage += sep + "Invalid End Date, reverting to default end date.";
            }
            else {
                paramValues['dateRangeOffset'] = -1; // force to custom date range selection
                paramValues['endDate'] = this.formatDate(new Date(urlParams['endDate']));
            }
        }

        paramValue = urlParams['plotTypes'];
        if (paramValue !== undefined) {
            var plotTypes = [];
            if (!Ext4.isArray(paramValue))
                paramValue = paramValue.split(',');

            Ext4.each(paramValue, function (value) {
                if (this.isValidQCPlotType(value.trim()))
                    plotTypes.push(value.trim());
            }, this);


            if (plotTypes.length === 0) {
                alertMessage += sep + "Invalid Plot Type, reverting to default plot type.";
            }
            else {
                paramValues['plotTypes'] = plotTypes;
            }
        }

        if (alertMessage.length > 0) {
            LABKEY.Utils.alert('Invalid URL Parameter(s)', alertMessage);
        }
        else if (Object.keys(paramValues).length > 0) {
            this.havePlotOptionsChanged = true;
            return paramValues;
        }

        return null;
    },

    validateMetricId : function(id) {
        // convert id to an integer, handling a parsing failure gracefully
        id = parseInt(id);
        for (let i = 0; i < this.metricPropArr.length; i++) {
            if (this.metricPropArr[i].id === id) {
                return this.metricPropArr[i].id;
            }
        }
        return null;
    },

    getYAxisOptions: function () {
        return {
            fields: ['value', 'display'],
            data: [['linear', 'Linear'], ['log', 'Log'], ['percentDeviation', 'Percent of Mean'], ['standardDeviation', 'Standard Deviations']]
        }
    },

    getScaleCombo : function() {
        if (!this.scaleCombo) {
            this.scaleCombo = Ext4.create('Ext.form.field.ComboBox', {
                id: 'scale-combo-box',
                fieldLabel: 'Y-axis scale',
                labelWidth: this.LABEL_WIDTH,
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.ArrayStore', this.getYAxisOptions()),
                valueField: 'value',
                displayField: 'display',
                value: this.yAxisScale,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal) {
                        this.yAxisScale = newVal;
                        this.havePlotOptionsChanged = true;

                        // call processPlotData instead of renderPlots so that we recalculate min y-axis scale for log
                        this.setLoadingMsg();
                        this.processPlotData();
                    }
                }
            });
        }

        return this.scaleCombo;
    },

    getDateRangeCombo : function() {
        if (!this.dateRangeCombo) {
            this.dateRangeCombo = Ext4.create('Ext.form.field.ComboBox', {
                id: 'daterange-combo-box',
                labelWidth: this.LABEL_WIDTH,
                fieldLabel: 'Date range',
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.ArrayStore', {
                    fields: ['value', 'display'],
                    data: [
                        [0, 'All dates'],
                        [7, 'Last 7 days'],
                        [15, 'Last 15 days'],
                        [30, 'Last 30 days'],
                        [90, 'Last 90 days'],
                        [180, 'Last 180 days'],
                        [365, 'Last 365 days'],
                        [-1, 'Custom range']
                    ]
                }),
                valueField: 'value',
                displayField: 'display',
                value: this.dateRangeOffset,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal) {
                        this.dateRangeOffset = newVal;
                        this.havePlotOptionsChanged = true;

                        var showCustomRangeItems = this.dateRangeOffset === -1;
                        this.getCustomDateRangeToolbar().setVisible(showCustomRangeItems);

                        if (!showCustomRangeItems) {
                            this.getDateRangeErrorToolbar().hide();
                            // either use the min and max values based on the data
                            // or calculate range based on today's date and the offset
                            this.startDate = this.formatDate(this.calculateStartDateByOffset());
                            this.endDate = this.formatDate(this.calculateEndDateByOffset());

                            this.displayTrendPlot();
                        }
                        else {
                            this.applyCustomDateRange();
                        }
                    }
                }
            });
        }

        return this.dateRangeCombo;
    },

    createDateField: function (config) {
        var defaultConfig = {
            width: '45%',
            allowBlank: false,
            format: 'Y-m-d',
            listeners: {
                scope: this,
                change: this.applyCustomDateRange
            }
        };

        return Ext4.create('Ext.form.field.Date', Ext4.apply(defaultConfig, config));
    },

    getStartDateField: function () {
        if (!this.startDateField) {
            this.startDateField = this.createDateField({
                id: 'start-date-field',
                value: this.startDate
            });
        }
        return this.startDateField;
    },

    getEndDateField: function () {
        if (!this.endDateField) {
            this.endDateField = this.createDateField({
                id: 'end-date-field',
                value: this.showExpRunRange ? this.formatDate(this.expRunDetails.endDate, false) : this.endDate
            });
        }
        return this.endDateField;
    },

    assignDefaultMetricIfNull: function () {
        if (this.metric == null || isNaN(Number(this.metric)) || !this.getMetricPropsById(this.metric)) {
            this.metric = null;
            for (let i = 0; i < this.metricPropArr.length; i++) {
                if (this.metricPropArr[i].name === 'Retention Time') {
                    this.metric = this.metricPropArr[i].id;
                }
            }
            // Fall back on the first one
            if (!this.metric && this.metricPropArr.length > 0) {
                this.metric = this.metricPropArr[0].id;
            }
        }
        if (this.metric2 && !this.getMetricPropsById(this.metric2)) {
            this.metric2 = null;
        }
    },

    getSecondMetricList : function() {
        const primaryMetric = this.getMetricPropsById(this.metric);
        const subset = this.metricPropArr.filter(function(metric) {
            return !primaryMetric ||
                    (primaryMetric.precursorScoped === metric.precursorScoped && primaryMetric.id !== metric.id);
        });

        return [{
            // It's easier to use 0 to avoid ambiguity of null vs not defined in JSON calls
            id: 0,
            name: this.noSecondMetricText
        }, ...subset];
    },

    createMetricCombo : function(primary) {

        let data;
        if (primary) {
            data = this.metricPropArr;
        }
        else {
            data = this.getSecondMetricList();
        }

        this.assignDefaultMetricIfNull();

        return Ext4.create('Ext.form.field.ComboBox', {
            id: 'metric-type-field' + (primary ? '1' : '2'),
            fieldLabel: primary ? 'Y-axis left' : 'Y-axis right',
            labelWidth: this.LABEL_WIDTH,
            triggerAction: 'all',
            queryMode: 'local',
            store: Ext4.create('Ext.data.Store', {
                fields: ['id', 'name'],
                data: data
            }),
            valueField: 'id',
            displayField: 'name',
            tpl : '<tpl for="."><li role="option" style="min-height: 1.75em;" class="x4-boundlist-item">{name:htmlEncode}</li></tpl>',
            value: primary ? this.metric : this.metric2,
            forceSelection: true,
            allowBlank: !primary,
            emptyText: primary ? 'No metric' : 'No second metric',
            editable: false,
            listeners: {
                scope: this,
                change: function(cmp, newVal) {
                    if (primary) {
                        this.metric = newVal;

                        const filteredMetrics = this.getSecondMetricList();
                        this.getMetricCombo2().getStore().loadData(filteredMetrics);
                        const metric2 = this.metric2;
                        const found = filteredMetrics.find(function(metric) {
                            return metric.id === metric2;
                        })
                        if (!found) {
                            this.getMetricCombo2().setValue(null);
                        }
                    }
                    else {
                        this.metric2 = newVal;
                    }
                    this.havePlotOptionsChanged = true;
                    // Update single-plot checkbox visibility directly in the 3-column layout
                    var showAllSeriesCheckBox = this.getMetricPropsById(this.metric).precursorScoped;
                    this.getPlotGroupRadioGroup().setVisible(showAllSeriesCheckBox);

                    if (this.filterQCPoints) {
                        this.resetFilterPointsIndices();
                    }
                    this.displayTrendPlot();
                }
            }
        });
    },

    getMetricCombo1 : function() {
        if (!this.metricField) {
            this.metricField = this.createMetricCombo(true);
        }

        return this.metricField;
    },

    getMetricCombo2 : function() {
        if (!this.metricField2) {
            this.metricField2 = this.createMetricCombo(false);
        }

        return this.metricField2;
    },

    getAnnotationListTree : function() {
        if (!this.annotationFiltersField) {
            const store = Ext4.create('Ext.data.TreeStore', {
                root: {expanded: false, children: this.replicateAnnotationsNodes},
            });

            this.annotationFiltersField = Ext4.create('Ext.tree.Panel', {
                id: 'annotation-filter-field',
                height: 150,
                title: 'Expand to select annotations',
                store: store,
                rootVisible: false,
                titleCollapse: true,
                collapsed: true,
                collapsible: true,
                header: { style: 'background-color: #ffffff' },
                useArrows: true,
                lines: false,
                listeners: {
                    scope: this,
                    expand: function() {
                        // Show Apply button when expanded
                        this.getApplyAnnotationFiltersButton().show();
                        // Only show Clear button when there are selected annotations
                        if (Object.keys(this.selectedAnnotations || {}).length > 0) {
                            this.getClearAnnotationFiltersButton().show();
                        }
                    },
                    collapse: function() {
                        // Hide both buttons when collapsed
                        this.getApplyAnnotationFiltersButton().hide();
                        this.getClearAnnotationFiltersButton().hide();
                    }
                }
            });

            this.getApplyAnnotationFiltersButton().hide();
            this.getClearAnnotationFiltersButton().hide();
        }

        return this.annotationFiltersField;
    },

    getApplyAnnotationFiltersButton : function() {
        if (!this.applyAnnotationFiltersButton) {
            this.applyAnnotationFiltersButton = Ext4.create('Ext.button.Button', {
                text: 'Apply',
                handler: this.applyAnnotationFiltersBtnClick,
                scope: this
            });
        }

        return this.applyAnnotationFiltersButton;
    },

    getClearAnnotationFiltersButton : function() {
        if (!this.clearAnnotationFiltersButton) {
            this.clearAnnotationFiltersButton = Ext4.create('Ext.button.Button', {
                text: 'Clear',
                handler: this.clearAnnotationFiltersBtnClick,
                scope: this,
                hidden: true
            });
        }

        return this.clearAnnotationFiltersButton;
    },

    getGroupedXRadioGroup : function() {
        if (!this.groupedXRadioGroup) {
            this.groupedXRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                id: 'grouped-x-field',
                fieldLabel: 'X-axis grouping',
                labelWidth: this.LABEL_WIDTH,
                columns: 2,
                vertical: false,
                items: [
                    { boxLabel: 'per replicate', id: 'x-axis-grouping-replicate', name: 'xAxisGrouping', inputValue: 'replicate', checked: this.groupedX === false },
                    { boxLabel: 'per date', id: 'x-axis-grouping-date', name: 'xAxisGrouping', inputValue: 'date', checked: this.groupedX === true }
                ],
                listeners: {
                    scope: this,
                    change: function(group, newValue) {
                        var val = newValue && (newValue.xAxisGrouping || newValue['xAxisGrouping']);
                        var groupByDate = val === 'date' || (val === true); // fallback safety
                        this.groupedX = groupByDate;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.groupedXRadioGroup;
    },

    getPlotGroupRadioGroup : function() {
        if (!this.plotGroupRadioGroup) {
            this.plotGroupRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                id: 'peptides-single-plot',
                labelWidth: this.LABEL_WIDTH,
                fieldLabel: 'Plots',
                columns: 2,
                vertical: false,
                items: [
                    { boxLabel: 'per precursor', name: 'showPlots', id: 'plots-per-precursor', inputValue: 'per-precursor', checked: this.singlePlot === false },
                    { boxLabel: 'combined', name: 'showPlots', id: 'plots-combined', inputValue: 'combined', checked: this.singlePlot === true }
                ],
                listeners: {
                    scope: this,
                    change: function(group, newValue) {
                        var val = newValue && (newValue.showPlots || newValue['showPlots']);
                        var combined = val === 'combined' || (val === true); // fallback safety
                        this.singlePlot = combined;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.setLoadingMsg();
                        this.processPlotData();
                    }
                }
            });
        }

        return this.plotGroupRadioGroup;
    },

    getExcludedReplicatesRadioGroup : function() {
        if (!this.excludedReplicatesRadioGroup) {
            this.excludedReplicatesRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                id: 'show-excluded-points',
                fieldLabel: 'Excluded replicates',
                labelWidth: this.LABEL_WIDTH,
                columns: 2,
                vertical: false,
                items: [
                    { boxLabel: 'show', id: 'excluded-replicates-show', name: 'excludedSamples', inputValue: 'show', checked: this.showExcluded === true },
                    { boxLabel: 'hide', id: 'excluded-replicates-hide', name: 'excludedSamples', inputValue: 'hide', checked: this.showExcluded === false }
                ],
                listeners: {
                    scope: this,
                    change: function(group, newValue) {
                        var val = newValue && (newValue.excludedSamples || newValue['excludedSamples']);
                        var newShow = val === 'show' || (val === true); // fallback safety
                        this.showExcluded = newShow;
                        this.havePlotOptionsChanged = true;

                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.excludedReplicatesRadioGroup;
    },

    getExcludedPrecursorsRadioGroup : function() {
        if (!this.excludedPrecursorsRadioGroup) {
            this.excludedPrecursorsRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                id: 'show-excluded-precursors',
                fieldLabel: 'Excluded precursors',
                labelWidth: this.LABEL_WIDTH,
                columns: 2,
                vertical: false,
                items: [
                    { boxLabel: 'show', id: 'excluded-precursors-show', name: 'excludedPrecursors', inputValue: 'show', checked: this.showExcludedPrecursors === true },
                    { boxLabel: 'hide', id: 'excluded-precursors-hide', name: 'excludedPrecursors', inputValue: 'hide', checked: this.showExcludedPrecursors === false }
                ],
                listeners: {
                    scope: this,
                    change: function(group, newValue) {
                        const val = newValue && (newValue.excludedPrecursors || newValue['excludedPrecursors']);
                        this.showExcludedPrecursors = val === 'show' || (val === true);
                        this.havePlotOptionsChanged = true;

                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.excludedPrecursorsRadioGroup;
    },

    resetFilterPointsIndices: function() {
        if (this.filterPoints) {
            this.filterPoints = undefined;
        }
    },

    getReferenceGuideSetRadioGroup : function() {
        if (!this.referenceGuideSetRadioGroup) {
            this.referenceGuideSetRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                id: 'show-oorange-gs',
                fieldLabel: 'Reference guide sets',
                labelWidth: this.LABEL_WIDTH,
                columns: 2,
                vertical: false,
                items: [
                    { boxLabel: 'always show', id: 'reference-guide-set-show', name: 'referenceGuideSets', inputValue: 'show', checked: this.showReferenceGS === true },
                    { boxLabel: 'when in date range', id: 'reference-guide-set-hide', name: 'referenceGuideSets', inputValue: 'hide', checked: this.showReferenceGS === false }
                ],
                listeners: {
                    scope: this,
                    change: function(group, newValue) {
                        var val = newValue && (newValue.referenceGuideSets || newValue['referenceGuideSets']);
                        var newShow = val === 'show' || (val === true);
                        this.showReferenceGS = newShow;
                        this.havePlotOptionsChanged = true;

                        if (this.showExpRunRange) {
                            if (newShow) {
                                this.resetFilterPointsIndices();
                                Ext4.apply(this, {
                                    startDate: this.formatDate(this.expRunDetails.startDate),
                                    endDate: this.formatDate(this.expRunDetails.endDate),
                                    dateRangeOffset: -1
                                });
                            }
                            else {
                                this.getStartDateField().setValue(this.formatDate(this.expRunDetails.startDate, false));
                            }
                        }
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.referenceGuideSetRadioGroup;
    },

    getGuideSetCreateButton : function() {
        if (!this.createGuideSetToggleButton) {
            this.createGuideSetToggleButton = Ext4.create('Ext.button.Button', {
                text: 'Create Guide Set',
                tooltip: 'Enable/disable guide set creation mode. Supported for plots not grouped by date, when ' + LABKEY.targetedms.QCPlotHelperBase.maxPointsPerSeries + ' or fewer samples are shown',
                disabled: !this.canCreateGuideSetFromPlot(),
                enableToggle: true,
                handler: function(btn) {
                    this.setBrushingEnabled(btn.pressed);
                },
                scope: this
            });
        }

        return this.createGuideSetToggleButton;
    },

    canCreateGuideSetFromPlot : function() {
        return !(this.groupedX || this.isMultiSeries() || this.showExpRunRange || !this.showDataPoints);
    },

    setBrushingEnabled : function(enabled) {
        // we don't currently allow creation of guide sets in grouped x-axis mode, multi series mode or when showingExpRunRange
        this.getGuideSetCreateButton().setDisabled(!this.canCreateGuideSetFromPlot());

        this.enableBrushing = enabled;
        this.clearPlotBrush();
        this.setPlotBrushingDisplayStyle();
        this.toggleGuideSetMsgDisplay();
        this.getGuideSetCreateButton().toggle(enabled);
    },

    setLoadingMsg : function() {
        Ext4.get(this.plotDivId).mask("Loading...");
    },

    displayTrendPlot: function() {
        hopscotch.getCalloutManager().removeAllCallouts();

        this.setBrushingEnabled(false);
        this.updateSelectedAnnotations();
        this.setLoadingMsg();
        this.getDistinctPrecursors();
    },

    getDistinctPrecursors: function() {

        this.assignDefaultMetricIfNull();

        var metricProps = this.getMetricPropsById(this.metric);

        if (metricProps) {
            this.getAnnotationData();
        }
        else {
            Ext4.get(this.plotDivId).update("There are no enabled QC Metric Configurations.");
        }
    },

    setPrecursorsForPage: function(plotDataRowsBySeriesLabel) {
        if (Ext4.isNumeric(LABKEY.ActionURL.getParameter('qcPlots.offset'))) {
            this.qcPlotsOffset = parseInt(LABKEY.ActionURL.getParameter('qcPlots.offset'));
        } else {
            this.qcPlotsOffset = 0;
        }
        this.pagingStartIndex = this.qcPlotsOffset;

        if (this.pagingStartIndex < 0)
            this.pagingStartIndex = 0;
        else if (this.pagingStartIndex > plotDataRowsBySeriesLabel.length)
            this.pagingStartIndex = plotDataRowsBySeriesLabel.length - this.maxCount;

        this.pagingEndIndex = Math.min(this.pagingStartIndex + this.maxCount, plotDataRowsBySeriesLabel.length);

        this.precursors = [];

        Ext4.iterate(plotDataRowsBySeriesLabel, function(plotDataRow) {
            this.precursors.push(plotDataRow['SeriesLabel']);
        }, this);

        this.updatePaginationDiv(plotDataRowsBySeriesLabel.length);
    },

    updatePaginationDiv: function(numOfPrecursors) {
        var exceedsPageLimit = numOfPrecursors > this.maxCount;

        var displayPagination = exceedsPageLimit || this.qcPlotsOffset;
        var displayHtml = "", sep = "";
        if (displayPagination) {
            displayHtml += this.getPaginationTxt(numOfPrecursors);
            sep = "&nbsp;&nbsp;&nbsp;";
            displayHtml += sep + this.getPaginationBtns(numOfPrecursors);
        }
        Ext4.get(this.plotPaginationDivId).update(displayHtml);
        Ext4.get(this.plotPaginationDivId).setStyle("display", displayPagination ? "block" : "none");

        this.attachPagingListeners(numOfPrecursors);
    },

    getPaginationTxt: function(numOfPrecursors) {
        return "Showing <b>" + (this.pagingStartIndex+1) + " - " + this.pagingEndIndex + "</b> of <b>"
                + numOfPrecursors + "</b> precursors";
    },

    getPaginationBtns: function(numOfPrecursors) {
        var btnHtml = '';

        btnHtml += '<span class="qc-paging-prev ' + (this.pagingStartIndex > 0 ? 'qc-paging-icon-enabled' : 'qc-paging-icon-disabled')
                + '"><i class="fa fa-angle-left"></i></span>';

        btnHtml += '<span class="qc-paging-next ' + (this.pagingEndIndex < numOfPrecursors ? 'qc-paging-icon-enabled' : 'qc-paging-icon-disabled')
                + '"><i class="fa fa-angle-right"></i></span>';

        return btnHtml;
    },

    attachPagingListeners: function(numOfPrecursors) {
        var prevBtn = Ext4.DomQuery.selectNode('.qc-paging-prev');
        if (prevBtn && this.pagingStartIndex > 0) {
            Ext4.get(prevBtn).on('click', function() {
                window.location = LABKEY.ActionURL.buildURL(LABKEY.ActionURL.getController(), LABKEY.ActionURL.getAction(), null,
                    Ext4.apply(LABKEY.ActionURL.getParameters(), {'qcPlots.offset': Math.max(0, this.pagingStartIndex - this.maxCount)}));
            }, this);
        }

        var nextBtn = Ext4.DomQuery.selectNode('.qc-paging-next');
        if (nextBtn && this.pagingEndIndex < numOfPrecursors) {
            Ext4.get(nextBtn).on('click', function() {
                window.location = LABKEY.ActionURL.buildURL(LABKEY.ActionURL.getController(), LABKEY.ActionURL.getAction(), null,
                    Ext4.apply(LABKEY.ActionURL.getParameters(), {'qcPlots.offset': this.pagingEndIndex}));
            }, this);
        }
    },

    getAnnotationData: function() {
        this.setLoadingMsg();

        var config = this.getReportConfig();

        var annotationSql = "SELECT qca.Date, qca.Description, qca.Created, qca.CreatedBy.DisplayName, qcat.Name, qcat.Color FROM qcannotation qca JOIN qcannotationtype qcat ON qcat.Id = qca.QCAnnotationTypeId";

        // Filter on start/end dates
        var separator = " WHERE ";
        if (config.StartDate) {
            annotationSql += separator + "CAST(Date AS Date) >= '" + config.StartDate + "'";
            separator = " AND ";
        }
        if (config.EndDate) {
            annotationSql += separator + "CAST(Date AS Date) <= '" + config.EndDate + "'";
        }

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: annotationSql,
            sort: 'Date',
            containerFilter: LABKEY.Query.containerFilter.currentPlusProjectAndShared,
            scope: this,
            success: this.processAnnotationData,
            failure: this.failureHandler
        });
    },

    processAnnotationData: function(data) {
        if (data) {
            this.annotationData = data.rows;
            this.annotationShape = LABKEY.vis.Scale.Shape()[4]; // 0: circle, 1: triangle, 2: square, 3: diamond, 4: X

            var dateCount = {};
            this.legendData = [];

            // if more than one type of legend present, add a legend header for annotations
            if (this.annotationData.length > 0 && (this.singlePlot || this.showMeanCUSUMPlot() || this.showVariableCUSUMPlot())) {
                    this.legendData.push({
                        text: 'Annotations',
                        separator: true
                    });
            }

        for (let i = 0; i < this.annotationData.length; i++)
        {
                const annotation = this.annotationData[i];
                const annotationDate = this.formatDate(new Date(annotation['Date']), !this.groupedX);

                    // track if we need to stack annotations that fall on the same date
                    if (!dateCount[annotationDate]) {
                        dateCount[annotationDate] = 0;
                    }
                    annotation.yStepIndex = dateCount[annotationDate];
                    dateCount[annotationDate]++;

                    // get unique annotation names and colors for the legend
                if (Ext4.Array.pluck(this.legendData, "text").indexOf(annotation['Name']) === -1) {
                        this.legendData.push({
                            text: annotation['Name'],
                            color: '#' + annotation['Color'],
                            shape: this.annotationShape
                        });
                    }
                }

            this.getPlotsData();
        }
    },

    getExportSVGStr: function(btn, extraMargin) {
        var svgStr = this.callParent([btn, extraMargin]);

        // issue 25066: pdf export has artifact of the brush resize handlers
        svgStr = svgStr.replace('class="e-resize-handle-rect"', 'class="e-resize-handle-rect" visibility="hidden"');
        svgStr = svgStr.replace('class="w-resize-handle-rect"', 'class="w-resize-handle-rect" visibility="hidden"');

        return svgStr;
    },

    showInvalidLogMsg : function(id, toShow) {
        if (toShow) {
            Ext4.get(id).update("<span style='font-style: italic;'>Log scale invalid for values &le; 0. "
                    + "Reverting to linear y-axis scale.</span>");
        }
    },

    legendMouseOver : function(data, item) {
        if (data.name) {
            // in the multi series case, the name has the series label appended, so use the hoverText instead
            this.highlightFragmentSeries(data.hoverText);
        }
    },

    pathMouseOver : function(event, pathData, layerSel, path, valueName, config) {
        if (pathData.group) {
            this.highlightFragmentSeries(pathData.group);
        }
    },

    plotPointMouseOver : function(event, row, layerSel, point, valueName, plotConfig) {
        var showHoverTask = new Ext4.util.DelayedTask(),
            metricProps = this.getMetricPropsById(row.MetricId),
            me = this;

        let panelY = me.canUserEdit() ? -375 : -270;
        if (valueName === "TrailingMean" || valueName === "TrailingCV") {
            panelY = panelY + 200;
        }

        let trailingRuns = this.trailingRuns;
        let trailingStartDate = Ext4.util.Format.date(row['TrailingStartDate'], 'Y-m-d H:i');
        let trailingEndDate = Ext4.util.Format.date(row['fullDate'], 'Y-m-d H:i');

        showHoverTask.delay(500, function() {
            var calloutMgr = hopscotch.getCalloutManager(),
                hopscotchId = Ext4.id(),
                contentDivId = Ext4.id(),
                shiftLeft = (event.clientX || event.x) > (Ext4.getBody().getWidth() / 2),
                config = {
                    id: hopscotchId,
                    showCloseButton: true,
                    bubbleWidth: 450,
                    placement: 'top',
                    xOffset: shiftLeft ? -428 : -53,
                    arrowOffset: shiftLeft ? 410 : 35,
                    yOffset: panelY,
                    target: point,
                    content: '<div id="' + contentDivId + '"></div>',
                    onShow: function() {
                        me.attachHopscotchMouseClose();

                        Ext4.create('LABKEY.targetedms.QCPlotHoverPanel', {
                            renderTo: contentDivId,
                            pointData: row,
                            valueName: valueName,
                            trailingRuns: trailingRuns,
                            trailingStartDate: trailingStartDate,
                            trailingEndDate: trailingEndDate,
                            metricProps: metricProps,
                            canEdit: me.canUserEdit(),
                            listeners: {
                                scope: me,
                                close: function() {
                                    calloutMgr.removeAllCallouts();
                                }
                            }
                        });
                    }
                };

            calloutMgr.removeAllCallouts();
            calloutMgr.createCallout(config);
        });

        // cancel the hover details show event if the user was just
        // passing over the point without stopping for X amount of time
        Ext4.get(point).on('mouseout', function() {
            showHoverTask.cancel();
        }, this);

        // for the combined / single plot case, we want to have point hover highlight the given series
        // by using opacity to "push" the other points and lines to the background
        if (plotConfig.properties.combined) {
            this.highlightFragmentSeries(row.fragment);
        }
    },

    plotPointMouseOut : function(event, row, layerSel, valueName, plotConfig) {
        d3.selectAll('.point path').attr('fill-opacity', 1).attr('stroke-opacity', 1);
        d3.selectAll('path.line').attr('fill-opacity', 1).attr('stroke-opacity', 1);
        d3.selectAll('.legend .legend-item').attr('fill-opacity', 1).attr('stroke-opacity', 1);
    },

    highlightFragmentSeries : function(fragment) {
        var points = d3.selectAll('.point path');
        var pointOpacityAcc = function(d) { return d.fragment === undefined || d.fragment === null || d.fragment === fragment ? 1 : 0.1 };
        points.attr('fill-opacity', pointOpacityAcc).attr('stroke-opacity', pointOpacityAcc);

        var hasYRightMetric = this.metric2;
        var lines = d3.selectAll('path.line');
        var lineOpacityAcc = function(d) { return d.group === undefined || d.group === null || d.group.indexOf(fragment + (hasYRightMetric ? '|' : '')) === 0 ? 1 : 0.1 };
        lines.attr('fill-opacity', lineOpacityAcc).attr('stroke-opacity', lineOpacityAcc);

        var legendItems = d3.selectAll('.legend .legend-item');
        var legendOpacityAcc = function(d) {
            if (!d.name) return 1;
            return d.name.indexOf(fragment + (hasYRightMetric ? '|' : '')) === 0 ? 1 : 0.1
        };
        legendItems.attr('fill-opacity', legendOpacityAcc).attr('stroke-opacity', legendOpacityAcc);
    },

    attachHopscotchMouseClose: function() {
        var closeTask = new Ext4.util.DelayedTask();
        var h = Ext4.select('.hopscotch-bubble-container');

        // on mouseout call the delayed task to close the callout
        h.on('mouseout', function() {
            closeTask.delay(1000, function() {
                hopscotch.getCalloutManager().removeAllCallouts();
            });
        });

        // if the mouseover happens again for this element before the delay, cancel it to keep callout open
        h.on('mouseover', function() {
            closeTask.cancel();
        });
    },

    plotBrushStartEvent : function(plot) {
        this.clearPlotBrush(plot);
    },

    plotBrushEvent : function(extent, plot, layers) {
        Ext4.each(layers, function(layer){
            const points = layer.selectAll('.point path');
            if (points && points[0] && points[0].length > 0) {
                const colorAcc = function(d) {
                    const x = plot.scales.x.scale(d.seqValue);
                    d.isInSelection = (x > extent[0][0] && x < extent[1][0]);
                    return d.isInSelection ? 'rgba(20, 204, 201, 1)' : '#000000';
                };

                points.attr('fill', colorAcc).attr('stroke', colorAcc);
            }
        });
    },

    plotBrushEndEvent : function(data, extent, plot) {
        var selectedPoints = Ext4.Array.filter(data, function(point){ return point.isInSelection; });
        this.plotBrushSelection = {plot: plot, points: selectedPoints};

        // add the guide set create and cancel buttons over the brushed region
        var me = this;
        var xMid = extent[0][0] + (extent[1][0] - extent[0][0]) / 2;

        if (selectedPoints.length > 0) {
            var createBtn = this.createGuideSetSvgButton(plot, 'Create', xMid - 57, 50);
            createBtn.on('click', function() {
                me.createGuideSetBtnClick();
            });
        }

        var cancelBtn = this.createGuideSetSvgButton(plot, 'Cancel', xMid + 3, 49);
        cancelBtn.on('click', function () {
            me.clearPlotBrush(plot);
            plot.clearBrush();
            me.setBrushingEnabled(false);
        });

        this.bringSvgElementToFront(plot, "g.guideset-svg-button");
    },

    plotBrushClearEvent : function(data, plot) {
        this.plotBrushSelection = undefined;
    },

    canUserEdit : function() {
        return LABKEY.user.canInsert && LABKEY.user.canUpdate;
    },

    allowGuideSetBrushing : function() {
        return this.canUserEdit() && !this.groupedX && !this.isMultiSeries();
    },

    createGuideSetSvgButton : function(plot, text, xLeftPos, width) {
        var yRange = plot.scales.yLeft.range;
        var yTopPos = yRange[1] + (yRange[0] - yRange[1]) / 2 - 10;

        var svgBtn = this.getSvgElForPlot(plot).append('g')
                .attr('class', 'guideset-svg-button');

        svgBtn.append('rect')
                .attr('x', xLeftPos).attr('y', yTopPos).attr('rx', 5).attr('ry', 5)
                .attr('width', width).attr('height', 20)
                .style({'fill': '#ffffff', 'stroke': '#b4b4b4'});

        svgBtn.append('text').text(text)
                .attr('x', xLeftPos + 5).attr('y', yTopPos + 14)
                .style({'fill': '#126495', 'font-size': '10px', 'font-weight': 'bold', 'text-transform': 'uppercase'});

        return svgBtn;
    },

    setPlotBrushingDisplayStyle : function() {
        // hide the brushing related components for all plots if not in "create guide set" mode
        var displayStyle = this.enableBrushing ? 'inline' : 'none';
        d3.selectAll('.brush').style({'display': displayStyle});
        d3.selectAll('.x-axis-handle').style({'display': displayStyle});
    },

    clearPlotBrush : function(plot) {
        // clear any create/cancel buttons and brush areas from other plots
        if (this.plotBrushSelection) {
            this.getSvgElForPlot(this.plotBrushSelection.plot).selectAll(".guideset-svg-button").remove();

            if (this.plotBrushSelection.plot !== plot) {
                this.plotBrushSelection.plot.clearBrush();
            }
        }
    },

    getSvgElForPlot : function(plot) {
        return d3.select('#' + plot.renderTo + ' svg');
    },

    toggleGuideSetMsgDisplay : function() {
        var toolbarMsg = this.down('#GuideSetMessageToolBar');
        if (toolbarMsg) {
            toolbarMsg.up('toolbar').setVisible(this.enableBrushing);
        }
    },

    highlightOutliersForClickedReplicate: function(plot, precursorInfo, replicateId) {
        // for each precursor in precursorInfo
        let me = this;

        let binWidth = (plot.grid.rightEdge - plot.grid.leftEdge) / (plot.scales.x.scale.domain().length);
        let yRange = plot.scales.yLeft.range;

        let xAcc = function (d) {
            return plot.scales.x.scale(d.StartIndex) - (binWidth/2);
        };

        let widthAcc = function (d) {
            return plot.scales.x.scale(d.EndIndex) - plot.scales.x.scale(d.StartIndex) + binWidth;
        };

        // find the data point for the clicked replicate
        for (let j = 0; j < precursorInfo.data.length; j++) {
            let data = precursorInfo.data[j];
            if (data.ReplicateId === replicateId) {
                let clickedReplicateData = [];
                clickedReplicateData.push({
                    'EndIndex': data.seqValue,
                    'StartIndex': data.seqValue
                })

                let outlierRect = "rect.outlier-" + j;

                let color;
                if (data && data.LJShape && data.LJShape.indexOf('Outlier') > -1) {
                    color = '#C50000FF';
                }
                else {
                    color = '#64f341';
                }

                me.getSvgElForPlot(plot).selectAll(outlierRect).data(clickedReplicateData)
                        .enter().append("rect").attr("class", "outlier-"+j)
                        .attr('x', xAcc).attr('y', yRange[1])
                        .attr('width', widthAcc).attr('height', yRange[0] - yRange[1])
                        .attr('stroke', color).attr('stroke-opacity', 0.1)
                        .attr('fill', color).attr('fill-opacity', 0.1)
                        .append("title")
                        .text(function (d) {
                            return "Selected replicate: " + Ext4.String.htmlEncode(plot.data[d.EndIndex].ReplicateName);
                        });

                this.sendSvgElementToBack(plot, outlierRect);
                break;
            }
        }

    },

    addGuideSetTrainingRangeToPlot : function(plot, precursorInfo) {
        var me = this;
        var guideSetTrainingData = [];

        // find the x-axis starting and ending index based on the guide set information attached to each data point
        Ext4.Object.each(this.guideSetDataMap, function(guideSetId, guideSetData) {
            // each() treats the indices as property names, so convert back to an integer
            guideSetId = parseInt(guideSetId);
            // only compare guide set info for matching precursor fragment
            if (!this.singlePlot && guideSetData.Series[precursorInfo.fragment] === undefined) {
                return true; // continue
            }

            var metricIds = [];
            for (var series in guideSetData.Series[precursorInfo.fragment]) {
                if (guideSetData.Series[precursorInfo.fragment].hasOwnProperty(series)) {
                    metricIds.push(series);
                }
            }

            var gs = {GuideSetId: guideSetId,
                      series: metricIds[0]};
            for (var j = 0; j < precursorInfo.data.length; j++) {
                // only use data points that match the GuideSet RowId and are in the training set range
                if (precursorInfo.data[j].guideSetId === gs.GuideSetId && precursorInfo.data[j].inGuideSetTrainingRange) {
                    if (gs.StartIndex === undefined)
                    {
                        gs.StartIndex = precursorInfo.data[j].seqValue;
                    }
                    gs.EndIndex = precursorInfo.data[j].seqValue;
                }
            }

            if (gs.StartIndex !== undefined) {
                guideSetTrainingData.push(gs);
            }
        }, this);

        var binWidth = (plot.grid.rightEdge - plot.grid.leftEdge) / (plot.scales.x.scale.domain().length);
        var yRange = plot.scales.yLeft.range;

        var xAcc = function (d) {
            return plot.scales.x.scale(d.StartIndex) - (binWidth/2);
        };

        var widthAcc = function (d) {
            return plot.scales.x.scale(d.EndIndex) - plot.scales.x.scale(d.StartIndex) + binWidth;
        };

        var xSep = function (d) {
            return (plot.scales.x.scale(d.StartIndex) - (binWidth/2)) + 5;
        };

        if (this.showExpRunRange) {
            // determine the start index and end index for exp region to be highlighted
            this.calculatePlotIndicesBetweenDates(precursorInfo);

            if (this.expRunDetails && this.expRunDetails.startIndex !== undefined && this.expRunDetails.endIndex !== undefined) {
                var startIndex = this.expRunDetails.startIndex;
                var endIndex = this.expRunDetails.endIndex;
                var pointsData = precursorInfo.data;
                var expDataArr = [];

                for (var i = startIndex; i <= endIndex; i++) {
                    expDataArr.push(pointsData[i].value);
                }

                var expMean = LABKEY.targetedms.PlotSettingsUtil.formatNumeric(LABKEY.vis.Stat.getMean(expDataArr));
                var expStdDev = LABKEY.targetedms.PlotSettingsUtil.formatNumeric(LABKEY.vis.Stat.getStdDev(expDataArr));
                var expPercentCV = LABKEY.targetedms.PlotSettingsUtil.formatNumeric((expStdDev / expMean) * 100);

                var expRangeData = [];
                expRangeData.push({
                    'EndIndex': endIndex,
                    'StartIndex': startIndex
                })
                var expRange = me.getSvgElForPlot(plot).selectAll("rect.expRange").data(expRangeData)
                        .enter().append("rect").attr("class", "expRange")
                        .attr('x', xAcc).attr('y', yRange[1])
                        .attr('width', widthAcc).attr('height', yRange[0] - yRange[1])
                        .attr('stroke', '#557098').attr('stroke-opacity', 0.1)
                        .attr('fill', '#557098').attr('fill-opacity', 0.1);

                // TODO: look into setting background color of title tooltip
                expRange.append("title").text(function (d) {
                    return "Skyline File: " + Ext4.String.htmlEncode(me.expRunDetails.fileName)
                            + (me.expRunDetails.serialNumber ? (", \nSerial No: " + Ext4.String.htmlEncode(me.expRunDetails.serialNumber)) : "")
                            + (me.expRunDetails.instrumentName ? (", \nInstrument Name: " + Ext4.String.htmlEncode(me.expRunDetails.instrumentName)) : "")
                            + ", \nStart: " + Ext4.String.htmlEncode(me.formatDate(new Date(me.expRunDetails.startDate), true))
                            + ", \nEnd: " + Ext4.String.htmlEncode(me.formatDate(new Date(me.expRunDetails.endDate), true))
                            + ", \nMean: " + Ext4.String.htmlEncode(expMean)
                            + ", \nStd Dev: " + Ext4.String.htmlEncode(expStdDev)
                            + ", \n%CV: " + Ext4.String.htmlEncode(expPercentCV);
                });
            }

        }

        // add a "shaded" rect to indicate which points in the plot are part of the guide set training range
        if (guideSetTrainingData.length > 0) {
            var guideSetTrainingRange = this.getSvgElForPlot(plot).selectAll("rect.training").data(guideSetTrainingData)
                .enter().append("rect").attr("class", "training")
                .attr('x', xAcc).attr('y', yRange[1])
                .attr('width', widthAcc).attr('height', yRange[0] - yRange[1])
                .attr('stroke', '#000000').attr('stroke-opacity', 0.1)
                .attr('fill', '#000000').attr('fill-opacity', 0.1);

            guideSetTrainingRange.append("title").text(function (d) {
                var guideSetInfo = me.guideSetDataMap[d.GuideSetId],
                    seriesGuideSetInfo = guideSetInfo.Series[precursorInfo.fragment][d.series],
                    numRecs = seriesGuideSetInfo ? seriesGuideSetInfo.NumRecords : 0,
                    showGuideSetStats = !me.singlePlot && numRecs > 0,
                    mean, stdDev, percentCV;

                if (showGuideSetStats) {
                    mean = LABKEY.targetedms.PlotSettingsUtil.formatNumeric(seriesGuideSetInfo.Mean);
                    stdDev = LABKEY.targetedms.PlotSettingsUtil.formatNumeric(seriesGuideSetInfo.StdDev);
                    percentCV = LABKEY.targetedms.PlotSettingsUtil.formatNumeric((stdDev / mean) * 100);
                }

                return "Guide Set ID: " + Ext4.String.htmlEncode(d.GuideSetId) + ","
                    + "\nStart: " + Ext4.String.htmlEncode(me.formatDate(new Date(guideSetInfo.TrainingStart), true))
                    + ",\nEnd: " + Ext4.String.htmlEncode(me.formatDate(new Date(guideSetInfo.TrainingEnd), true))
                    + (showGuideSetStats ? ",\n# Runs: " + Ext4.String.htmlEncode(numRecs) : "")
                    + (showGuideSetStats ? ",\nMean: " + Ext4.String.htmlEncode(mean) : "")
                    + (showGuideSetStats ? ",\nStd Dev: " + Ext4.String.htmlEncode(stdDev) : "")
                    + (showGuideSetStats ? ",\n%CV: " + Ext4.String.htmlEncode(percentCV) : "")
                    + (guideSetInfo.Comment ? (",\nComment: " + Ext4.String.htmlEncode(guideSetInfo.Comment)) : "");
            });

            if (this.filterQCPoints) {
                var guideSetEndIndex = guideSetTrainingData[0]['EndIndex'];
                this.getSvgElForPlot(plot).selectAll("line.separator").data([{'StartIndex': guideSetEndIndex + 1, 'EndIndex': guideSetEndIndex + 1}])
                        .enter().append("line").attr("class", "separator")
                        .attr('x1', xSep).attr('y1', yRange[0]).attr('x2', xSep).attr('y2', yRange[1])
                        .attr('stroke', '#000000').attr('stroke-opacity', 1)
                        .attr('fill', '#000000').attr('fill-opacity', 1);
            }
        }

        // Issue 46477: need to move the guide set range display behind the data points
        // so that points can be interacted with (i.e. hover to exclude, see details, etc.)
        this.sendSvgElementToBack(plot, "rect.training");
    },

    bringSvgElementToFront: function(plot, selector) {
        this.getSvgElForPlot(plot).selectAll(selector)
            .each(function() {
               this.parentNode.parentNode.appendChild(this.parentNode);
            });
    },

    sendSvgElementToBack: function(plot, selector) {
        var firstPointLayer = this.getSvgElForPlot(plot).selectAll('g.layer')[0][0];
        this.getSvgElForPlot(plot).selectAll(selector)
            .each(function() {
                this.parentNode.insertBefore(this, firstPointLayer);
            });
    },

    addAnnotationsToPlot: function(plot, precursorInfo) {
        var me = this;

        // Issue 38270. Get unique dates just in case there are two replicates with the same acquired time.
        // This can happen e.g. if a raw file is imported from different locations.
        var xAxisLabels = Ext4.Array.unique(Ext4.Array.pluck(precursorInfo.data, "fullDate"));
        if (this.groupedX) {
            xAxisLabels = [];

            // determine the annotation index based on the "date" but unique values are based on "groupedXTick"
            var prevGroupedXTick = null;
            Ext4.each(precursorInfo.data, function(row) {
                if (row['groupedXTick'] !== prevGroupedXTick) {
                    xAxisLabels.push(row['date']);
                }
                prevGroupedXTick = row['groupedXTick'];
            });
        }

        // use direct D3 code to inject the annotation icons to the rendered SVG
        var xAcc = function(d) {
            var annotationDate = me.formatDate(new Date(d['Date']), !me.groupedX);
            return plot.scales.x.scale(xAxisLabels.indexOf(annotationDate));
        };
        var yAcc = function(d) {
            return plot.scales.yLeft.range[1] - (d['yStepIndex'] * 12) - 12;
        };
        var transformAcc = function(d){
            return 'translate(' + xAcc(d) + ',' + yAcc(d) + ')';
        };
        var colorAcc = function(d) {
            return '#' + d['Color'];
        };
        var annotations = this.getSvgElForPlot(plot).selectAll("path.annotation").data(this.annotationData)
            .enter().append("path").attr("class", "annotation")
            .attr("d", this.annotationShape(5)).attr('transform', transformAcc)
            .style("fill", colorAcc).style("stroke", colorAcc);

        // add hover text for the annotation details
        annotations.append("title")
            .text(function(d) {
                return "Created By: " + d['DisplayName'] + ", "
                        + "\nType: " + d['Name'] + ", "
                    + "\nDate: " + me.formatDate(new Date(d['Date']), true) + ", "
                    + "\nDescription: " + d['Description'];
            });

        // add some mouseover effects for fun
        var mouseOn = function(pt, strokeWidth) {
            d3.select(pt).transition().duration(800).attr("stroke-width", strokeWidth).ease("elastic");
        };
        var mouseOff = function(pt) {
            d3.select(pt).transition().duration(800).attr("stroke-width", 1).ease("elastic");
        };
        annotations.on("mouseover", function(){ return mouseOn(this, 3); });
        annotations.on("mouseout", function(){ return mouseOff(this); });
    },

    formatDate: function(d, includeTime) {
        if (d instanceof Date) {
            if (includeTime) {
                return Ext4.util.Format.date(d, 'Y-m-d H:i:s');
            }
            else {
                return Ext4.util.Format.date(d, 'Y-m-d');
            }
        }
        else if (typeof(d) === 'string' && (d.length === 19 || d.length === 23)) {
            // support format of strings like "2013-08-27 14:45:49" or "2013-08-16 20:26:28.000"
            return includeTime ? d : d.substring(0, d.indexOf(' '));
        }
        else {
            return d;
        }
    },

    getReportConfig: function() {
        var config = { metric: this.metric };

        if (this.startDate) {
            config['StartDate'] = this.formatDate(this.startDate);
        }
        if (this.endDate) {
            config['EndDate'] = this.formatDate(this.endDate);
        }

        return config;
    },

    applyCustomDateRange: function() {
        const startDateRawValue = this.getStartDateField().getRawValue(),
            startDateValue = this.getStartDateField().getValue(),
            endDateRawValue = this.getEndDateField().getRawValue(),
            endDateValue = this.getEndDateField().getValue();

        if ((!this.getStartDateField().isValid() || !this.getEndDateField().isValid()) ||
                (startDateRawValue === '' && endDateRawValue === '')) {
            this.getDateRangeErrorToolbar().show();
            Ext4.get('DateRangeErrorBar').setHTML('Please correct the date range. Check the start and end dates are valid.');
        }
        // verify that the start date is not after the end date
        else if (startDateValue > endDateValue && endDateValue !== '') {
            this.getDateRangeErrorToolbar().show();
            Ext4.get('DateRangeErrorBar').setHTML('Please enter an end date that does not occur before the start date.');
        }
        else {
            this.getDateRangeErrorToolbar().hide();
            // get date values without the time zone info
            this.startDate = startDateRawValue;
            this.endDate = endDateRawValue;
            this.havePlotOptionsChanged = true;

            this.displayTrendPlot();

            // reset expRunDetails highlighted region startIndex and endIndex
            if (this.showExpRunRange && this.expRunDetails) {
                if (this.expRunDetails.startIndex) {
                    this.expRunDetails.startIndex = undefined;
                }
                if (this.expRunDetails.endIndex) {
                    this.expRunDetails.endIndex = undefined;
                }
            }

            // reset reference guideset indices
            this.resetFilterPointsIndices();
        }
    },

    applyAnnotationFiltersBtnClick: function() {
        // make sure that at least one filter is selected
        if (this.getAnnotationListTree().getChecked().length === 0) {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please select a replicate annotation.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }

        else {
            this.displayTrendPlot();
        }
    },

    updateSelectedAnnotations: function() {

        if(!this.annotationFiltersField)
            return;

        var filters = this.annotationFiltersField.getChecked();

        this.selectedAnnotations = {};

        for(var i = 0; i < filters.length; i++) {
            var annotation = filters[i];
            var annotationName = annotation.parentNode.get('text');
            var annotationValue = annotation.get('text');

            var selected = this.selectedAnnotations[annotationName];
            if (!selected) {
                selected = [];
                this.selectedAnnotations[annotationName] = selected;
            }

            selected.push(annotationValue);
        }

        this.updateSelectedAnnotationsToolbar();
        this.havePlotOptionsChanged = true;
        this.annotationFiltersField.collapse();
    },

    updateSelectedAnnotationsToolbar: function() {
        var selectedAnnotationsTb = this.selectedAnnotationsToolbar;
        if(!selectedAnnotationsTb)
            return;

        selectedAnnotationsTb.removeAll();
        var selectedDisplay = '';
        var and = '';
        Ext4.Object.each(this.selectedAnnotations, function(name, values) {
            selectedDisplay += and;
            and = 'AND ';
            selectedDisplay += (name + ' (');
            for(var i = 0; i < values.length; i++)
            {
                if(i > 0) selectedDisplay += ' OR ';
                selectedDisplay += values[i];
            }
            selectedDisplay += ') ';
        });
        if(selectedDisplay.length > 0) {
            selectedDisplay = 'Selected: ' + selectedDisplay;
            selectedAnnotationsTb.add({
                xtype: 'box',
                flex: 1,
                style: 'white-space: normal; text-align: center;',
                html: Ext4.String.htmlEncode(selectedDisplay)
            });
            selectedAnnotationsTb.show();
        }
        else {
            selectedAnnotationsTb.hide();
        }
    },

    clearAnnotationFiltersBtnClick: function() {

        this.selectedAnnotations = {};
        var annotationsTree = this.getAnnotationListTree();
        var records = annotationsTree.getChecked();

        if(records.length === 0) {
            return;
        }

        for(var i = 0; i < records.length; i++) {
            records[i].set('checked', false);
        }

        this.havePlotOptionsChanged = true;
        this.clearAnnotationFiltersButton.hide();

        this.displayTrendPlot();
    },
    
    createGuideSetBtnClick: function() {
        var minGuideSetPointCount = 5; // to warn user if less than this many points are selected for the new guide set

        if (this.plotBrushSelection && this.plotBrushSelection.points.length > 0) {
            var startDate = this.plotBrushSelection.points[0]['fullDate'];
            var endDate = this.plotBrushSelection.points[this.plotBrushSelection.points.length - 1]['fullDate'];

            if (this.plotBrushSelection.points.length < minGuideSetPointCount) {
                Ext4.Msg.show({
                    title:'Create Guide Set Warning',
                    icon: Ext4.MessageBox.WARNING,
                    msg: 'Fewer than ' + minGuideSetPointCount + ' data points were selected for the new guide set, which may not be statistically significant. Would you like to proceed anyway?',
                    buttons: Ext4.Msg.YESNO,
                    scope: this,
                    fn: function(btnId, text, opt){
                        if(btnId === 'yes'){
                            this.insertNewGuideSet(startDate, endDate);
                        }
                    }
                });
            }
            else {
                this.insertNewGuideSet(startDate, endDate);
            }
        }
    },

    insertNewGuideSet : function(startDate, endDate) {
        LABKEY.Query.insertRows({
            schemaName: 'targetedms',
            queryName: 'GuideSet',
            rows: [{TrainingStart: startDate, TrainingEnd: endDate}],
            success: function(data) {
                this.plotBrushSelection = undefined;
                this.setBrushingEnabled(false);

                // issue 26019: since guide sets won't be created that often and we now remember plot option selections,
                // force page reload for new guide set creation this allows the sample file information to be updated
                // easily in the QC Summary webpart (which is commonly displayed on the same page as this plot).
                window.location.reload();
            },
            failure: function(response) {
                Ext4.Msg.show({
                    title:'Error Creating Guide Set',
                    icon: Ext4.MessageBox.ERROR,
                    msg: response.exception,
                    buttons: Ext4.Msg.OK
                });
            },
            scope: this
        })
    },

    persistSelectedFormOptions : function() {
        if (this.havePlotOptionsChanged) {
            this.havePlotOptionsChanged = false;
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
                method: 'POST',
                params: this.getSelectedPlotFormOptions()
            });
        }
    },

    getSelectedPlotFormOptions : function() {
        var annotationsProp = [];

        Ext4.Object.each(this.selectedAnnotations, function(name, values) {
            for(var i = 0; i < values.length; i++)
            {
                annotationsProp.push(name + ":" + values[i]);
            }
        });

        var props = {
            metric: this.metric,
            metric2: this.metric2,
            plotTypes: this.plotTypes,
            yAxisScale: this.yAxisScale,
            groupedX: this.groupedX,
            singlePlot: this.singlePlot,
            showExcluded: this.showExcluded,
            dateRangeOffset: this.dateRangeOffset,
            selectedAnnotations: annotationsProp,
            showExcludedPrecursors: this.showExcludedPrecursors,
            trailingRuns: this.trailingRuns
        };

        // set start and end date to null unless we are
        props.startDate = this.dateRangeOffset === -1 ? this.formatDate(this.startDate) : null;
        props.endDate = this.dateRangeOffset === -1 ? this.formatDate(this.endDate) : null;

        return props;
    },

    getMaxStackedAnnotations : function() {
        if (this.annotationData.length > 0) {
            return Math.max.apply(Math, (Ext4.Array.pluck(this.annotationData, "yStepIndex"))) + 1;
        }
        return 0;
    },

    getColorRange: function() {
        // Use our default colors
        const result = LABKEY.vis.Scale.ColorDiscrete().concat(LABKEY.vis.Scale.DarkColorDiscrete());

        // But override them with colors assigned by the server, when available, so that we match Skyline's assignment
        for (let i = 0; i < this.precursors.length; i++) {
            // We only get data points for the precursors in the current "page" so double check that it's in the fragmentPlotData object
            if (this.fragmentPlotData[this.precursors[i]] && this.fragmentPlotData[this.precursors[i]].color) {
                result[i] = this.fragmentPlotData[this.precursors[i]].color;
            }
        }
        return result;
    }

});
