SELECT
    PeptideGroupId,
    -- Explicitly cast for SQLServer to avoid trying to add as numeric types
    AminoAcid || CAST(Location AS VARCHAR) AS SiteLocation,
    AminoAcid,
    Location,
    PeptideModifiedSequence,
    Sequence @hidden,
    Modification,
    MIN(Id) AS Id @hidden,
    PreviousAA @hidden,
    NextAA @hidden,
    SampleName,

    MAX(MaxPercentModified) AS MaxPercentModified,
    SUM(PercentModified) AS PercentModified,
    SUM(TotalPercentModified) AS TotalPercentModified,

    MAX(ModificationCount) AS ModificationCount @hidden

FROM
    PTMPercentsGroupedPrepivot
GROUP BY
    SampleName,
    Sequence,
    PreviousAA,
    NextAA,
    PeptideModifiedSequence,
    PeptideGroupId,
    AminoAcid,
    Location,
    Modification
PIVOT PercentModified, TotalPercentModified BY SampleName