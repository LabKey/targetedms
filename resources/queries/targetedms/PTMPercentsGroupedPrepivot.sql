
SELECT p1.Modification,
       p1.PercentModified,
       TotalPercentModified,
       p2.ModificationCount,
       Id AS Id @hidden,
       PeptideModifiedSequence,
       p1.Sequence @hidden,
       PreviousAA @hidden,
       NextAA @hidden,
       SampleName,
       p1.SiteLocation,
       p1.PeptideGroupId

FROM
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
    (SELECT
         SUM(ModifiedAreaProportion) AS TotalPercentModified,
         COUNT(*) AS ModificationCount,
         SampleName AS SampleName2,
         Sequence,
         PeptideGroupId,
         SUBSTRING(Sequence, IndexAA + 1, 1) || CAST(StartIndex + IndexAA + 1 AS VARCHAR) AS SiteLocation,
     FROM PTMPercentsPrepivot
     GROUP BY SampleName,
              Sequence,
              PeptideGroupId,
              IndexAA,
              StartIndex) p2

    ON p1.SampleName = p2.SampleName2 AND
       p1.Sequence = p2.Sequence AND
       p1.PeptideGroupId = p2.PeptideGroupId AND
       p1.SiteLocation = p2.SiteLocation
