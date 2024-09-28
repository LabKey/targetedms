--Calculate the percentage for each individual modification
WITH InitialGrouping AS (
    (SELECT StructuralModId                                                                  AS Modification,
            SUM(ModifiedAreaProportion)                                                      AS PercentModified,
            MIN(Id)                                                                          AS Id,
            MAX(PeptideModifiedSequence) AS PeptideModifiedSequence,
            Sequence @hidden,
            PreviousAA @hidden,
            NextAA @hidden,
            SampleFileId,
            IndexAA,
            SUBSTRING(Sequence, IndexAA + 1, 1) AS AminoAcid,
            StartIndex + IndexAA + 1 AS Location,
            SiteLocation,
            PeptideGroupId
     FROM PTMPercentsPrepivot
     GROUP BY SampleFileId,
              Sequence,
              PreviousAA,
              NextAA,
              StartIndex,
              PeptideGroupId,
              IndexAA,
              StructuralModId,
              SiteLocation)
),
Summarized AS (
    SELECT SUM(PercentModified) AS TotalPercentModified,
           COUNT(DISTINCT Modification) AS ModificationCount,
           Sequence,
           PeptideGroupId,
           SampleFileId,
           SiteLocation
    FROM InitialGrouping
    GROUP BY SampleFileId,
             Sequence,
             PeptideGroupId,
             SiteLocation
)


SELECT
    -- We have a special rule for this C-term modification - we're actually interested in the _unmodified_ percentage
       (CASE WHEN p1.Modification.Name = 'Lys-loss (Protein C-term K)' THEN 'C-term K' ELSE p1.Modification.Name END) AS Modification,

    -- Normally we want to show the max percentage modified across all replicates (5%, 10%, 20% -> 20% as the worst case)
    -- However, for this one special modification, we invert the percentage. So 95% -> 5%, 90% -> 10%, 80% -> 20%. Thus,
    -- we need to find the one with the lowest value, which becomes the highest percentage when we subtract it from 100%.
       (CASE WHEN p1.Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - TotalPercentModified) ELSE TotalPercentModified END) AS TotalPercentModified,
       (CASE WHEN p1.Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - PercentModified) ELSE PercentModified END) AS PercentModified,
       (CASE WHEN p1.Modification.Name = 'Lys-loss (Protein C-term K)' THEN (1 - p3.MinPercentModified) ELSE p3.MaxPercentModified END) AS MaxPercentModified,
       p2.ModificationCount,

       p1.Id AS Id @hidden,
       PeptideModifiedSequence,
       p1.Sequence @hidden,
       PreviousAA @hidden,
       NextAA @hidden,
       p1.SampleFileId.SampleName,
       p1.AminoAcid,
       p1.SiteLocation,
       p1.Location,
       p1.PeptideGroupId,

       p1.PeptideGroupId.RunId AS RunId,

       CAST(NULL AS VARCHAR) IsCdr,
       CAST(NULL AS VARCHAR) Risk

FROM
    InitialGrouping p1

        INNER JOIN

    -- Second, calculate total percent across all modifications for each amino acid
   Summarized p2

    ON p1.SampleFileId = p2.SampleFileId AND
       p1.Sequence = p2.Sequence AND
       p1.PeptideGroupId = p2.PeptideGroupId AND
       p1.SiteLocation = p2.SiteLocation

    INNER JOIN

        -- Third, calculate max percent modified across all samples
            (SELECT MAX(TotalPercentModified) AS MaxPercentModified,
                    MIN(TotalPercentModified) AS MinPercentModified,
                    Sequence,
                    PeptideGroupId,
                    SiteLocation
             FROM Summarized x
             GROUP BY Sequence, PeptideGroupId, SiteLocation
             ) p3

    ON p1.Sequence = p3.Sequence AND
       p1.PeptideGroupId = p3.PeptideGroupId AND
       p1.SiteLocation = p3.SiteLocation
