/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// Shared utilities for TargetedMS scheduling pages
// Expose on a namespaced object to avoid globals
(function(window) {
    const utils = {};

    const pad = function(n) { return (n < 10 ? '0' : '') + n; };

    // Date-only 'yyyy-MM-dd' wire form from local fields.
    utils.toDateValue = function(date) {
        return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());
    };

    // Format a Date as datetime-local's fixed 'yyyy-MM-ddTHH:mm' wire form from local fields; avoids DateFormat, which zeroes the time in timezones whose label contains a colon (e.g. Honolulu).
    utils.toDateTimeLocalValue = function(date) {
        return utils.toDateValue(date) + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
    };

    // Convert a CSS color string (named, rgb, hex) to standard 6-digit HEX color (#RRGGBB)
    utils.stringToColor = function(color) {
        if (!color) return '#888888';
        try {
            const tempElement = document.createElement('div');
            tempElement.style.color = color;
            document.body.appendChild(tempElement);
            const computedColor = window.getComputedStyle(tempElement).color;
            document.body.removeChild(tempElement);
            const rgbValues = computedColor.match(/\d+/g);
            if (rgbValues && rgbValues.length >= 3) {
                const r = parseInt(rgbValues[0], 10);
                const g = parseInt(rgbValues[1], 10);
                const b = parseInt(rgbValues[2], 10);
                return '#' + ((1 << 24) + (r << 16) + (g << 8) + b)
                    .toString(16)
                    .slice(1)
                    .toUpperCase();
            }
        } catch (e) {
            // fall through to default return
        }
        return '#888888';
    };

    // Pick black or white text for best contrast over a given hex background color
    utils.getContrastTextColor = function(hexColor) {
        if (!hexColor) return '#000000';
        const c = hexColor.replace('#', '');
        if (c.length !== 6) return '#000000';
        const r = parseInt(c.substring(0, 2), 16);
        const g = parseInt(c.substring(2, 4), 16);
        const b = parseInt(c.substring(4, 6), 16);
        const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return yiq >= 128 ? '#000000' : '#FFFFFF';
    };

    // Return a time-only format string without seconds/millis based on the container setting
    utils.getTimeOnlyFormat = function() {
        if (window.LABKEY && LABKEY.container?.formats?.timeFormat) {
            return LABKEY.container.formats.timeFormat.replace(':ss', '').replace('.SSS', '');
        }
        return 'HH:mm';
    };

    // Format a time range using DateFormat and time-only format
    utils.formatTimeRange = function(start, end) {
        const fmt = utils.getTimeOnlyFormat();
        return DateFormat.format.date(start, fmt) + ' - ' + DateFormat.format.date(end, fmt);
    };

    window.ScheduleUtils = utils;
})(window);
