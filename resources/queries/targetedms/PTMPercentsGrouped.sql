SELECT
    PeptideGroupId,
    SiteLocation,
    PeptideModifiedSequence,
    Sequence @hidden,
    -- We have a special rule for this C-term modification - we're actually interested in the _unmodified_ percentage
    (CASE WHEN Modification.Name = 'Lys-loss (Protein C-term K)' THEN 'C-term K' ELSE Modification.Name END) AS Modification,
    MIN(Id) AS Id @hidden,
    PreviousAA @hidden,
    NextAA @hidden,
    SampleName,
    (CASE WHEN Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - MAX(MinPercentModified)) ELSE MAX(MaxPercentModified) END) AS MaxPercentModified,
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
    SiteLocation,
    Modification.Name,
    ModificationCount
PIVOT PercentModified, TotalPercentModified BY SampleName