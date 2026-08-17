/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

(function($) {
    window.Panorama = window.Panorama || {};
    window.Panorama.Window = window.Panorama.Window || {};

    const DIALOG_ID = 'lk-trace-metric-dialog';
    const TIME_VALUE_OPTIONS = ['First', 'Last', 'Max', 'Min'];
    let _config = null;

    function closeDialog() {
        $('#' + DIALOG_ID).remove();
    }

    function showError(msg) {
        $('#lk-trace-metric-error').text(msg).show();
    }

    function clearErrors() {
        $('#lk-trace-metric-error').hide().text('');
        $('#lk-trace-metric-name, #lk-trace-use-trace, #lk-trace-ylabel, #lk-trace-time-option, #lk-trace-min-time, #lk-trace-max-time, #lk-trace-value')
            .css('border-color', '');
    }

    function markInvalid($field) {
        $field.css('border-color', 'red');
    }

    // Disable the buttons while a save/delete is in flight so we don't submit twice
    function setBusy(buttonId, busyText) {
        $('#lk-trace-metric-save, #lk-trace-metric-cancel, #lk-trace-metric-delete').prop('disabled', true);
        $('#' + buttonId).html(busyText + ' <i class="fa fa-spinner fa-pulse"></i>');
    }

    function clearBusy() {
        $('#lk-trace-metric-save, #lk-trace-metric-cancel, #lk-trace-metric-delete').prop('disabled', false);
        $('#lk-trace-metric-save').text('Save');
        $('#lk-trace-metric-delete').text('Delete');
    }

    function getMode() {
        return $('input[name="metricValue"]:checked').val(); // 'timeValue' or 'traceValue'
    }

    // Enable only the fields belonging to the selected mode
    function refreshMode() {
        const isTime = getMode() === 'timeValue';
        $('#lk-trace-time-option, #lk-trace-min-time, #lk-trace-max-time').prop('disabled', !isTime);
        $('#lk-trace-value').prop('disabled', isTime);
    }

    function isNonNegativeNumber(val) {
        return val !== '' && val !== null && !isNaN(val) && Number(val) >= 0;
    }

    // A zero trace value is treated as "not set", so require a positive number here
    function isPositiveNumber(val) {
        return isNonNegativeNumber(val) && Number(val) > 0;
    }

    function validate() {
        clearErrors();
        let isValid = true;

        if (!$('#lk-trace-metric-name').val().trim()) {
            markInvalid($('#lk-trace-metric-name'));
            isValid = false;
        }
        if (!$('#lk-trace-use-trace').val()) {
            markInvalid($('#lk-trace-use-trace'));
            isValid = false;
        }
        if (!$('#lk-trace-ylabel').val().trim()) {
            markInvalid($('#lk-trace-ylabel'));
            isValid = false;
        }

        if (getMode() === 'timeValue') {
            if (!$('#lk-trace-time-option').val()) {
                markInvalid($('#lk-trace-time-option'));
                isValid = false;
            }
            if (!isNonNegativeNumber($('#lk-trace-min-time').val())) {
                markInvalid($('#lk-trace-min-time'));
                isValid = false;
            }
            if (!isNonNegativeNumber($('#lk-trace-max-time').val())) {
                markInvalid($('#lk-trace-max-time'));
                isValid = false;
            }
        }
        else if (!isPositiveNumber($('#lk-trace-value').val())) {
            markInvalid($('#lk-trace-value'));
            isValid = false;
        }

        if (!isValid) {
            showError('Please fill in all required fields.');
        }
        return isValid;
    }

    function checkMetricNameExists(metricName, callback) {
        const filterArray = [LABKEY.Filter.create('Name', metricName, LABKEY.Filter.Types.EQUAL)];
        if (_config.operation === 'update' && _config.metric) {
            filterArray.push(LABKEY.Filter.create('id', _config.metric.id, LABKEY.Filter.Types.NOT_EQUAL));
        }
        LABKEY.Query.selectRows({
            containerPath: LABKEY.container.id,
            schemaName: 'targetedms',
            queryName: 'qcmetricconfiguration',
            filterArray: filterArray,
            success: function(data) { callback(data.rows.length > 0); },
            failure: function() { callback(false); }
        });
    }

    function save() {
        if (!validate()) return;

        setBusy('lk-trace-metric-save', 'Saving');

        const metricName = $('#lk-trace-metric-name').val().trim();
        checkMetricNameExists(metricName, function(exists) {
            if (exists) {
                showError('A metric with the name "' + metricName + '" already exists. Please choose a different name.');
                markInvalid($('#lk-trace-metric-name'));
                clearBusy();
                return;
            }

            const newMetric = {
                Name: metricName,
                QueryName: 'QCTraceMetric', // dummy text to insert and not an actual query
                PrecursorScoped: false,
                TraceName: $('#lk-trace-use-trace').val(),
                YAxisLabel: $('#lk-trace-ylabel').val().trim()
            };

            if (getMode() === 'traceValue') {
                newMetric.TraceValue = Number($('#lk-trace-value').val());
            }
            else {
                newMetric.TimeValueOption = $('#lk-trace-time-option').val();
                newMetric.MinTimeValue = Number($('#lk-trace-min-time').val());
                newMetric.MaxTimeValue = Number($('#lk-trace-max-time').val());
            }

            if (_config.operation === 'update') {
                newMetric.id = _config.metric.id;
            }

            LABKEY.Query.saveRows({
                containerPath: LABKEY.container.id,
                commands: [{ schemaName: 'targetedms', queryName: 'qcmetricconfiguration', command: _config.operation, rows: [newMetric] }],
                method: 'POST',
                success: function() { window.location.reload(); },
                failure: function(response) {
                    showError((response && (response.exception || response.message)) || 'Error saving metric');
                    clearBusy();
                }
            });
        });
    }

    function deleteMetric() {
        if (!confirm('This will delete the "' + _config.metric.name + '" metric. Are you sure?')) return;

        setBusy('lk-trace-metric-delete', 'Deleting');

        LABKEY.Query.saveRows({
            containerPath: LABKEY.container.id,
            commands: [
                { schemaName: 'targetedms', queryName: 'qcenabledmetrics', command: 'delete', rows: [{ metric: _config.metric.id }] },
                { schemaName: 'targetedms', queryName: 'qcmetricconfiguration', command: 'delete', rows: [{ id: _config.metric.id }] }
            ],
            method: 'POST',
            success: function() { window.location.reload(); },
            failure: function(response) {
                showError((response && (response.exception || response.message)) || 'Error deleting metric');
                clearBusy();
            }
        });
    }

    function buildTraceOptions(selectedTrace) {
        const textIds = (_config.traces || []).map(function(row) { return row.TextId; });
        // keep the previously saved trace available even if it is no longer in the list
        if (selectedTrace && textIds.indexOf(selectedTrace) === -1) {
            textIds.push(selectedTrace);
        }
        textIds.sort(function(a, b) {
            return String(a).localeCompare(String(b));
        });
        let html = '<option value="">-- Select trace --</option>';
        textIds.forEach(function(textId) {
            const sel = textId === selectedTrace ? ' selected' : '';
            html += '<option value="' + LABKEY.Utils.encodeHtml(textId) + '"' + sel + '>' + LABKEY.Utils.encodeHtml(textId) + '</option>';
        });
        return html;
    }

    function buildTimeOptions(selectedOption) {
        let html = '<option value=""></option>';
        TIME_VALUE_OPTIONS.forEach(function(opt) {
            const sel = opt === selectedOption ? ' selected' : '';
            html += '<option value="' + opt + '"' + sel + '>' + opt + '</option>';
        });
        return html;
    }

    function buildDialogHtml() {
        const op = _config.operation;
        const metric = _config.metric || {};
        const tracesPresent = !!_config.tracesPresent;
        const title = op === 'insert' ? 'Add New Trace Metric' : 'Edit Trace Metric';

        // In update mode, pick the mode based on the stored values; default to the time-value mode.
        const isTraceValueMode = op === 'update' && metric.TraceValue > 0;

        const num = function(v) {
            return (v !== undefined && v !== null) ? LABKEY.Utils.encodeHtml(v) : '';
        };

        const traceSelect = tracesPresent
            ? '<select id="lk-trace-use-trace" style="width:100%;box-sizing:border-box;">' + buildTraceOptions(metric.TraceName) + '</select>'
            : '<select id="lk-trace-use-trace" style="width:100%;box-sizing:border-box;" disabled><option value="">No trace can be found</option></select>';

        return '<div id="' + DIALOG_ID + '" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9999;display:flex;align-items:center;justify-content:center;">'
            + '<div class="x4-window x4-window-default" style="min-width:640px;max-width:760px;">'
            +   '<div class="x4-window-header x4-window-header-default x4-window-header-default-top" style="padding:4px 8px;border:none;">'
            +     '<p class="x4-window-header-text-container-default" style="font-size:14px;margin:0;">' + LABKEY.Utils.encodeHtml(title) + '</p>'
            +   '</div>'
            +   '<div class="x4-window-body" style="background:white;padding:10px 12px;">'
            +     '<table style="border-collapse:collapse;width:100%;">'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-trace-metric-name">Metric Name *</label></td>'
            +           '<td style="padding:5px 0;"><input type="text" id="lk-trace-metric-name" style="width:100%;box-sizing:border-box;" value="' + num(metric.name) + '"/></td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-trace-use-trace">Use Trace *</label></td>'
            +           '<td style="padding:5px 0;">' + traceSelect + '</td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-trace-ylabel">Y Axis Label *</label></td>'
            +           '<td style="padding:5px 0;"><input type="text" id="lk-trace-ylabel" style="width:100%;box-sizing:border-box;" value="' + num(metric.YAxisLabel) + '"/></td></tr>'
            +     '</table>'
            +     '<div style="margin-top:10px;">'
            +       '<div style="display:flex;align-items:center;flex-wrap:wrap;gap:5px;margin-bottom:8px;">'
            +         '<label style="margin:0;"><input type="radio" name="metricValue" value="timeValue"' + (!isTraceValueMode ? ' checked' : '') + '> Use the</label>'
            +         '<select id="lk-trace-time-option" style="width:70px;">' + buildTimeOptions(metric.TimeValueOption) + '</select>'
            +         '<span>trace value when time in minutes is between</span>'
            +         '<input type="number" id="lk-trace-min-time" style="width:70px;" value="' + num(metric.MinTimeValue) + '"/>'
            +         '<span>and</span>'
            +         '<input type="number" id="lk-trace-max-time" style="width:70px;" value="' + num(metric.MaxTimeValue) + '"/>'
            +       '</div>'
            +       '<div style="display:flex;align-items:center;flex-wrap:wrap;gap:5px;">'
            +         '<label style="margin:0;"><input type="radio" name="metricValue" value="traceValue"' + (isTraceValueMode ? ' checked' : '') + '> Use time in minutes when the trace first reaches a value greater than or equal to</label>'
            +         '<input type="number" id="lk-trace-value" style="width:70px;" value="' + num(metric.TraceValue) + '"/>'
            +       '</div>'
            +     '</div>'
            +     '<div id="lk-trace-metric-error" class="labkey-error" style="display:none;margin-top:8px;"></div>'
            +     '<div style="margin-top:12px;text-align:right;">'
            +       '<button type="button" class="labkey-button" id="lk-trace-metric-cancel">Cancel</button>'
            +       (op === 'update' ? ' <button type="button" class="labkey-button" id="lk-trace-metric-delete">Delete</button>' : '')
            +       ' <button type="button" class="labkey-button primary" id="lk-trace-metric-save"' + (tracesPresent ? '' : ' disabled') + '>Save</button>'
            +     '</div>'
            +   '</div>'
            + '</div>'
            + '</div>';
    }

    window.Panorama.Window.AddTraceMetricWindow = {
        show: function(config) {
            _config = config;

            $('#' + DIALOG_ID).remove();
            $('body').append(buildDialogHtml());

            $('#lk-trace-metric-cancel').on('click', closeDialog);
            $('#lk-trace-metric-save').on('click', save);
            if (config.operation === 'update') {
                $('#lk-trace-metric-delete').on('click', deleteMetric);
            }
            $('input[name="metricValue"]').on('change', refreshMode);

            // close on overlay click
            $('#' + DIALOG_ID).on('click', function(e) {
                if (e.target === this) closeDialog();
            });

            refreshMode();
        }
    };
})(jQuery);
