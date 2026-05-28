/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    PeptideGroupId,
    SiteLocation,
    AminoAcid,
    Location,
    PeptideModifiedSequence,
    Sequence @hidden,
    Modification,
    MIN(Id) AS Id @hidden,
    PreviousAA @hidden,
    NextAA @hidden,
    ReplicateName,

    MAX(MaxPercentModified) AS MaxPercentModified,
    SUM(PercentModified) AS PercentModified,
    SUM(TotalPercentModified) AS TotalPercentModified,

    MAX(ModificationCount) AS ModificationCount @hidden

FROM
    PTMPercentsGroupedPrepivotCache
GROUP BY
    ReplicateName,
    Sequence,
    PreviousAA,
    NextAA,
    PeptideModifiedSequence,
    PeptideGroupId,
    AminoAcid,
    Location,
    SiteLocation,
    Modification
PIVOT PercentModified, TotalPercentModified BY ReplicateName