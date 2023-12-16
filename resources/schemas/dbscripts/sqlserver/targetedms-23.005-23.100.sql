ALTER TABLE targetedms.QCEnabledMetrics ADD Status NVARCHAR(20);
UPDATE targetedms.QCEnabledMetrics SET Status = 'Disabled' WHERE Enabled = 0;
UPDATE targetedms.QCEnabledMetrics SET Status = 'LeveyJennings' WHERE Enabled = 1;
ALTER TABLE targetedms.QCEnabledMetrics DROP COLUMN Enabled;
