/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

Ext4.define('Panorama.Window.AddCustomMetricWindow', {
    extend: 'Ext.window.Window',

    modal: true,
    closeAction: 'destroy',
    bodyStyle: 'padding: 10px;',
    autoScroll: true,
    border: false,
    update: 'update',
    insert: 'insert',

    SCHEMA_NAME: 'targetedms',


    initComponent: function() {
        var title = this.operation === this.insert ? 'Add New Metric' : 'Edit Metric';
        this.setTitle(title);
        this.height = Ext4.max([Ext4.getBody().getHeight() * 0.25, 200]);
        this.width = Ext4.max([Ext4.getBody().getWidth() * 0.2, 450]);
        this.items = this.getItems();
        this.dockedItems= [{
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: this.getButtons()
        }]

        this.callParent();

        LABKEY.Query.getQueries({
            scope: this,
            schemaName: this.SCHEMA_NAME,
            success: function(queriesInfo) {
                this.queries = queriesInfo.queries;
                this.enabledqueries = queriesInfo.queries;
            }
        });

    },

    getItems: function() {
        return [
            this.getMetricNameField(),
            this.getQueriesCombo(),
            this.getMetricTypeCombo(),
            this.getYAxisLabelField(),
            this.getEnabledQueriesCombo(),
            this.getQueryError(),
        ];
    },

    getButtons: function () {
        var buttons = [];

        buttons.push(this.getCancelButton());
        buttons.push('->'); // to push remaining buttons to the right
        if (this.operation === this.update) {
            buttons.push(this.getDeleteButton());
        }
        buttons.push(this.getSaveButton());
        return buttons;
    },

    getMetricNameField: function() {
        if (!this.metricNameField) {
            this.metricNameField = Ext4.create('Ext.form.field.Text', {
                fieldLabel: 'Name',
                labelWidth: 150,
                width: 400,
                name: 'metricName'
            });

            if(this.operation === this.update) {
                this.metricNameField.setValue(this.metric.name);
            }
        }

        return this.metricNameField;
    },

    getQueriesConfig: function(label, name) {
        return {
            fieldLabel: label,
            name: name,
            labelWidth: 150,
            width: 400,
            displayField : 'title',
            valueField : 'name'
        };
    },

    getQueriesStore: function() {
        return  Ext4.create('Ext.data.Store', {
            data: this.queries,
            fields: ['name','title'],
            sorters: [{
                sorterFn: this.getQueriesSorter
            }]
        });
    },

    getQueriesSorter: function(val1, val2) {
        return LABKEY.internal.SortUtil.naturalSort(val1.get('name'), val2.get('name'));
    },

    getQueriesCombo: function() {
        if(!this.queriesCombo) {
            var config = Ext4.apply(this.getQueriesConfig('Metrics Query', 'queryName'), {
                listeners: {
                    scope: this,
                    expand: function (field, options) {
                        if (this.queries) {
                            this.queriesCombo.bindStore(this.getQueriesStore());
                        }
                    },
                    select: function(combo, records) {
                        this.validateQCMetricQuery(records[0].data.name, this.queries, combo);
                    }
                }
            });
            this.queriesCombo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.queriesCombo.setValue(this.metric.QueryName);
            }
        }

        return this.queriesCombo;
    },

    getYAxisLabelField: function() {
        if (!this.yAxisLabelField) {
            this.yAxisLabelField = Ext4.create('Ext.form.field.Text', {
                fieldLabel: 'Y-Axis Label',
                labelWidth: 150,
                width: 400,
                name: 'yAxisLabel'
            });

            if(this.operation === this.update) {
                this.yAxisLabelField.setValue(this.metric.YAxisLabel);
            }
        }

        return this.yAxisLabelField;
    },

    getEnabledQueriesCombo: function() {
        if(!this.enabledQueriesCombo) {
            var config = Ext4.apply(this.getQueriesConfig('Enabled Query', 'enabledQueryName'), {
                listeners: {
                    scope: this,
                    expand: function (field, options) {
                        if (this.enabledqueries) {
                            this.enabledQueriesCombo.bindStore(this.getQueriesStore());
                        }
                    }
                }
            });
            this.enabledQueriesCombo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.enabledQueriesCombo.setValue(this.metric.EnabledQueryName);
            }
        }

        return this.enabledQueriesCombo;
    },

    getMetricTypeCombo: function() {
        if(!this.metricTypeCombo) {
            var metricTypeStore = Ext4.create('Ext.data.Store', {
                data: [{name: 'Precursor', value: true}, {name: 'Run', value: false}],
                fields: ['name','value']
            });

            var config = {
                fieldLabel: 'Metric Type',
                name: 'metricType',
                labelWidth: 150,
                width: 400,
                store: metricTypeStore,
                displayField : 'name',
                valueField : 'value'
            };
            this.metricTypeCombo = Ext4.create('Ext.form.field.ComboBox', config);

            this.metricTypeCombo.setValue(this.operation === this.insert ? true : this.metric.PrecursorScoped);
        }
        return this.metricTypeCombo;
    },

    getQueriesForSchema: function(schemaName, callback) {
        LABKEY.Query.getQueries({
            scope: this,
            schemaName: schemaName,
            success: function(queriesInfo) {
                 callback(queriesInfo.queries, this);
            }
        });
    },

    getQueryError: function() {
        if (!this.queryError) {
            this.queryError = Ext4.create('Ext.form.Label', {
                name: 'errorMsg',
                hidden: true,
                cls: 'labkey-error',
                text:''
            });

        }

        return this.queryError;
    },

    getSaveButton: function() {
        if (!this.saveButton) {
            this.saveButton = Ext4.create('Ext.button.Button', {
                text: 'Save',
                scope: this,
                handler: this.saveNewMetric
            });
        }
        return this.saveButton;
    },

    getDeleteButton: function() {
        if (!this.deleteButton) {
            this.deleteButton = Ext4.create('Ext.button.Button', {
                text: 'Delete',
                scope: this,
                handler: this.deleteMetric
            });
        }
        return this.deleteButton;
    },

    getCancelButton: function() {
        if (!this.cancelButton) {
            this.cancelButton = Ext4.create('Ext.button.Button', {
                text: 'Cancel',
                scope: this,
                handler: function(btn){
                    btn.up('window').close();
                }
            });
        }
        return this.cancelButton;
    },

    validateValues: function() {
        var isValid = true;
        var errorText = 'Required';

        if(!this.metricNameField.getValue().length > 0) {
            this.metricNameField.setActiveError(errorText);
            isValid = false;
        }

        if(!this.queriesCombo.getValue()) {
            this.queriesCombo.setActiveError(errorText);
            isValid = false;
        }

        if(this.metricTypeCombo.getValue() == null) {
            this.metricTypeCombo.setActiveError(errorText);
            isValid = false;
        }

        return isValid;
    },

    validateQCMetricQuery: function(query, queries, combo) {
        var requiredColumns = [
                {name: 'MetricValue', isPresent: false},
                {name: 'SampleFileId', isPresent: false},
                {name: 'PrecursorChromInfoId', isPresent: false},
                ];
        var queryConfig;
        var isValid = true;

        for(var q=0; q<queries.length; q++) {
            if (query === queries[q].name) {
                queryConfig = queries[q];
            }
        }

        for(var i=0; i<requiredColumns.length; i++) {
            var requiredColumnName = requiredColumns[i].name;

            for(var j=0; j<queryConfig.columns.length;j++) {
                var presentColumnName = queryConfig.columns[j].name;

                if(requiredColumnName === presentColumnName) {
                    requiredColumns[i].isPresent = true;
                }
            }
        }
        var errorMessage = 'Query ' + query + ' is missing required column(s): ';

        var separator = '';
        Ext4.Array.forEach(requiredColumns, function (column) {
           if(!column.isPresent) {
               errorMessage += separator + column.name;
               separator = ', ';
               isValid = false;
           }
        });

        if(!isValid) {
            this.queryError.setText(errorMessage);
            combo.setActiveError('Invalid query');
        }
        this.queryError.setVisible(!isValid);
    },

    saveNewMetric: function () {
        var isValid = this.validateValues();

        if(isValid) {
            var records = [];
            var newMetric = {};
            newMetric.Name = this.metricNameField.getValue();
            newMetric.QueryName = this.queriesCombo.getValue();
            newMetric.YAxisLabel = this.yAxisLabelField.getValue();
            newMetric.PrecursorScoped = this.metricTypeCombo.getValue();

            newMetric.EnabledQueryName = this.enabledQueriesCombo.getValue();

            if(this.operation === this.update) {
                newMetric.id = this.metric.id;
            }

            records.push(newMetric);

            LABKEY.Query.saveRows({
                containerPath: LABKEY.container.id,
                commands: [{
                    schemaName: 'targetedms',
                    queryName: 'qcmetricconfiguration',
                    command: this.operation,
                    rows: records
                }],
                scope: this,
                method: 'POST',
                success: function () {
                    window.location = this.getReturnUrl();
                }
            });
        }

    },

    deleteMetric: function() {
        Ext4.Msg.confirm('Delete Custom Metric', 'This will delete ' + LABKEY.Utils.encodeHtml(this.metric.name) +   ' metric. Are you sure you want to do this?', function(val){
                if (val == 'yes'){
                    var qcMetricToDelete = {metric: this.metric.id};
                    var metricToDelete = {id: this.metric.id};
                    LABKEY.Query.saveRows({
                        containerPath: LABKEY.container.id,
                        commands: [{
                            schemaName: 'targetedms',
                            queryName: 'qcenabledmetrics',
                            command: 'delete',
                            rows: [qcMetricToDelete]
                        },{
                            schemaName: 'targetedms',
                            queryName: 'qcmetricconfiguration',
                            command: 'delete',
                            rows: [metricToDelete]
                        }],
                        scope: this,
                        method: 'POST',
                        success: function () {
                            window.location = this.getReturnUrl();
                        }
                    });
                }
            }, this);




    },

    getReturnUrl: function () {
        var returnUrl = LABKEY.ActionURL.getParameter('returnUrl');

        if(returnUrl) {
            return returnUrl;
        }
        else {
            return LABKEY.ActionURL.buildURL('project', 'start');
        }
    }
});
