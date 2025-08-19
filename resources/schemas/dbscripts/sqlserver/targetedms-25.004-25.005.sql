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

EXEC sp_rename 'targetedms.QCMetricConfiguration.Series1QueryName', 'QueryName', 'COLUMN';
EXEC sp_rename 'targetedms.QCMetricConfiguration.YAxisLabel1', 'YAxisLabel', 'COLUMN';
