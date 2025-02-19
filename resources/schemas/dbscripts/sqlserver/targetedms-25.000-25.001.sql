EXEC sp_rename 'targetedms.QCMetricConfiguration.TimeValue', 'MinTimeValue', 'COLUMN';
GO
ALTER TABLE targetedms.QCMetricConfiguration ADD MaxTimeValue REAL;
GO
ALTER TABLE targetedms.QCMetricConfiguration ADD TimeValueOption NVARCHAR(10);
GO