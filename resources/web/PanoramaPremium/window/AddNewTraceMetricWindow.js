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

    initComponent: function() {
        var title = this.operation === this.insert ? 'Add New Trace Metric' : 'Edit Trace Metric';
        this.setTitle(title);
        this.height = Ext4.max([Ext4.getBody().getHeight() * 0.3, 250]);
        this.width = Ext4.max([Ext4.getBody().getWidth() * 0.3, 500]);
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
                width: 470,
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
              width: 470
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
            this.traceValueRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                columns: 2,
                layoutOptions: 'time',
                padding: '0 10px 0 0',
                items: [
                     {
                         boxLabel: 'Use trace value when time is greater than or equal to',
                         inputValue: 'timeValue',
                         name: 'metricValue',
                         width: 400,

                         checked: this.operation === this.update ? this.metric.TimeValue > 0 : true,
                         listeners: {
                             change: {fn : function(cmp, newVal, oldVal){
                                 this.timeValueNumberField.setDisabled(oldVal);
                                 this.traceValueNumberField.setDisabled(newVal);

                                 // restore the value onChange when it is present
                                 if (this.operation === this.update) {
                                     if (newVal) {
                                         this.timeValueNumberField.setValue(this.metric.TimeValue);
                                     }
                                     else {
                                         this.traceValueNumberField.setValue(this.metric.TraceValue);
                                     }
                                 }
                                 else {
                                     this.traceValueNumberField.setValue(undefined);
                                     this.timeValueNumberField.setValue(undefined);
                                 }
                             }},
                             scope   : this
                         }
                     },
                    this.getTimeValueNumberField(),
                    {
                        boxLabel: 'Use time when the trace first reaches a value greater than or equal to',
                        inputValue: 'traceValue',
                        name: 'metricValue',
                        width: 400,
                        checked: this.operation === this.update ? this.metric.TraceValue > 0 : false
                    },
                    this.getTraceValueNumberField()
                ]
            });
        }
        return this.traceValueRadioGroup;
    },

    getTimeValueNumberField: function () {
        if (!this.timeValueNumberField) {
            this.timeValueNumberField = Ext4.create('Ext.form.field.Number', {
                name: 'timeValue',
                width: 65,
                disabled: this.operation === this.update ? !(this.metric.TimeValue > 0) : false
            });

            if (this.operation === this.update) {
                this.timeValueNumberField.setValue(this.metric.TimeValue);
            }

        }
        return this.timeValueNumberField;
    },

    getTraceValueNumberField: function () {
        if (!this.traceValueNumberField) {
            this.traceValueNumberField = Ext4.create('Ext.form.field.Number', {
                name: 'traceValue',
                width: 65,
                disabled: this.operation === this.update ? !(this.metric.TraceValue > 0) : true
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
                width: 470,
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

        if (this.traceValueRadioGroup.getValue()['metricValue'] === 'timeValue' &&
                (!this.timeValueNumberField.getValue() || this.timeValueNumberField.getValue() < 0)) {
            this.timeValueNumberField.setActiveError(errorText);
            isValid = false;
        }

        if (this.traceValueRadioGroup.getValue()['metricValue'] === 'traceValue' && !this.traceValueNumberField.getValue()) {
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
            else if (this.timeValueNumberField.getValue()) {
                newMetric.TimeValue = this.timeValueNumberField.getValue();
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
                        window.location = this.getReturnURL();
                    }
                });
                win.close();
            }
        }, this);
    },

    getReturnURL: function () {
        var returnURL = LABKEY.ActionURL.getParameter('returnUrl');

        if (returnURL) {
            return returnURL;
        }
        else {
            return LABKEY.ActionURL.buildURL('project', 'start');
        }
    }
});
