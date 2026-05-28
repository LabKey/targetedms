/*
 * Copyright (c) 2017-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

LABKEY = LABKEY || {};
LABKEY.targetedms = LABKEY.targetedms || {};

LABKEY.targetedms.QCPlotHoverPanel = function (config) {
    this.pointData = config.pointData || {};
    this.valueName = config.valueName || null;
    this.metricProps = config.metricProps || {};
    this.originalStatus = 0;
    this.existingExclusions = null;
    this.canEdit = config.canEdit || false;
    this.trailingRuns = config.trailingRuns || null;
    this.trailingStartDate = config.trailingStartDate || null;
    this.trailingEndDate = config.trailingEndDate || null;
    this.renderTo = config.renderTo;
    this.onClose = config.onClose || function () {
    };

    this.STATE = {
        INCLUDE: 0,
        EXCLUDE_METRIC: 1,
        EXCLUDE_ALL: 2
    };

    this.containerEl = null;
    this.exclusionPanel = null;
    this.exclusionsSaveBtn = null;
    this.exclusionRadioGroup = null;
    this.viewDocumentURL = null;

    this.init();
};

LABKEY.targetedms.QCPlotHoverPanel.prototype = {

    init: function () {
        if (!this.metricProps.precursorScoped) {
            this.getRunId();
        } else {
            this.getExistingReplicateExclusions();
        }
    },

    getExistingReplicateExclusions: function () {
        if (typeof this.pointData['ReplicateId'] === 'number') {
            LABKEY.Query.selectRows({
                schemaName: 'targetedms',
                queryName: 'QCMetricExclusion',
                filterArray: [LABKEY.Filter.create('ReplicateId', this.pointData['ReplicateId'])],
                scope: this,
                success: function (data) {
                    this.existingExclusions = data.rows;

                    // set the initial status for this point based on the existing exclusions
                    var metricIds = this.existingExclusions.map(function (item) {
                        return item.MetricId;
                    });
                    if (metricIds.indexOf(null) > -1) {
                        this.originalStatus = this.STATE.EXCLUDE_ALL;
                    } else if (metricIds.indexOf(this.metricProps.id) > -1) {
                        this.originalStatus = this.STATE.EXCLUDE_METRIC;
                    }

                    this.initializePanel();
                }
            });
        }
    },

    initializePanel: function () {
        this.containerEl = document.createElement('div');
        this.containerEl.className = 'qc-plot-hover-panel';
        this.containerEl.style.backgroundColor = 'white';
        this.containerEl.style.border = '5px solid rgba(0, 0, 0, 0.5)';
        this.containerEl.style.padding = '10px';
        this.containerEl.style.minWidth = '600px';
        this.containerEl.style.color = 'black';

        let hideExclusionAndPointClickLinks = false;
        if (this.valueName === "TrailingMean") {
            hideExclusionAndPointClickLinks = true
        }
        if (this.valueName === "TrailingCV") {
            hideExclusionAndPointClickLinks = true
        }

        if (this.metricProps.name !== undefined) {
            this.addElement(this.getPlotPointDetailField('Metric', this.metricProps.name));
        }

        var fragmentValue = this.pointData['fragment'];
        if (typeof fragmentValue === 'object' && fragmentValue !== null) {
            fragmentValue = fragmentValue.name || fragmentValue.toString();
        }
        this.addElement(this.getPlotPointDetailField(this.pointData['dataType'], fragmentValue, 'qc-hover-value-break'));

        if (this.valueName.indexOf('CUSUM') > -1) {
            this.addElement(this.getPlotPointDetailField('Group', 'CUSUMmN' === this.valueName || 'CUSUMvN' === this.valueName ? 'CUSUM-' : 'CUSUM+'));
        }

        if (this.pointData.conversion && this.pointData.rawValue !== undefined && this.valueName.indexOf("CUSUM") === -1) {
            if (this.pointData.conversion === 'percentDeviation') {
                this.addElement(this.getPlotPointDetailField('Value', LABKEY.targetedms.PlotSettingsUtil.formatNumeric(this.pointData.rawValue)));
                this.addElement(this.getPlotPointDetailField('% of Mean', (this.valueName ? this.pointData[this.valueName] : this.pointData['value']) + '%'))
            } else if (this.pointData.conversion === 'standardDeviation') {
                this.addElement(this.getPlotPointDetailField('Value', LABKEY.targetedms.PlotSettingsUtil.formatNumeric(this.pointData.rawValue)));
                this.addElement(this.getPlotPointDetailField('Std Devs', this.valueName ? this.pointData[this.valueName] : this.pointData['value']))
            } else if (this.pointData.conversion === 'deltaFromMean') {
                this.addElement(this.getPlotPointDetailField('Value', LABKEY.targetedms.PlotSettingsUtil.formatNumeric(this.pointData.rawValue)));
                this.addElement(this.getPlotPointDetailField('Delta From Mean', LABKEY.targetedms.PlotSettingsUtil.formatNumeric(this.valueName ? this.pointData[this.valueName] : this.pointData['value'])))
            } else {
                this.addElement(this.getPlotPointDetailField('Value', LABKEY.targetedms.PlotSettingsUtil.formatNumeric(this.valueName ? this.pointData[this.valueName] : this.pointData['value'])));
            }
        } else {
            this.addElement(this.getPlotPointDetailField('Value', LABKEY.targetedms.PlotSettingsUtil.formatNumeric(this.valueName ? this.pointData[this.valueName] : this.pointData['value'])));
        }

        if (hideExclusionAndPointClickLinks) {
            let numOfRunsAverage = 0;
            // check if guide set is present
            if (this.pointData['inGuideSetTrainingRange'] !== undefined) {
                numOfRunsAverage = this.trailingRuns > this.pointData['TrainingSeqIdx'] ? this.pointData['TrainingSeqIdx'] : this.trailingRuns
            } else {
                numOfRunsAverage = this.trailingRuns > this.pointData['seqValue'] + 1 ? this.pointData['seqValue'] + 1 : this.trailingRuns;
            }
            this.addElement(this.getPlotPointDetailField('Replicate', numOfRunsAverage + " runs average"));
            this.addElement(this.getPlotPointDetailField('Acquired', this.trailingStartDate + " - " + this.trailingEndDate));
        } else {
            this.addElement(this.getPlotPointDetailField('Replicate', this.pointData['ReplicateName']));
            this.addElement(this.getPlotPointDetailField('Acquired', this.pointData['fullDate']));
        }

        if (!hideExclusionAndPointClickLinks) {
            this.addElement(this.getPlotPointDetailField('File Path', this.pointData['FilePath'].replace(/\\/g, '\\<wbr>').replace(/\//g, '\/<wbr>').replace(/_/g, '_<wbr>')));
            if (this.canEdit) {
                this.addElement(this.getPlotPointExclusionPanel());
            } else {
                this.addElement(this.getPlotPointDetailField('Status', this.pointData['IgnoreInQC'] ? 'Not included in QC' : 'Included in QC'));
            }

            var linksDiv = document.createElement('div');
            linksDiv.innerHTML = this.getPlotPointClickLinks();
            this.addElement(linksDiv);
        }

        if (this.renderTo) {
            var targetEl = typeof this.renderTo === 'string' ? document.getElementById(this.renderTo) : this.renderTo;
            if (targetEl) {
                targetEl.appendChild(this.containerEl);
            }
        }
    },

    addElement: function (el) {
        if (el && this.containerEl) {
            this.containerEl.appendChild(el);
        }
    },

    getPlotPointDetailField: function (label, value, includeCls) {
        var fieldDiv = document.createElement('div');
        fieldDiv.className = 'qc-hover-field' + (typeof includeCls === 'string' ? ' ' + includeCls : '');
        fieldDiv.style.width = '100%';
        fieldDiv.style.display = 'flex';
        fieldDiv.style.marginBottom = '5px';

        var labelSpan = document.createElement('span');
        labelSpan.className = 'qc-hover-field-label';
        labelSpan.style.display = 'inline-block';
        labelSpan.style.width = '120px';
        labelSpan.style.fontWeight = 'bold';
        labelSpan.style.flexShrink = '0';
        labelSpan.textContent = LABKEY.Utils.encodeHtml(label) + ':';

        var valueSpan = document.createElement('span');
        valueSpan.className = 'qc-hover-field-value';
        valueSpan.style.flex = '1';
        valueSpan.style.wordBreak = 'break-all';
        // file path is already getting formatted in the caller
        valueSpan.innerHTML = label === 'File Path' ? value : LABKEY.Utils.encodeHtml(value);

        fieldDiv.appendChild(labelSpan);
        fieldDiv.appendChild(valueSpan);

        return fieldDiv;
    },

    getPlotPointExclusionPanel: function () {
        if (!this.exclusionPanel) {
            this.exclusionPanel = document.createElement('div');
            this.exclusionPanel.className = 'qc-hover-exclusion-panel';
            this.exclusionPanel.style.margin = '10px 0';
            this.exclusionPanel.style.borderTop = 'solid #eeeeee 1px';
            this.exclusionPanel.style.borderBottom = 'solid #eeeeee 1px';
            this.exclusionPanel.style.padding = '10px 0';

            this.exclusionPanel.appendChild(this.getPlotPointExclusionRadioGroup());
            this.exclusionPanel.appendChild(this.getPlotPointExclusionSaveBtn());
        }

        return this.exclusionPanel;
    },

    getPlotPointExclusionSaveBtn: function () {
        if (!this.exclusionsSaveBtn) {
            var btnContainer = document.createElement('div');
            btnContainer.style.marginLeft = '120px';
            btnContainer.style.marginTop = '10px';

            this.exclusionsSaveBtn = document.createElement('button');
            this.exclusionsSaveBtn.textContent = 'Save';
            this.exclusionsSaveBtn.disabled = true;
            this.exclusionsSaveBtn.className = 'labkey-button';

            btnContainer.appendChild(this.exclusionsSaveBtn);

            var self = this;
            this.exclusionsSaveBtn.addEventListener('click', function () {
                var newStatus = self.getRadioGroupValue();
                if (newStatus !== self.originalStatus) {
                    self.showMask();

                    // Scenarios:
                    // 1 - from include to exclude metric - insert new row with MetricId
                    // 2 - from include to exclude all - delete all for replicate and then insert new row without MetricId
                    // 3 - from exclude metric to include - delete row for MetricId
                    // 4 - from exclude metric to exclude all - delete all for replicate and then insert new row without MetricId
                    // 5 - from exclude all to include - delete all for replicate
                    // 6 - from exclude all to exclude metric - delete all for replicate and then insert new row with MetricId
                    var s1 = self.originalStatus === self.STATE.INCLUDE && newStatus === self.STATE.EXCLUDE_METRIC;
                    var s2 = self.originalStatus === self.STATE.INCLUDE && newStatus === self.STATE.EXCLUDE_ALL;
                    var s3 = self.originalStatus === self.STATE.EXCLUDE_METRIC && newStatus === self.STATE.INCLUDE;
                    var s4 = self.originalStatus === self.STATE.EXCLUDE_METRIC && newStatus === self.STATE.EXCLUDE_ALL;
                    var s5 = self.originalStatus === self.STATE.EXCLUDE_ALL && newStatus === self.STATE.INCLUDE;
                    var s6 = self.originalStatus === self.STATE.EXCLUDE_ALL && newStatus === self.STATE.EXCLUDE_METRIC;

                    var commands = [];

                    if (self.existingExclusions.length > 0) {
                        // for scenarios s2, s4, s5, and s6 - delete all existing exclusions for this replicate
                        if (s2 || s4 || s5 || s6) {
                            commands.push({
                                schemaName: 'targetedms',
                                queryName: 'QCMetricExclusion',
                                command: 'delete',
                                rows: self.existingExclusions
                            });
                        }

                        // for scenario s3 - delete the existing exclusion for this replicate/metric
                        if (s3) {
                            var metricIds = self.existingExclusions.map(function (item) {
                                return item.MetricId;
                            });
                            commands.push({
                                schemaName: 'targetedms',
                                queryName: 'QCMetricExclusion',
                                command: 'delete',
                                rows: [self.existingExclusions[metricIds.indexOf(self.metricProps.id)]]
                            });
                        }
                    }

                    // for scenarios s1 and s6 - insert a new exclusion for this replicate/metric
                    if (s1 || s6) {
                        commands.push({
                            schemaName: 'targetedms',
                            queryName: 'QCMetricExclusion',
                            command: 'insert',
                            rows: [{ ReplicateId: self.pointData['ReplicateId'], MetricId: self.metricProps.id }]
                        });
                    }

                    // for scenarios s2 and s4 - insert a new exclusion for this replicate without a metric value
                    if (s2 || s4) {
                        commands.push({
                            schemaName: 'targetedms',
                            queryName: 'QCMetricExclusion',
                            command: 'insert',
                            rows: [{ ReplicateId: self.pointData['ReplicateId'] }]
                        });
                    }

                    LABKEY.Query.saveRows({
                        commands: commands,
                        scope: self,
                        success: function (data) {
                            // Issue 30343: need to reload the full page because the QC Summary webpart might be
                            // present and need to be updated according to the updated exclusion state.
                            window.location.reload();
                        }
                    });
                } else {
                    self.onClose();
                }
            });

            return btnContainer;
        }

        return this.exclusionsSaveBtn.parentNode;
    },

    getPlotPointExclusionRadioGroup: function () {
        if (!this.exclusionRadioGroup) {
            var radioGroupDiv = document.createElement('div');
            radioGroupDiv.className = 'qc-hover-field';
            radioGroupDiv.style.padding = '10px 0 0 0';
            radioGroupDiv.style.width = '100%';
            radioGroupDiv.style.display = 'flex';

            var labelDiv = document.createElement('div');
            labelDiv.style.display = 'inline-block';
            labelDiv.style.width = '120px';
            labelDiv.style.fontWeight = 'bold';
            labelDiv.style.flexShrink = '0';
            labelDiv.style.verticalAlign = 'top';
            labelDiv.textContent = 'Status:';
            radioGroupDiv.appendChild(labelDiv);

            var optionsDiv = document.createElement('div');
            optionsDiv.style.flex = '1';

            var self = this;
            var options = [
                { label: 'Include', value: this.STATE.INCLUDE, checked: this.originalStatus === this.STATE.INCLUDE },
                {
                    label: 'Exclude replicate for this metric',
                    value: this.STATE.EXCLUDE_METRIC,
                    checked: this.originalStatus === this.STATE.EXCLUDE_METRIC
                },
                {
                    label: 'Exclude replicate for all metrics',
                    value: this.STATE.EXCLUDE_ALL,
                    checked: this.originalStatus === this.STATE.EXCLUDE_ALL
                }
            ];

            options.forEach(function (option) {
                var optionDiv = document.createElement('div');
                optionDiv.style.marginBottom = '5px';

                var radio = document.createElement('input');
                radio.type = 'radio';
                radio.name = 'exclusion-status';
                radio.value = option.value;
                radio.checked = option.checked;
                radio.addEventListener('change', function () {
                    var currentValue = self.getRadioGroupValue();
                    self.exclusionsSaveBtn.disabled = (currentValue === self.originalStatus);
                });

                var label = document.createElement('label');
                label.style.marginLeft = '5px';
                label.textContent = option.label;

                optionDiv.appendChild(radio);
                optionDiv.appendChild(label);
                optionsDiv.appendChild(optionDiv);
            });

            radioGroupDiv.appendChild(optionsDiv);
            this.exclusionRadioGroup = radioGroupDiv;
        }

        return this.exclusionRadioGroup;
    },

    getRadioGroupValue: function () {
        if (this.exclusionRadioGroup) {
            var radios = this.exclusionRadioGroup.querySelectorAll('input[name="exclusion-status"]');
            for (var i = 0; i < radios.length; i++) {
                if (radios[i].checked) {
                    return parseInt(radios[i].value);
                }
            }
        }
        return this.STATE.INCLUDE;
    },

    showMask: function () {
        if (this.containerEl) {
            var mask = document.createElement('div');
            mask.style.position = 'absolute';
            mask.style.top = '0';
            mask.style.left = '0';
            mask.style.width = '100%';
            mask.style.height = '100%';
            mask.style.backgroundColor = 'rgba(255, 255, 255, 0.7)';
            mask.style.zIndex = '1000';
            this.containerEl.style.position = 'relative';
            this.containerEl.appendChild(mask);
        }
    },

    getPlotPointClickLinks: function () {
        //Choose action target based on precursor type
        var action = this.pointData['dataType'] === 'Peptide' ? "precursorAllChromatogramsChart" : "moleculePrecursorAllChromatogramsChart",
                url = LABKEY.ActionURL.buildURL('targetedms', action, LABKEY.ActionURL.getContainer(), {
                    id: this.pointData['PrecursorId'],
                    chromInfoId: this.pointData['PrecursorChromInfoId']
                });

        return LABKEY.Utils.textLink({
                    text: this.metricProps.precursorScoped ? 'View Chromatogram' : 'View Document',
                    href: this.metricProps.precursorScoped ? url + '#ChromInfo' + this.pointData['PrecursorChromInfoId'] : this.viewDocumentURL
                }) + ' ' +
                LABKEY.Utils.textLink({
                    text: 'View Replicate',
                    href: LABKEY.ActionURL.buildURL('targetedms', 'showSampleFile', LABKEY.ActionURL.getContainer(), { id: this.pointData['SampleFileId'] })
                });
    },

    getRunId: function () {
        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: 'SELECT SampleFileId.ReplicateId.RunId.Id as runId from PrecursorChromInfo',
            scope: this,
            success: function (results) {
                var runId;
                if (results && results.rows)
                    runId = results.rows[0]["runId"];

                this.viewDocumentURL = LABKEY.ActionURL.buildURL('targetedms', 'showPrecursorList', null, { id: runId });
                this.getExistingReplicateExclusions();
            }
        });
    },

    destroy: function () {
        if (this.containerEl && this.containerEl.parentNode) {
            this.containerEl.parentNode.removeChild(this.containerEl);
        }
    }
};