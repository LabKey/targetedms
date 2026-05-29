/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
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