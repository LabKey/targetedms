/*
 * Copyright (c) 2021 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

Ext4.define('Panorama.Window.AddTraceMetricWindow', {
    extend: 'Ext.window.Window',

    modal: true,
    closeAction: 'destroy',
    bodyStyle: 'padding: 10px;',
    autoScroll: true,
    border: false,
    update: 'update',
    insert: 'insert',
    timeValueOptions:  Ext4.create('Ext.data.Store', {
        fields: ['value'],
        data : [
            { "value":"First"},
            { "value":"Last"},
            { "value":"Max"},
            { "value":"Min"}
        ]
    }),

    initComponent: function() {
        var title = this.operation === this.insert ? 'Add New Trace Metric' : 'Edit Trace Metric';
        this.setTitle(title);
        this.height = Ext4.max([Ext4.getBody().getHeight() * 0.3, 250]);
        this.width = Ext4.max([Ext4.getBody().getWidth() * 0.3, 600]);
        this.items = this.getItems();
        this.dockedItems= [{
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: this.getButtons()
        }]

        this.callParent();

        this.timeValue = true;
        this.traceValue = false;

    },

    getItems: function() {
        return [
            this.getMetricNameField(),
            this.getTracesCombo(),
            this.getYAxisLabelField(),
            this.getTraceValueRadioGroup()
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
                fieldLabel: 'Metric Name',
                labelWidth: 150,
                width: 570,
                name: 'metricName'
            });

            if (this.operation === this.update) {
                this.metricNameField.setValue(this.metric.name);
            }
        }

        return this.metricNameField;
    },

    getTracesCombo: function () {
      if (!this.tracesCombo) {
          var config = {
              fieldLabel: 'Use Trace',
              name: 'useTrace',
              labelWidth: 150,
              width: 570
          }
          if (!this.tracesPresent) {
              config.emptyText = 'No trace can be found';
          }
          else {
              config.store = this.getTracesComboStore();
              config.valueField = 'TextId';
              config.displayField = 'TextId';
          }
          this.tracesCombo = Ext4.create('Ext.form.field.ComboBox', config);

          if (this.operation === this.update) {
              this.tracesCombo.setValue(this.metric.TraceName);
              this.tracesCombo.bindStore(this.getTracesComboStore());
          }
      }
      return this.tracesCombo;
    },

    getTracesComboStore: function () {
        return Ext4.create('Ext.data.Store', {
            fields: ['TextId'],
            sorters: [{property: 'TextId'}],
            data: this.traces
        });
    },

    getTraceValueRadioGroup: function () {
        if (!this.traceValueRadioGroup) {
            this.traceValueRadioGroup =
                    Ext4.create('Ext.form.Panel', {
                        renderTo: Ext4.getBody(),
                        width: 570,

                        border:false,
                        items: [{
                            xtype: 'radiogroup',
                            columns: 1, // Stack radio buttons vertically
                            vertical: true,
                            items: [
                                {
                                    xtype: 'container',
                                    layout: 'hbox',
                                    items: [
                                        {
                                            xtype: 'radio',
                                            name: 'metricValue',
                                            inputValue: 'timeValue',
                                            boxLabel: 'Use the',
                                            width: 65,
                                            checked: this.operation === this.update ? this.metric.MinTimeValue >= 0 : true,
                                            listeners: {
                                                 change: {fn : function(cmp, newVal, oldVal){
                                                     this.minTimeValueNumberField.setDisabled(oldVal);
                                                     this.maxTimeValueNumberField.setDisabled(oldVal);
                                                     this.timeValueOptionField.setDisabled(oldVal);
                                                     this.traceValueNumberField.setDisabled(newVal);

                                                     // restore the value onChange when it is present
                                                     if (this.operation === this.update) {
                                                         if (newVal) {
                                                             this.minTimeValueNumberField.setValue(this.metric.MinTimeValue);
                                                             this.maxTimeValueNumberField.setValue(this.metric.MaxTimeValue);
                                                         }
                                                         else {
                                                             this.traceValueNumberField.setValue(this.metric.TraceValue);
                                                         }
                                                     }
                                                     else {
                                                         this.traceValueNumberField.setValue(undefined);
                                                         this.minTimeValueNumberField.setValue(undefined);
                                                         this.maxTimeValueNumberField.setValue(undefined);
                                                         this.timeValueOptionField.setValue(undefined);
                                                     }
                                                 }},
                                                 scope   : this
                                            }
                                        },
                                        this.getTimeValueOptionField(),
                                        {
                                            xtype: 'displayfield',
                                            value: 'trace value when time in minutes is between',
                                            margin: '0 5'
                                        },
                                        this.getMinTimeValueNumberField(),
                                        {
                                            xtype: 'displayfield',
                                            value: 'and',
                                            margin: '0 5'
                                        },
                                        this.getMaxTimeValueNumberField()
                                    ]
                                },
                                {
                                    xtype: 'container',
                                    layout: 'hbox',
                                    items: [
                                        {
                                            xtype: 'radio',
                                            name: 'metricValue',
                                            inputValue: 'traceValue',
                                            boxLabel: 'Use time in minutes when the trace first reaches a value greater than or equal to',
                                            width: 450,
                                            checked: this.operation === this.update ? this.metric.TraceValue > 0 : false
                                        },
                                        this.getTraceValueNumberField()
                                    ]
                                }
                            ]
                        }]
                    });
        }
        return this.traceValueRadioGroup;
    },

    getTimeValueOptionField: function() {
        if (!this.timeValueOptionField) {
            this.timeValueOptionField = Ext4.create('Ext.form.field.ComboBox', {
                name: 'timeValueOption',
                store: this.timeValueOptions,
                displayField: 'value',
                valueField: 'value',
                width: 50,
                itemId: 'timeValueOption',
            });

            if(this.operation === this.update) {
                this.timeValueOptionField.setValue(this.metric.TimeValueOption);
            }
        }

        return this.timeValueOptionField
    },

    getMinTimeValueNumberField: function () {
        if (!this.minTimeValueNumberField) {
            this.minTimeValueNumberField = Ext4.create('Ext.form.field.Number', {
                name: 'minTimeValue',
                width: 65,
                disabled: this.operation === this.update ? !(this.metric.MinTimeValue >= 0) : false
            });

            if (this.operation === this.update) {
                this.minTimeValueNumberField.setValue(this.metric.MinTimeValue);
            }

        }
        return this.minTimeValueNumberField;
    },

    getMaxTimeValueNumberField: function () {
        if (!this.maxTimeValueNumberField) {
            this.maxTimeValueNumberField = Ext4.create('Ext.form.field.Number', {
                name: 'maxTimeValue',
                width: 65,
                disabled: this.operation === this.update ? !(this.metric.MaxTimeValue >= 0) : false
            });

            if (this.operation === this.update) {
                this.maxTimeValueNumberField.setValue(this.metric.MaxTimeValue);
            }

        }
        return this.maxTimeValueNumberField;
    },


    getTraceValueNumberField: function () {
        if (!this.traceValueNumberField) {
            this.traceValueNumberField = Ext4.create('Ext.form.field.Number', {
                name: 'traceValue',
                width: 65,
                disabled: this.operation === this.update ? !(this.metric.TraceValue >= 0) : true
            });

            if (this.operation === this.update) {
                this.traceValueNumberField.setValue(this.metric.TraceValue);
            }
        }
        return this.traceValueNumberField;
    },

    getYAxisLabelField: function() {
        if (!this.yAxisLabelField) {
            this.yAxisLabelField = Ext4.create('Ext.form.field.Text', {
                fieldLabel: 'Y Axis Label',
                labelWidth: 150,
                width: 570,
                name: 'yAxisLabel'
            });

            if(this.operation === this.update) {
                this.yAxisLabelField.setValue(this.metric.YAxisLabel1);
            }
        }

        return this.yAxisLabelField;
    },

    getSaveButton: function() {
        if (!this.saveButton) {
            this.saveButton = Ext4.create('Ext.button.Button', {
                text: 'Save',
                scope: this,
                handler: this.saveNewMetric,
                disabled: !this.tracesPresent
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

        if (!(this.metricNameField.getValue().length > 0)) {
            this.metricNameField.setActiveError(errorText);
            isValid = false;
        }

        if (!this.tracesCombo.getValue()) {
            this.tracesCombo.setActiveError(errorText);
            isValid = false;
        }

        if (!(this.yAxisLabelField.getValue().length > 0)) {
            this.yAxisLabelField.setActiveError(errorText);
            isValid = false;
        }

        if (this.timeValueOptionField.getValue() === null) {
            this.timeValueOptionField.setActiveError(errorText);
            isValid = false;
        }

        if (this.traceValueRadioGroup.down().getValue()['metricValue'] === 'timeValue' &&
                (!(this.minTimeValueNumberField.getValue() >= 0))) {
            this.minTimeValueNumberField.setActiveError(errorText);
            isValid = false;
        }

        if (this.traceValueRadioGroup.down().getValue()['metricValue'] === 'timeValue' &&
                (!(this.maxTimeValueNumberField.getValue() >= 0))) {
            this.maxTimeValueNumberField.setActiveError(errorText);
            isValid = false;
        }

        if (this.traceValueRadioGroup.down().getValue()['metricValue'] === 'traceValue' && !this.traceValueNumberField.getValue()) {
            this.traceValueNumberField.setActiveError(errorText);
            isValid = false;
        }

        return isValid;
    },

    saveNewMetric: function () {
        var isValid = this.validateValues();

        if (isValid) {
            var records = [];
            var newMetric = {};
            newMetric.Name = this.metricNameField.getValue();
            newMetric.Series1SchemaName = 'targetedms';
            newMetric.Series1QueryName = 'QCTraceMetric'; // dummy text to insert and not an actual query
            newMetric.Series1Label = this.metricNameField.getValue();
            newMetric.PrecursorScoped = false;
            newMetric.TraceName = this.tracesCombo.getValue();
            newMetric.YAxisLabel1 = this.yAxisLabelField.getValue();

            if (this.traceValueNumberField.getValue()) {
                newMetric.TraceValue = this.traceValueNumberField.getValue();
            }
            else {
                if (this.timeValueOptionField.getValue()) {
                    newMetric.TimeValueOption = this.timeValueOptionField.getValue();
                }
                if (this.minTimeValueNumberField.getValue()) {
                    newMetric.MinTimeValue = this.minTimeValueNumberField.getValue();
                }
                if (this.maxTimeValueNumberField.getValue()) {
                    newMetric.MaxTimeValue = this.maxTimeValueNumberField.getValue();
                }
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
                    window.location = this.getReturnUrl();
                }
            });
        }

    },

    deleteMetric: function() {
        Ext4.Msg.confirm('Delete Trace Metric', 'This will delete ' + LABKEY.Utils.encodeHtml(this.metric.name) +  ' metric. Are you sure you want to do this?', function(val){
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
                win.close();
            }
        }, this);
    },

    getReturnUrl: function () {
        var returnUrl = LABKEY.ActionURL.getParameter('returnUrl');

        if (returnUrl) {
            return returnUrl;
        }
        else {
            return LABKEY.ActionURL.buildURL('project', 'start');
        }
    }
});
