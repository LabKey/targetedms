SELECT
    p.Id as project @hidden,
    pm.UWBudgetNumber,
    pm.name,
    pm.isCurrent,
    pm.budgetExpirationDate,
    rt.setupFee,
    timestampdiff('SQL_TSI_HOUR', isc.startTime, isc.endTime) * ir.fee + (SUM(iup.percentPayment * ir.fee) / 100) AS InstrumentCost,
    timestampdiff('SQL_TSI_HOUR', isc.startTime, isc.endTime) * ir.fee + (SUM(iup.percentPayment * ir.fee) / 100) + rt.setupFee AS TotalCost
FROM paymentmethod pm
INNER JOIN projectPaymentMethod ppm ON ppm.paymentMethod = pm.id
INNER JOIN msProject p ON ppm.project = p.id
LEFT JOIN instrumentUsagePayment iup ON iup.paymentMethod = pm.id
LEFT JOIN instrumentSchedule isc ON iup.instrumentScheduleId = isc.id
LEFT JOIN instrumentRate ir ON isc.instrumentRate = ir.id
LEFT JOIN rateType rt ON ir.rateType = rt.id
GROUP BY p.title, pm.UWBudgetNumber, pm.name, pm.isCurrent, pm.budgetExpirationDate, rt.setupFee,ir.fee, isc.startTime, isc.endTime, p.Id