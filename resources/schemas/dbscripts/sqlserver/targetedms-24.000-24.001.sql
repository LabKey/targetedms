ALTER TABLE targetedms.QCEnabledMetrics ADD Status NVARCHAR(20);
GO
UPDATE targetedms.QCEnabledMetrics SET Status = 'Disabled' WHERE Enabled = 0;
UPDATE targetedms.QCEnabledMetrics SET Status = 'LeveyJennings' WHERE Enabled = 1;
ALTER TABLE targetedms.QCEnabledMetrics DROP COLUMN Enabled;
GO
UPDATE targetedms.QCEnabledMetrics SET UpperBound = 3, LowerBound = 3 WHERE Status = 'LeveyJennings';
