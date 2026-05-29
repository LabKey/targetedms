/*
 * Copyright (c) 2019-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
       precursorchrominfoid AS PrecursorChromInfoId,
       precursorchrominfoid.SampleFileId AS SampleFileId,
       CAST(Value AS DOUBLE) AS MetricValue
FROM PrecursorChromInfoAnnotation

-- Pull only for unmodified variant
WHERE Name='RSquared' AND precursorchrominfoid.PrecursorId.ModifiedSequence NOT LIKE '%]%'