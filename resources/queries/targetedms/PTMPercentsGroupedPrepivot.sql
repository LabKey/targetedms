
SELECT p1.Modification,
       p1.PercentModified,
       TotalPercentModified,
       p2.ModificationCount,
       p3.MaxPercentModified,
       p3.MinPercentModified,
       Id AS Id @hidden,
       PeptideModifiedSequence,
       p1.Sequence @hidden,
       PreviousAA @hidden,
       NextAA @hidden,
       p1.SampleName,
       p1.SiteLocation,
       p1.PeptideGroupId

FROM
    -- First, calculate the percentage for each individual modification
    (SELECT StructuralModId                                                                  AS Modification,
            SUM(ModifiedAreaProportion)                                                      AS PercentModified,
            MIN(Id)                                                                          AS Id,
            PeptideModifiedSequence,
            Sequence @hidden,
            PreviousAA @hidden,
            NextAA @hidden,
            SampleName,
            StartIndex,
            IndexAA,
            -- Explicitly cast for SQLServer to avoid trying to add as numeric types
            SUBSTRING(Sequence, IndexAA + 1, 1) || CAST(StartIndex + IndexAA + 1 AS VARCHAR) AS SiteLocation,
            PeptideGroupId
     FROM PTMPercentsPrepivot
     GROUP BY SampleName,
              Sequence,
              PreviousAA,
              NextAA,
              PeptideModifiedSequence,
              StartIndex,
              PeptideGroupId,
              IndexAA,
              StructuralModId) p1

        INNER JOIN

    -- Second, calculate total percent across all modifications for each amino acid
    (SELECT
         SUM(ModifiedAreaProportion) AS TotalPercentModified,
         COUNT(*) AS ModificationCount,
         SampleName,
         Sequence,
         PeptideGroupId,
         SUBSTRING(Sequence, IndexAA + 1, 1) || CAST(StartIndex + IndexAA + 1 AS VARCHAR) AS SiteLocation,
     FROM PTMPercentsPrepivot
     GROUP BY SampleName,
              Sequence,
              PeptideGroupId,
              IndexAA,
              StartIndex) p2

    ON p1.SampleName = p2.SampleName AND
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
             FROM (SELECT SUM(ModifiedAreaProportion)                                                      AS TotalPercentModified,
                          SampleName                                                                       AS SampleName2,
                          Sequence,
                          PeptideGroupId,
                          SUBSTRING(Sequence, IndexAA + 1, 1) ||
                          CAST(StartIndex + IndexAA + 1 AS VARCHAR)                                        AS SiteLocation,
                   FROM PTMPercentsPrepivot
                   GROUP BY SampleName,
                            Sequence,
                            PeptideGroupId,
                            IndexAA,
                            StartIndex) x
             GROUP BY Sequence, PeptideGroupId, SiteLocation
             ) p3

    ON p1.Sequence = p3.Sequence AND
       p1.PeptideGroupId = p3.PeptideGroupId AND
       p1.SiteLocation = p3.SiteLocation
