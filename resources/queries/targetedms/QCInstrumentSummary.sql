SELECT
    sf.InstrumentId.model AS InstrumentName,
    sf.InstrumentSerialNumber AS SerialNumber,
    MIN(sf.AcquiredTime) AS StartDate,
    MAX(sf.AcquiredTime) AS EndDate,
    COUNT(DISTINCT sf.ReplicateId) AS NoOfReplicates
FROM targetedms.SampleFile sf
WHERE
    sf.InstrumentSerialNumber IS NOT NULL
GROUP BY
         sf.InstrumentSerialNumber,
         sf.InstrumentId.model