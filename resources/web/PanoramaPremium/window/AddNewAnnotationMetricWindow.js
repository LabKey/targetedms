/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

(function($) {
    window.Panorama = window.Panorama || {};
    window.Panorama.Window = window.Panorama.Window || {};

    const DIALOG_ID = 'lk-annotation-metric-dialog';
    let _config = null;
    let _allAnnotations = [];

    function closeDialog() {
        $(document).off('keydown.lkAnnotationMetric');
        $('#' + DIALOG_ID).remove();
    }

    // Ext.window.Window used to keep Tab inside the modal; do the same by hand
    function trapFocus(e) {
        const $focusable = $('#' + DIALOG_ID).find('input, select, button').filter(':visible').not(':disabled');
        if ($focusable.length === 0) {
            return;
        }
        const first = $focusable[0];
        const last = $focusable[$focusable.length - 1];
        if (e.shiftKey && e.target === first) {
            e.preventDefault();
            last.focus();
        }
        else if (!e.shiftKey && e.target === last) {
            e.preventDefault();
            first.focus();
        }
    }

    function showError(msg) {
        $('#lk-annotation-metric-error').text(msg).show();
    }

    function clearErrors() {
        $('#lk-annotation-metric-error').hide().text('');
        $('#lk-annotation-metric-name, #lk-annotation-metric-ylabel, #lk-annotation-name-select')
            .css('border-color', '');
    }

    function markInvalid($field) {
        $field.css('border-color', 'red');
    }

    function validate() {
        clearErrors();
        let isValid = true;

        if (!$('#lk-annotation-metric-name').val().trim()) {
            markInvalid($('#lk-annotation-metric-name'));
            isValid = false;
        }
        if (!$('#lk-annotation-metric-ylabel').val().trim()) {
            markInvalid($('#lk-annotation-metric-ylabel'));
            isValid = false;
        }
        if (!$('#lk-annotation-name-select').val()) {
            markInvalid($('#lk-annotation-name-select'));
            isValid = false;
        }

        if (!isValid) {
            showError('Please fill in all required fields.');
        }
        return isValid;
    }

    function getAnnotationTarget() {
        return $('input[name="annotationType"]:checked').val() === 'precursor'
            ? 'precursor_result'
            : 'replicate';
    }

    function getFilteredAnnotations() {
        const target = getAnnotationTarget();
        const seen = {};
        const result = [];
        _allAnnotations.forEach(function(row) {
            const targets = (row['Targets'] || '').split(',').map(function(s) { return s.trim(); });
            if (targets.indexOf(target) >= 0 && !seen[row['Name']]) {
                seen[row['Name']] = true;
                result.push(row['Name']);
            }
        });
        result.sort();
        return result;
    }

    function refreshAnnotationsSelect() {
        const $select = $('#lk-annotation-name-select');
        const currentVal = $select.val();
        $select.empty().append($('<option>').val('').text('-- Select annotation --'));
        getFilteredAnnotations().forEach(function(name) {
            $select.append($('<option>').val(name).text(name));
        });

        const metric = _config && _config.metric;
        if (_config.operation === 'update' && metric && metric.AnnotationName) {
            $select.val(metric.AnnotationName);
        } else if (currentVal) {
            $select.val(currentVal);
        }
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

        const metricName = $('#lk-annotation-metric-name').val().trim();
        checkMetricNameExists(metricName, function(exists) {
            if (exists) {
                showError('A metric with the name "' + LABKEY.Utils.encodeHtml(metricName) + '" already exists. Please choose a different name.');
                markInvalid($('#lk-annotation-metric-name'));
                return;
            }

            const newMetric = {
                Name: metricName,
                QueryName: 'QCAnnotationMetric',
                YAxisLabel: $('#lk-annotation-metric-ylabel').val().trim(),
                PrecursorScoped: $('input[name="annotationType"]:checked').val() === 'precursor',
                AnnotationName: $('#lk-annotation-name-select').val()
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
    }

    function deleteMetric() {
        if (!confirm('This will delete the "' + _config.metric.name + '" metric. Are you sure?')) return;

        LABKEY.Query.saveRows({
            containerPath: LABKEY.container.path,
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

    function buildDialogHtml() {
        const op = _config.operation;
        const metric = _config.metric || {};
        const isPrecursor = op === 'update' && metric.PrecursorScoped;
        const title = op === 'insert' ? 'Add Annotation-Backed Metric' : 'Edit Annotation-Backed Metric';

        return '<div id="' + DIALOG_ID + '" role="dialog" aria-modal="true" aria-labelledby="lk-annotation-metric-title"'
            + ' style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9999;display:flex;align-items:center;justify-content:center;">'
            + '<div class="x4-window x4-window-default" style="min-width:480px;max-width:580px;">'
            // The x4-window* classes come from the Ext4 stylesheet, which configureQCMetric.view.xml
            // still declares. Dropping that dependency strips this dialog's border, header and background.
            +   '<div class="x4-window-header x4-window-header-default x4-window-header-default-top" style="padding:4px 8px;border:none;">'
            +     '<p class="x4-window-header-text-container-default" id="lk-annotation-metric-title" style="font-size:14px;margin:0;">' + LABKEY.Utils.encodeHtml(title) + '</p>'
            +   '</div>'
            +   '<div class="x4-window-body" style="background:white;padding:10px 12px;">'
            +     '<table style="border-collapse:collapse;width:100%;">'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-annotation-metric-name">Metric Name *</label></td>'
            +           '<td style="padding:5px 0;"><input type="text" id="lk-annotation-metric-name" style="width:100%;box-sizing:border-box;" value="' + LABKEY.Utils.encodeHtml(metric.name || '') + '"/></td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-annotation-metric-ylabel">Y-Axis Label *</label></td>'
            +           '<td style="padding:5px 0;"><input type="text" id="lk-annotation-metric-ylabel" style="width:100%;box-sizing:border-box;" value="' + LABKEY.Utils.encodeHtml(metric.YAxisLabel || '') + '"/></td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;">Annotation Type</td>'
            +           '<td style="padding:5px 0;">'
            +             '<label style="margin-right:16px;"><input type="radio" name="annotationType" value="replicate"' + (!isPrecursor ? ' checked' : '') + '> Replicate</label>'
            +             '<label><input type="radio" name="annotationType" value="precursor"' + (isPrecursor ? ' checked' : '') + '> Precursor</label>'
            +           '</td></tr>'
            +       '<tr><td style="padding:5px 10px 5px 0;white-space:nowrap;"><label for="lk-annotation-name-select">Annotation *</label></td>'
            +           '<td style="padding:5px 0;"><select id="lk-annotation-name-select" style="width:100%;box-sizing:border-box;"><option value="">Loading...</option></select></td></tr>'
            +     '</table>'
            +     '<div id="lk-annotation-metric-error" class="labkey-error" style="display:none;margin-top:8px;"></div>'
            +     '<div style="margin-top:12px;text-align:right;">'
            +       '<button type="button" class="labkey-button" id="lk-annotation-metric-cancel">Cancel</button>'
            +       (op === 'update' ? ' <button type="button" class="labkey-button" id="lk-annotation-metric-delete">Delete</button>' : '')
            +       ' <button type="button" class="labkey-button primary" id="lk-annotation-metric-save">Save</button>'
            +     '</div>'
            +   '</div>'
            + '</div>'
            + '</div>';
    }

    window.Panorama.Window.AddAnnotationMetricWindow = {
        show: function(config) {
            _config = config;
            _allAnnotations = [];

            closeDialog();
            $('body').append(buildDialogHtml());

            $('#lk-annotation-metric-cancel').on('click', closeDialog);
            $('#lk-annotation-metric-save').on('click', save);
            if (config.operation === 'update') {
                $('#lk-annotation-metric-delete').on('click', deleteMetric);
            }
            $('input[name="annotationType"]').on('change', refreshAnnotationsSelect);

            // close on overlay click
            $('#' + DIALOG_ID).on('click', function(e) {
                if (e.target === this) closeDialog();
            });

            $('#lk-annotation-metric-name').trigger('focus');

            $(document).on('keydown.lkAnnotationMetric', function(e) {
                if (e.key === 'Escape') {
                    closeDialog();
                }
                else if (e.key === 'Tab') {
                    trapFocus(e);
                }
            });

            LABKEY.Query.selectRows({
                schemaName: 'targetedms',
                queryName: 'AnnotationSettings',
                columns: ['Name', 'Targets', 'Type'],
                filterArray: [LABKEY.Filter.create('Type', 'number', LABKEY.Filter.Types.EQUAL)],
                success: function(data) {
                    _allAnnotations = data.rows || [];
                    refreshAnnotationsSelect();
                }
            });
        }
    };
})(jQuery);
