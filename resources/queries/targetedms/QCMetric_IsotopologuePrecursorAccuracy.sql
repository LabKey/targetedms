/*
 * Copyright (c) 2019-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
       PrecursorChromInfoId,
       x.SampleFileId AS SampleFileId,
       MetricValue
FROM
     (
     SELECT
            AVG(CAST(p.Value AS DOUBLE)) AS MetricValue,
            MIN(p.precursorchrominfoid) AS precursorchrominfoid,
            p.precursorchrominfoid.SampleFileId AS SampleFileId
     FROM PrecursorChromInfoAnnotation p
     WHERE Name = 'PrecursorAccuracy'
     GROUP BY p.precursorchrominfoid.PrecursorId.PeptideId.Sequence, p.precursorchrominfoid.SampleFileId
     ) x
INNER JOIN PrecursorChromInfo pci ON x.precursorchrominfoid = pci.Id
