ALTER TABLE targetedms.QCEnabledMetrics ADD COLUMN Status VARCHAR(20);
UPDATE targetedms.QCEnabledMetrics SET Status = 'Disabled' WHERE Enabled = false;
UPDATE targetedms.QCEnabledMetrics SET Status = 'LeveyJennings' WHERE Enabled = true;
ALTER TABLE targetedms.QCEnabledMetrics DROP COLUMN Enabled;
