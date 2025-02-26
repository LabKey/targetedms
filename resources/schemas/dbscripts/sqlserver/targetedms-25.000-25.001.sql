EXEC sp_rename 'targetedms.QCMetricConfiguration.TimeValue', 'MinTimeValue', 'COLUMN';
GO
ALTER TABLE targetedms.QCMetricConfiguration ADD MaxTimeValue REAL;
GO
ALTER TABLE targetedms.QCMetricConfiguration ADD TimeValueOption NVARCHAR(10);
GO

UPDATE targetedms.QCMetricConfiguration SET MaxTimeValue = 1000 WHERE MinTimeValue IS NOT NULL;
GO
UPDATE targetedms.QCMetricConfiguration SET TimeValueOption = 'First' WHERE MinTimeValue IS NOT NULL;
GO