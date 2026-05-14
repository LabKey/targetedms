/*
 * Copyright (c) 2025 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

Ext4.define('Panorama.Window.AddAnnotationMetricWindow', {
    extend: 'Ext.window.Window',

    modal: true,
    closeAction: 'destroy',
    bodyStyle: 'padding: 10px;',
    autoScroll: true,
    border: false,
    update: 'update',
    insert: 'insert',

    initComponent: function() {
        var title = this.operation === this.insert ? 'Add Annotation-Backed Metric' : 'Edit Annotation-Backed Metric';
        this.setTitle(title);
        this.height = Ext4.max([Ext4.getBody().getHeight() * 0.3, 300]);
        this.width = Ext4.max([Ext4.getBody().getWidth() * 0.25, 500]);
        this._allAnnotations = [];
        this.items = this.getItems();
        this.dockedItems = [{
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: this.getButtons()
        }];

        this.callParent();
        this.loadAnnotations();
    },

    loadAnnotations: function() {
        LABKEY.Query.selectRows({
            schemaName: 'targetedms',
            queryName: 'AnnotationSettings',
            columns: ['Name', 'Targets', 'Type'],
            filterArray: [LABKEY.Filter.create('Type', 'number', LABKEY.Filter.Types.EQUAL)],
            scope: this,
            success: function(data) {
                this._allAnnotations = data.rows || [];
                this.refreshAnnotationsCombo();
            }
        });
    },

    getAnnotationTarget: function() {
        var val = this.annotationTypeGroup.down('radiogroup').getValue();
        return val && val['annotationType'] === 'precursor' ? 'precursor_result' : 'replicates';
    },

    getFilteredAnnotations: function() {
        var target = this.getAnnotationTarget();
        var seen = {};
        var result = [];
        this._allAnnotations.forEach(function(row) {
            var targets = (row['Targets'] || '').split(',').map(function(s) { return s.trim(); });
            if (targets.indexOf(target) >= 0 && !seen[row['Name']]) {
                seen[row['Name']] = true;
                result.push({ Name: row['Name'] });
            }
        });
        return result;
    },

    refreshAnnotationsCombo: function() {
        var annotations = this.getFilteredAnnotations();
        var store = Ext4.create('Ext.data.Store', {
            fields: ['Name'],
            sorters: [{property: 'Name'}],
            data: annotations
        });
        this.annotationsCombo.bindStore(store);
        if (this.operation === this.update && this.metric && this.metric.AnnotationName) {
            this.annotationsCombo.setValue(this.metric.AnnotationName);
        } else {
            this.annotationsCombo.clearValue();
        }
    },

    getItems: function() {
        return [
            this.getMetricNameField(),
            this.getYAxisLabelField(),
            this.getAnnotationTypeRadioGroup(),
            this.getAnnotationsCombo(),
            this.getQueryError()
        ];
    },

    getButtons: function() {
        var buttons = [];
        buttons.push(this.getCancelButton());
        buttons.push('->');
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
                width: 450,
                name: 'metricName'
            });
            if (this.operation === this.update) {
                this.metricNameField.setValue(this.metric.name);
            }
        }
        return this.metricNameField;
    },

    getYAxisLabelField: function() {
        if (!this.yAxisLabelField) {
            this.yAxisLabelField = Ext4.create('Ext.form.field.Text', {
                fieldLabel: 'Y-Axis Label',
                labelWidth: 150,
                width: 450,
                name: 'yAxisLabel'
            });
            if (this.operation === this.update) {
                this.yAxisLabelField.setValue(this.metric.YAxisLabel);
            }
        }
        return this.yAxisLabelField;
    },

    getAnnotationTypeRadioGroup: function() {
        if (!this.annotationTypeGroup) {
            var isPrecursor = this.operation === this.update ? this.metric.PrecursorScoped : false;
            this.annotationTypeGroup = Ext4.create('Ext.form.Panel', {
                border: false,
                width: 450,
                items: [{
                    xtype: 'radiogroup',
                    fieldLabel: 'Annotation Type',
                    labelWidth: 150,
                    columns: 2,
                    items: [
                        {
                            xtype: 'radio',
                            name: 'annotationType',
                            inputValue: 'replicate',
                            boxLabel: 'Replicate',
                            checked: !isPrecursor,
                            listeners: {
                                change: {
                                    fn: function(cmp, newVal) {
                                        if (newVal) {
                                            this.refreshAnnotationsCombo();
                                        }
                                    },
                                    scope: this
                                }
                            }
                        },
                        {
                            xtype: 'radio',
                            name: 'annotationType',
                            inputValue: 'precursor',
                            boxLabel: 'Precursor',
                            checked: isPrecursor,
                            listeners: {
                                change: {
                                    fn: function(cmp, newVal) {
                                        if (newVal) {
                                            this.refreshAnnotationsCombo();
                                        }
                                    },
                                    scope: this
                                }
                            }
                        }
                    ]
                }]
            });
        }
        return this.annotationTypeGroup;
    },

    getAnnotationsCombo: function() {
        if (!this.annotationsCombo) {
            this.annotationsCombo = Ext4.create('Ext.form.field.ComboBox', {
                fieldLabel: 'Annotation',
                labelWidth: 150,
                width: 450,
                name: 'annotationName',
                displayField: 'Name',
                valueField: 'Name',
                store: Ext4.create('Ext.data.Store', { fields: ['Name'] }),
                emptyText: 'Loading annotations...',
                forceSelection: true,
                queryMode: 'local'
            });
        }
        return this.annotationsCombo;
    },

    getQueryError: function() {
        if (!this.queryError) {
            this.queryError = Ext4.create('Ext.form.Label', {
                name: 'errorMsg',
                hidden: true,
                cls: 'labkey-error',
                text: ''
            });
        }
        return this.queryError;
    },

    getSaveButton: function() {
        if (!this.saveButton) {
            this.saveButton = Ext4.create('Ext.button.Button', {
                text: 'Save',
                scope: this,
                handler: this.saveMetric
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
                handler: function(btn) {
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

        if (!(this.yAxisLabelField.getValue().length > 0)) {
            this.yAxisLabelField.setActiveError(errorText);
            isValid = false;
        }

        if (!this.annotationsCombo.getValue()) {
            this.annotationsCombo.setActiveError(errorText);
            isValid = false;
        }

        return isValid;
    },

    checkMetricNameExists: function(metricName, callback) {
        var filterArray = [LABKEY.Filter.create('Name', metricName, LABKEY.Filter.Types.EQUAL)];

        if (this.operation === this.update && this.metric) {
            filterArray.push(LABKEY.Filter.create('id', this.metric.id, LABKEY.Filter.Types.NOT_EQUAL));
        }

        LABKEY.Query.selectRows({
            containerPath: LABKEY.container.id,
            schemaName: 'targetedms',
            queryName: 'qcmetricconfiguration',
            filterArray: filterArray,
            scope: this,
            success: function(data) {
                callback.call(this, data.rows.length > 0);
            },
            failure: function() {
                callback.call(this, false);
            }
        });
    },

    saveMetric: function() {
        if (!this.validateValues()) {
            return;
        }

        var metricName = this.metricNameField.getValue();

        this.checkMetricNameExists(metricName, function(exists) {
            if (exists) {
                this.queryError.setText('A metric with the name "' + metricName + '" already exists. Please choose a different name.');
                this.queryError.setVisible(true);
                this.metricNameField.setActiveError('Metric name already exists');
                return;
            }

            var typeVal = this.annotationTypeGroup.down('radiogroup').getValue();
            var isPrecursor = typeVal && typeVal['annotationType'] === 'precursor';

            var newMetric = {
                Name: metricName,
                QueryName: 'QCAnnotationMetric',
                YAxisLabel: this.yAxisLabelField.getValue(),
                PrecursorScoped: isPrecursor,
                AnnotationName: this.annotationsCombo.getValue()
            };

            if (this.operation === this.update) {
                newMetric.id = this.metric.id;
            }

            LABKEY.Query.saveRows({
                containerPath: LABKEY.container.id,
                commands: [{
                    schemaName: 'targetedms',
                    queryName: 'qcmetricconfiguration',
                    command: this.operation,
                    rows: [newMetric]
                }],
                scope: this,
                method: 'POST',
                success: function() {
                    window.location.reload();
                },
                failure: function(response) {
                    var errorMessage = 'Error saving metric';
                    if (response && response.exception) {
                        errorMessage = response.exception;
                    } else if (response && response.message) {
                        errorMessage = response.message;
                    }
                    this.queryError.setText(errorMessage);
                    this.queryError.setVisible(true);
                }
            });
        });
    },

    deleteMetric: function() {
        Ext4.Msg.confirm('Delete Annotation-Backed Metric', 'This will delete ' + LABKEY.Utils.encodeHtml(this.metric.name) + ' metric. Are you sure?', function(val) {
            if (val === 'yes') {
                LABKEY.Query.saveRows({
                    containerPath: LABKEY.container.id,
                    commands: [{
                        schemaName: 'targetedms',
                        queryName: 'qcenabledmetrics',
                        command: 'delete',
                        rows: [{metric: this.metric.id}]
                    }, {
                        schemaName: 'targetedms',
                        queryName: 'qcmetricconfiguration',
                        command: 'delete',
                        rows: [{id: this.metric.id}]
                    }],
                    scope: this,
                    method: 'POST',
                    success: function() {
                        window.location.reload();
                    },
                    failure: function(response) {
                        var errorMessage = 'Error deleting metric';
                        if (response && response.exception) {
                            errorMessage = response.exception;
                        }
                        this.queryError.setText(errorMessage);
                        this.queryError.setVisible(true);
                    }
                });
            }
        }, this);
    }
});
