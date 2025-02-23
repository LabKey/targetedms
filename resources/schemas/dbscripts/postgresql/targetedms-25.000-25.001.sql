ALTER TABLE targetedms.QCMetricConfiguration RENAME COLUMN TimeValue To MinTimeValue;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN MaxTimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TimeValueOption VARCHAR(10);

UPDATE targetedms.QCMetricConfiguration SET MaxTimeValue = 1000 WHERE MinTimeValue IS NOT NULL;
UPDATE targetedms.QCMetricConfiguration SET TimeValueOption = 'First' WHERE MinTimeValue IS NOT NULL;