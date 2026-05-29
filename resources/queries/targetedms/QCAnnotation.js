/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function beforeInsert(row, errors) {
    if (row.date && row.date.getTime && row.enddate && row.enddate.getTime) {
        if (row.enddate.getTime() < row.date.getTime()) {
            errors.enddate = 'End date cannot be before the start date';
        }
    }
}

function beforeUpdate(row, oldRow, errors) {
    beforeInsert(row, errors);
}