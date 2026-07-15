/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Number of sample files and runs acquired per month, by instrument, across all folders the user can read.
-- Consumed by the "Runs by Month" grid on the Show Instrument page.
SELECT
    CAST(MonthStart || '-01' AS TIMESTAMP) AS MonthStart,
    TIMESTAMPADD('SQL_TSI_MONTH', 1, CAST(MonthStart || '-01' AS TIMESTAMP)) AS MonthEnd,
    COUNT(*) AS FileCount,
    COUNT(DISTINCT RunId) AS RunCount,
    InstrumentNickname
FROM
    (SELECT
        CAST(YEAR(AcquiredTime) AS VARCHAR) || '-' ||
            (CASE WHEN MONTH(AcquiredTime) < 10 THEN '0' ELSE '' END) || CAST(MONTH(AcquiredTime) AS VARCHAR) AS MonthStart,
        ReplicateId.RunId AS RunId,
        InstrumentNickname
    FROM targetedms.SampleFile
    WHERE AcquiredTime IS NOT NULL) X
GROUP BY MonthStart, InstrumentNickname
