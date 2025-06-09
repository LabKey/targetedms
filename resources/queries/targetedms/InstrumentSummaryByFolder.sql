SELECT
       COUNT(DISTINCT ReplicateId.RunId) AS SkylineDocumentCount,
       COUNT(DISTINCT ReplicateId) AS ReplicateCount,
       MIN(AcquiredTime) AS FirstAcquisition,
       MAX(AcquiredTime) AS LastAcquisition,
       ReplicateId.RunId.Container,
       InstrumentNickname
FROM targetedms.SampleFile
GROUP BY
         ReplicateId.RunId.Container,
         InstrumentNickname