ALTER TABLE targetedms.QCMetricConfiguration RENAME COLUMN TimeValue To MinTimeValue;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN MaxTimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TimeValueOption VARCHAR(10);