SELECT
    PeptideGroupId,
    -- Explicitly cast for SQLServer to avoid trying to add as numeric types
    AminoAcid || CAST(Location AS VARCHAR) AS SiteLocation,
    AminoAcid,
    Location,
    PeptideModifiedSequence,
    Sequence @hidden,
    -- We have a special rule for this C-term modification - we're actually interested in the _unmodified_ percentage
    (CASE WHEN Modification.Name = 'Lys-loss (Protein C-term K)' THEN 'C-term K' ELSE Modification.Name END) AS Modification,
    MIN(Id) AS Id @hidden,
    PreviousAA @hidden,
    NextAA @hidden,
    SampleName,
    -- Normally we want to show the max percentage modified across all replicates (5%, 10%, 20% -> 20% as the worst case)
    -- However, for this one special modification, we invert the percentage. So 95% -> 5%, 90% -> 10%, 80% -> 20%. Thus,
    -- we need to find the one with the lowest value, which becomes the highest percentage when we subtract it from 100%.
    (CASE WHEN Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - MIN(MinPercentModified)) ELSE MAX(MaxPercentModified) END) AS MaxPercentModified,
    (CASE WHEN Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - SUM(PercentModified)) ELSE SUM(PercentModified) END) AS PercentModified,
    (CASE WHEN Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - SUM(TotalPercentModified)) ELSE SUM(TotalPercentModified) END) AS TotalPercentModified,
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
    AminoAcid,
    Location,
    Modification.Name,
    ModificationCount
PIVOT PercentModified, TotalPercentModified BY SampleName