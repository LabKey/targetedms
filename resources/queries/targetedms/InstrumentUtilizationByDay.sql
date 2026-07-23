/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Number of sample files and runs acquired per day, by instrument, across all folders the user can read.
-- Consumed by the instrument utilization calendar and the "Runs by Day" grid on the Show Instrument page.
SELECT
    CAST(AcquisitionDay AS TIMESTAMP) AS AcquisitionDate,
    COUNT(*) AS ReplicateCount,
    COUNT(DISTINCT RunId) AS RunCount,
    InstrumentNickname
FROM
    (SELECT
        CAST(YEAR(AcquiredTime) AS VARCHAR) || '-' ||
            (CASE WHEN MONTH(AcquiredTime) < 10 THEN '0' ELSE '' END) || CAST(MONTH(AcquiredTime) AS VARCHAR) || '-' ||
            (CASE WHEN DAYOFMONTH(AcquiredTime) < 10 THEN '0' ELSE '' END) || CAST(DAYOFMONTH(AcquiredTime) AS VARCHAR) AS AcquisitionDay,
        ReplicateId.RunId AS RunId,
        InstrumentNickname
    FROM targetedms.SampleFile
    WHERE AcquiredTime IS NOT NULL) X
GROUP BY AcquisitionDay, InstrumentNickname
