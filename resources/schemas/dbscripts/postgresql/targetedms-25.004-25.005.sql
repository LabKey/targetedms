DELETE FROM targetedms.QCMetricExclusion WHERE MetricId IN
    (SELECT Id FROM targetedms.QCMetricConfiguration WHERE Series2QueryName IS NOT NULL);

DELETE FROM targetedms.QCTraceMetricValues WHERE Metric IN
   (SELECT Id FROM targetedms.QCMetricConfiguration WHERE Series2QueryName IS NOT NULL);

DELETE FROM targetedms.QCEnabledMetrics WHERE Metric IN
    (SELECT Id FROM targetedms.QCMetricConfiguration WHERE Series2QueryName IS NOT NULL);

DELETE FROM targetedms.QCMetricConfiguration WHERE Series2QueryName IS NOT NULL;

ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN Series1Label;
ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN Series2Label;
ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN Series1SchemaName;
ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN Series2SchemaName;
ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN EnabledSchemaName;
ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN Series2QueryName;
ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN YAxisLabel2;

ALTER TABLE targetedms.QCMetricConfiguration RENAME COLUMN Series1QueryName TO QueryName;
ALTER TABLE targetedms.QCMetricConfiguration RENAME COLUMN YAxisLabel1 TO YAxisLabel;
