
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
       p1.AminoAcid,
       p1.Location,
       p1.PeptideGroupId

FROM
    -- First, calculate the percentage for each individual modification
    (SELECT StructuralModId                                                                  AS Modification,
            SUM(ModifiedAreaProportion)                                                      AS PercentModified,
            MIN(Id)                                                                          AS Id,
            MAX(PeptideModifiedSequence) AS PeptideModifiedSequence,
            Sequence @hidden,
            PreviousAA @hidden,
            NextAA @hidden,
            SampleName,
            StartIndex,
            IndexAA,
            SUBSTRING(Sequence, IndexAA + 1, 1) AS AminoAcid,
            StartIndex + IndexAA + 1 AS Location,
            PeptideGroupId
     FROM PTMPercentsPrepivot
     GROUP BY SampleName,
              Sequence,
              PreviousAA,
              NextAA,
              StartIndex,
              PeptideGroupId,
              IndexAA,
              StructuralModId) p1

        INNER JOIN

    -- Second, calculate total percent across all modifications for each amino acid
    (SELECT
         SUM(ModifiedAreaProportion) AS TotalPercentModified,
         COUNT(DISTINCT StructuralModId) AS ModificationCount,
         SampleName,
         Sequence,
         PeptideGroupId,
         SUBSTRING(Sequence, IndexAA + 1, 1) AS AminoAcid,
         StartIndex + IndexAA + 1 AS Location,
     FROM PTMPercentsPrepivot
     GROUP BY SampleName,
              Sequence,
              PeptideGroupId,
              IndexAA,
              StartIndex) p2

    ON p1.SampleName = p2.SampleName AND
       p1.Sequence = p2.Sequence AND
       p1.PeptideGroupId = p2.PeptideGroupId AND
       p1.AminoAcid = p2.AminoAcid AND
       p1.Location = p2.Location

    INNER JOIN

        -- Third, calculate max percent modified across all samples
            (SELECT MAX(TotalPercentModified) AS MaxPercentModified,
                    MIN(TotalPercentModified) AS MinPercentModified,
                    Sequence,
                    PeptideGroupId,
                    AminoAcid,
                    Location
             FROM (SELECT SUM(ModifiedAreaProportion)                                                      AS TotalPercentModified,
                          SampleName                                                                       AS SampleName2,
                          Sequence,
                          PeptideGroupId,
                          SUBSTRING(Sequence, IndexAA + 1, 1) AS AminoAcid,
                          StartIndex + IndexAA + 1 AS Location,
                   FROM PTMPercentsPrepivot
                   GROUP BY SampleName,
                            Sequence,
                            PeptideGroupId,
                            IndexAA,
                            StartIndex) x
             GROUP BY Sequence, PeptideGroupId, AminoAcid, Location
             ) p3

    ON p1.Sequence = p3.Sequence AND
       p1.PeptideGroupId = p3.PeptideGroupId AND
       p1.AminoAcid = p3.AminoAcid AND
       p1.Location = p3.Location
