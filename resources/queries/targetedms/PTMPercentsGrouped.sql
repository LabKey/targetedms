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
    PTMPercentsGroupedPrepivot
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