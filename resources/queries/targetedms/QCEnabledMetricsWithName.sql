-- this query is used to export metric settings during a folder export
SELECT
metric.name AS metric,
enabled,
lowerBound,
upperBound,
cusumLimit
FROM
targetedms.QCEnabledMetrics