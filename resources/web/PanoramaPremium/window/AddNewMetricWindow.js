/*
 * Copyright (c) 2019-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

(function($) {
    window.Panorama = window.Panorama || {};
    window.Panorama.Window = window.Panorama.Window || {};

    const DIALOG_ID = 'lk-custom-metric-dialog';
    let _config = null;
    let _queries = [];

    function closeDialog() {
        $('#' + DIALOG_ID).remove();
    }

    function showError(msg) {
        $('#lk-custom-metric-error').text(msg).show();
    }

    function clearErrors() {
        $('#lk-custom-metric-error').hide().text('');
        $('#lk-custom-metric-name, #lk-custom-metric-ylabel, #lk-custom-metric-query')
            .css('border-color', '');
    }

    function markInvalid($field) {
        $field.css('border-color', 'red');
    }

    function validate() {
        clearErrors();
        let isValid = true;

        if (!$('#lk-custom-metric-name').val().trim()) {
            markInvalid($('#lk-custom-metric-name'));
            isValid = false;
        }
        if (!$('#lk-custom-metric-ylabel').val().trim()) {
            markInvalid($('#lk-custom-metric-ylabel'));
            isValid = false;
        }
        if (!$('#lk-custom-metric-query').val()) {
            markInvalid($('#lk-custom-metric-query'));
            isValid = false;
        }

        if (!isValid) {
            showError('Please fill in all required fields.');
        }
        return isValid;
    }

    function validateQueryColumns(queryName, callback) {
        const requiredColumns = ['MetricValue', 'SampleFileId', 'PrecursorChromInfoId'];
        let query;
        for (let i = 0; i < _queries.length; i++) {
            if (_queries[i].name === queryName) {
                query = _queries[i];
                break;
            }
        }
        if (!query) {
            callback(null);
            return;
        }
        const presentNames = (query.columns || []).map(function(c) { return c.name; });
        const missing = requiredColumns.filter(function(c) { return presentNames.indexOf(c) < 0; });
        callback(missing.length > 0 ? missing : null);
    }

    function checkMetricNameExists(metricName, callback) {
        const filterArray = [LABKEY.Filter.create('Name', metricName, LABKEY.Filter.Types.EQUAL)];
        if (_config.operation === 'update' && _config.metric) {
            filterArray.push(LABKEY.Filter.create('id', _config.metric.id, LABKEY.Filter.Types.NOT_EQUAL));
        }
        LABKEY.Query.selectRows({
            containerPath: LABKEY.container.path,
            schemaName: 'targetedms',
            queryName: 'qcmetricconfiguration',
            filterArray: filterArray,
            success: function(data) { callback(data.rows.length > 0); },
            failure: function() { callback(false); }
        });
    }

    function save() {
        if (!validate()) return;

        const metricName = $('#lk-custom-metric-name').val().trim();
        const queryName = $('#lk-custom-metric-query').val();

        validateQueryColumns(queryName, function(missing) {
            if (missing) {
                showError('Query ' + queryName + ' is missing required column(s): ' + missing.join(', '));
                markInvalid($('#lk-custom-metric-query'));
                return;
            }

            checkMetricNameExists(metricName, function(exists) {
                if (exists) {
                    showError('A metric with the name "' + LABKEY.Utils.encodeHtml(metricName) + '" already exists. Please choose a different name.');
                    markInvalid($('#lk-custom-metric-name'));
                    return;
                }

                const newMetric = {
                    Name: metricName,
                    QueryName: queryName,
                    YAxisLabel: $('#lk-custom-metric-ylabel').val().trim(),
                    PrecursorScoped: $('input[name="customMetricType"]:checked').val() === 'precursor'
                };
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
                    }
                });
            });
        });
    }

    function deleteMetric() {
        if (!confirm('This will delete the "' + _config.metric.name + '" metric. Are you sure?')) return;

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
            }
        });
    }

    function populateQueriesSelect() {
        const $select = $('#lk-custom-metric-query');
        $select.empty().append($('<option>').val('').text('-- Select query --'));

        const sorted = _queries.slice().sort(function(a, b) {
            return LABKEY.internal.SortUtil.naturalSort(a.name, b.name);
        });
        sorted.forEach(function(q) {
            $select.append($('<option>').val(q.name).text(q.title || q.name));
        });

        const metric = _config && _config.metric;
        if (_config.operation === 'update' && metric && metric.QueryName) {
            $select.val(metric.QueryName);
        }
    }

    function buildDialogHtml() {
        const op = _config.operation;
        const metric = _config.metric || {};
        const isPrecursor = op === 'update' ? !!metric.PrecursorScoped : false;
        const title = op === 'insert' ? 'Add New Custom Metric' : 'Edit Custom Metric';

        return '<div id="' + DIALOG_ID + '" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9999;display:flex;align-items:center;justify-content:center;">'
            + '<div class="x4-window x4-window-default" style="min-width:480px;max-width:580px;">'
            +   '<div class="x4-window-header x4-window-header-default x4-window-header-default-top" style="padding:4px 8px;border:none;">'
            +     '<p class="x4-window-header-text-container-default" style="font-size:14px;margin:0;">' + LABKEY.Utils.encodeHtml(title) + '</p>'
            +   '</div>'
            +   '<div class="x4-window-body" style="background:white;padding:10px 12px;">'
            +     '<table style="border-collapse:collapse;width:100%;">'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-custom-metric-name">Metric Name *</label></td>'
            +           '<td style="padding:5px 0;"><input type="text" id="lk-custom-metric-name" style="width:100%;box-sizing:border-box;" value="' + LABKEY.Utils.encodeHtml(metric.name || '') + '"/></td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-custom-metric-ylabel">Y-Axis Label *</label></td>'
            +           '<td style="padding:5px 0;"><input type="text" id="lk-custom-metric-ylabel" style="width:100%;box-sizing:border-box;" value="' + LABKEY.Utils.encodeHtml(metric.YAxisLabel || '') + '"/></td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;">Metric Type</td>'
            +           '<td style="padding:5px 0;">'
            +             '<label style="margin-right:16px;"><input type="radio" name="customMetricType" value="replicate"' + (!isPrecursor ? ' checked' : '') + '> Replicate</label>'
            +             '<label><input type="radio" name="customMetricType" value="precursor"' + (isPrecursor ? ' checked' : '') + '> Precursor</label>'
            +           '</td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-custom-metric-query">Metrics Query *</label></td>'
            +           '<td style="padding:5px 0;"><select id="lk-custom-metric-query" style="width:100%;box-sizing:border-box;"><option value="">Loading...</option></select></td></tr>'
            +     '</table>'
            +     '<div id="lk-custom-metric-error" class="labkey-error" style="display:none;margin-top:8px;"></div>'
            +     '<div style="margin-top:12px;text-align:right;">'
            +       '<button type="button" class="labkey-button" id="lk-custom-metric-cancel">Cancel</button>'
            +       (op === 'update' ? ' <button type="button" class="labkey-button" id="lk-custom-metric-delete">Delete</button>' : '')
            +       ' <button type="button" class="labkey-button primary" id="lk-custom-metric-save">Save</button>'
            +     '</div>'
            +   '</div>'
            + '</div>'
            + '</div>';
    }

    window.Panorama.Window.AddCustomMetricWindow = {
        show: function(config) {
            _config = config;
            _queries = [];

            $('#' + DIALOG_ID).remove();
            $('body').append(buildDialogHtml());

            $('#lk-custom-metric-cancel').on('click', closeDialog);
            $('#lk-custom-metric-save').on('click', save);
            if (config.operation === 'update') {
                $('#lk-custom-metric-delete').on('click', deleteMetric);
            }

            $('#' + DIALOG_ID).on('click', function(e) {
                if (e.target === this) closeDialog();
            });

            LABKEY.Query.getQueries({
                schemaName: 'targetedms',
                success: function(queriesInfo) {
                    _queries = queriesInfo.queries || [];
                    populateQueriesSelect();
                }
            });
        }
    };
})(jQuery);
