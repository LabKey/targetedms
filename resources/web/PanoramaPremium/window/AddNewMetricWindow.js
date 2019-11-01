/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

Ext4.define('Panorama.Window.AddCustomMetricWindow', {
    extend: 'Ext.window.Window',

    modal: true,
    closeAction: 'destroy',
    bodyStyle: 'padding: 10px;',
    height: Ext4.max([Ext4.getBody().getHeight() * 0.3, 450]),
    width: Ext4.max([Ext4.getBody().getWidth() * 0.2, 450]),
    autoScroll: true,
    border: false,
    update: 'update',
    insert: 'insert',

    initComponent: function() {
        var title = this.operation === this.insert ? 'Add New Metric' : 'Edit Metric';
        this.setTitle(title);
        this.items = this.getItems();
        this.buttons = [this.getSaveButton()];
        if(this.operation === this.update) {
            this.buttons.push(this.getDeleteButton());
        }
        this.buttons.push(this.getCancelButton());
        this.callParent();
        if(this.operation === this.update) {
            if(this.metric.Series1SchemaName) {
                this.getQueriesForSchema(this.metric.Series1SchemaName, function (queries, scope) {
                    scope.series1queries = queries;
                    scope.queries1Combo.bindStore(scope.getQueriesStore());

                });
            }
            if(this.metric.Series2SchemaName) {
                this.getQueriesForSchema(this.metric.Series2SchemaName, function (queries, scope) {
                    scope.series2queries = queries;
                    scope.queries2Combo.bindStore(scope.getQueriesStore());
                });
            }
            if(this.metric.EnabledSchemaName) {
                this.getQueriesForSchema(this.metric.EnabledSchemaName, function (queries, scope) {
                    scope.enabledqueries = queries;
                    scope.enabledQueriesCombo.bindStore(scope.getQueriesStore());
                });
            }
        }

    },

    getItems: function() {
        return [
                this.getMetricNameField(),
                this.getSchema1Combo(),
                this.getQueries1Combo(),
                this.getSeries1AxisLabelField(),
                this.getSchema2Combo(),
                this.getQueries2Combo(),
                this.getSeries2AxisLabelField(),
                this.getMetricTypeCombo(),
                this.getEnabledSchemaCombo(),
                this.getEnabledQueriesCombo(),
            this.getQueryError(),
        ];
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

    getSchemaConfig: function(label, name) {
        return {
            fieldLabel: label +' Schema',
            name: name + 'Schema',
            store: this.schemas,
            labelWidth: 150,
            width: 400
        };
    },

    getSchema1Combo: function() {
        if(!this.schema1Combo) {
            var config = Ext4.apply(this.getSchemaConfig('Series 1', 'series1'), {
                listeners: {
                    scope: this,
                    select: function(combo, recs){
                        var rec = recs[0];
                        this.series1schema = rec.data.field1;
                        this.queries1Combo.setValue(null);

                        LABKEY.Query.getQueries({
                            scope: this,
                            schemaName: this.series1schema,
                            success: function(queriesInfo) {
                                this.series1queries = queriesInfo.queries;
                            }
                        });
                    }
                }
            });

            this.schema1Combo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.schema1Combo.setValue(this.metric.Series1SchemaName);
                this.schema1Combo.bindStore(this.schemas);
            }
        }

        return this.schema1Combo;
    },

    getQueriesConfig: function(label, name) {
        return {
            fieldLabel: label + ' Query',
            name: name + 'Query',
            labelWidth: 150,
            width: 400,
            displayField : 'title',
            valueField : 'name',
            emptyText: 'Please select ' + label + ' Schema.'
        };
    },

    getQueriesStore: function() {
        return  Ext4.create('Ext.data.Store', {
            data: this.series1queries,
            fields: ['name','title'],
            sorters: [{
                sorterFn: this.getQueriesSorter
            }]
        });
    },

    getQueriesSorter: function(val1, val2) {
        return LABKEY.internal.SortUtil.naturalSort(val1.get('name'), val2.get('name'));
    },

    getQueries1Combo: function() {
        if(!this.queries1Combo) {
            var config = Ext4.apply(this.getQueriesConfig('Series 1', 'series1'), {
                listeners: {
                    scope: this,
                    expand: function (field, options) {
                        if (this.series1queries) {
                            this.queries1Combo.bindStore(this.getQueriesStore());
                        }
                    },
                    select: function(combo, records) {
                        this.validateQCMetricQuery(records[0].data.name, this.series1queries, combo);
                    }
                }
            });
            this.queries1Combo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.queries1Combo.setValue(this.metric.Series1QueryName);
            }
        }

        return this.queries1Combo;
    },

    getSeries1AxisLabelField: function() {
        if (!this.series1AxisLabelField) {
            this.series1AxisLabelField = Ext4.create('Ext.form.field.Text', {
                fieldLabel: 'Series 1 Axis Label',
                labelWidth: 150,
                width: 400,
                name: 'series1AxisLabel'
            });

            if(this.operation === this.update) {
                this.series1AxisLabelField.setValue(this.metric.Series1Label);
            }
        }

        return this.series1AxisLabelField;
    },

    getSeries2AxisLabelField: function() {
        if (!this.series2AxisLabelField) {
            this.series2AxisLabelField = Ext4.create('Ext.form.field.Text', {
                fieldLabel: 'Series 2 Axis Label',
                labelWidth: 150,
                width: 400,
                name: 'series2AxisLabel'
            });

            if(this.operation === this.update) {
                this.series2AxisLabelField.setValue(this.metric.Series2Label);
            }
        }

        return this.series2AxisLabelField;
    },

    getSchema2Combo: function() {
        if(!this.schema2Combo) {
            var config = Ext4.apply(this.getSchemaConfig('Series 2', 'series2'), {
                listeners: {
                    scope: this,
                    select: function(combo, recs){
                        var rec = recs[0];
                        this.series2schema = rec.data.field1;
                        this.queries2Combo.setValue(null);

                        LABKEY.Query.getQueries({
                            scope: this,
                            schemaName: this.series2schema,
                            success: function(queriesInfo) {
                                this.series2queries = queriesInfo.queries;
                            }
                        });
                    }
                }
            });

            this.schema2Combo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.schema2Combo.setValue(this.metric.Series2SchemaName);
            }
        }

        return this.schema2Combo;
    },

    getQueries2Combo: function() {
        if(!this.queries2Combo) {
            var config = Ext4.apply(this.getQueriesConfig('Series 2', 'series2'), {
                listeners: {
                    scope: this,
                    expand: function (field, options) {
                        if (this.series2queries) {
                            this.queries2Combo.bindStore(this.getQueriesStore());
                        }
                    },
                    select: function(combo, records) {
                        this.validateQCMetricQuery(records[0].data.name, this.series2queries, combo);
                    }
                }
            });
            this.queries2Combo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.queries2Combo.setValue(this.metric.Series2QueryName);
            }
        }

        return this.queries2Combo;
    },

    getEnabledSchemaCombo: function() {
        if(!this.enabledSchemaCombo) {
            var config = Ext4.apply(this.getSchemaConfig('Enabled', 'enabled'), {
                listeners: {
                    scope: this,
                    select: function(combo, recs){
                        var rec = recs[0];
                        this.enabledschema = rec.data.field1;
                        this.enabledQueriesCombo.setValue(null);

                        LABKEY.Query.getQueries({
                            scope: this,
                            schemaName: this.enabledschema,
                            success: function(queriesInfo) {
                                this.enabledqueries = queriesInfo.queries;
                            }
                        });
                    }
                }
            });

            this.enabledSchemaCombo = Ext4.create('Ext.form.field.ComboBox', config);

            if(this.operation === this.update) {
                this.enabledSchemaCombo.setValue(this.metric.EnabledSchemaName);
            }
        }

        return this.enabledSchemaCombo;
    },

    getEnabledQueriesCombo: function() {
        if(!this.enabledQueriesCombo) {
            var config = Ext4.apply(this.getQueriesConfig('Enabled', 'enabled'), {
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

            if(this.operation === this.update) {
                this.metricTypeCombo.setValue(this.metric.PrecursorScoped);
            }
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

        if(!this.schema1Combo.getValue()) {
            this.schema1Combo.setActiveError(errorText);
            isValid = false;
        }

        if(this.schema1Combo.getValue() && !this.queries1Combo.getValue()) {
            this.queries1Combo.setActiveError(errorText);
            isValid = false;
        }

        if(this.schema1Combo.getValue() && !this.series1AxisLabelField.getValue().length > 0) {
            this.series1AxisLabelField.setActiveError(errorText);
            isValid = false;
        }

        if(this.schema2Combo.getValue() && !this.queries2Combo.getValue()) {
            this.queries2Combo.setActiveError("Required when series 2 schema is provided.");
            isValid = false;
        }

        if(this.schema2Combo.getValue() && !this.series2AxisLabelField.getValue().length > 0) {
            this.series2AxisLabelField.setActiveError("Required when series 2 schema is provided.");
            isValid = false;
        }

        if(this.schema1Combo.getValue() && this.metricTypeCombo.getValue() == null) {
            this.metricTypeCombo.setActiveError(errorText);
            isValid = false;
        }

        return isValid;
    },

    validateQCMetricQuery: function(query, queries, combo) {
        var requiredColumns = [
                {name: 'MetricValue',isPresent: false},
                {name: 'SampleFileId',isPresent: false},
                {name: 'SeriesLabel',isPresent: false},
                {name: 'DataType',isPresent: false},
                {name: 'PrecursorId' ,isPresent: false},
                {name: 'PrecursorChromInfoId',isPresent: false},
                {name: 'mz',isPresent: false}
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
        var errorMessage = 'Query' + query + ' is missing required column(s): ';

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
            newMetric.Series1SchemaName = this.schema1Combo.getValue();
            newMetric.Series1QueryName = this.queries1Combo.getValue();
            newMetric.Series1Label = this.series1AxisLabelField.getValue();
            newMetric.PrecursorScoped = this.metricTypeCombo.getValue();


            if(this.schema2Combo.getValue()) {
                newMetric.Series2SchemaName = this.schema2Combo.getValue();
                newMetric.Series2QueryName = this.queries2Combo.getValue();
                newMetric.Series2Label = this.series2AxisLabelField.getValue();
            }

            if(this.enabledSchemaCombo.getValue()) {
                newMetric.EnabledSchemaName = this.enabledSchemaCombo.getValue();
                newMetric.EnabledQueryName = this.enabledQueriesCombo.getValue();
            }

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
                    window.location = this.getReturnURL();
                }
            });
        }

    },

    deleteMetric: function() {
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
                window.location = this.getReturnURL();
            }
        });
    },

    getReturnURL: function () {
        var returnURL = LABKEY.ActionURL.getParameter('returnUrl');

        if(returnURL) {
            return returnURL;
        }
        else {
            return LABKEY.ActionURL.buildURL('project', 'start');
        }
    }
});