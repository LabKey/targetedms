SELECT
    PeptideGroupId,
    SiteLocation,
    PeptideModifiedSequence,
    Sequence @hidden,
    Modification,
    MIN(Id) AS Id @hidden,
    PreviousAA @hidden,
    NextAA @hidden,
    SampleName,
    SUM(PercentModified) AS PercentModified,
    SUM(TotalPercentModified) AS TotalPercentModified,
    ModificationCount @hidden

FROM
    PTMPercentsGroupedPrepivot
GROUP BY
    SampleName,
    Sequence,
    PreviousAA,
    NextAA,
    PeptideModifiedSequence,
    PeptideGroupId,
    SiteLocation,
    Modification,
    ModificationCount
PIVOT PercentModified, TotalPercentModified BY SampleName