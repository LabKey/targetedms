
SELECT
       0 AS RowId,
       MIN(AcquiredTime) AS TrainingStart,
       MAX(AcquiredTime) AS TrainingEnd,
       CAST(NULL AS TIMESTAMP) AS ReferenceEnd
FROM targetedms.SampleFile WHERE AcquiredTime < COALESCE((SELECT MIN(TrainingStart) FROM GuideSet), curdate())