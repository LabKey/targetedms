
SELECT
       0 AS RowId,
       MIN(AcquiredTime) AS TrainingStart,
       MAX(AcquiredTime) AS TrainingEnd,
       MAX(AcquiredTime) AS ReferenceEnd
FROM targetedms.SampleFile WHERE AcquiredTime < (SELECT MIN(TrainingStart) FROM GuideSet)