
SELECT
       0 AS RowId,
       COALESCE(MIN(AcquiredTime), curdate()) AS TrainingStart,
       COALESCE(MAX(AcquiredTime), curdate()) AS TrainingEnd,
       CAST(NULL AS TIMESTAMP) AS ReferenceEnd
FROM targetedms.SampleFile WHERE AcquiredTime < COALESCE((SELECT MIN(TrainingStart) FROM GuideSet), curdate())