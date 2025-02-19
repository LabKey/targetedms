ALTER TABLE targetedms.QCMetricConfiguration RENAME COLUMN TimeValue To MinTimeValue;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN MinTimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TimeValueOption VARCHAR(10);