function beforeInsert(row, errors) {
    if (row.date && row.enddate) {
        if (row.enddate.getTime() < row.date.getTime()) {
            errors.enddate = 'End date cannot be before the start date';
        }
    }
}

function beforeUpdate(row, oldRow, errors) {
    beforeInsert(row, errors);
}