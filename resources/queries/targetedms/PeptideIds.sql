/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
   PrecursorId.PeptideId.PeptideGroupId @hidden,
   ROUND(AVG(BestRetentionTime), 1) AS RetentionTime,
   MIN((PrecursorId.mz - 1.00727647) * PrecursorId.Charge * (1 + (pci.AverageMassErrorPPM / 1000000 ))) AS MinObservedPeptideMass,
   MAX((PrecursorId.mz - 1.00727647) * PrecursorId.Charge * (1 + (pci.AverageMassErrorPPM / 1000000 ))) AS MaxObservedPeptideMass,
   PrecursorId.NeutralMass AS ExpectedPeptideMass,
   -- Concatenate the empty string so that the protein DisplayColumn doesn't get propagated too
   PrecursorId.PeptideId.PeptideGroupId.Label || '' AS Chain,
   CAST(PrecursorId.PeptideId.StartIndex + 1 AS VARCHAR) || '-' ||
        -- Subtract one from the index if it was a c-term clipping of lysine, shortening the sequence
        CAST(PrecursorId.PeptideId.EndIndex - (CASE WHEN LOCATE('C-Term Lys Clipping', Modification) >= 0 THEN 1 ELSE 0 END) AS VARCHAR) AS PeptideLocation,
   CAST(PrecursorId.PeptideId.StartIndex + 1 AS VARCHAR) || '-' ||
        -- Subtract one from the index if it was a c-term clipping of lysine, shortening the sequence
        CAST(PrecursorId.PeptideId.EndIndex - (CASE WHEN LOCATE('C-Term Lys Clipping', Modification) >= 0 THEN 1 ELSE 0 END) AS VARCHAR) AS PeptideIdentity,
   -- Value is calculated in Java in CrossLinkedPeptideDisplayColumn
   CAST(NULL AS VARCHAR) AS BondLocation,
   PrecursorId.PeptideId.Sequence @hidden,
   PrecursorId.PeptideId.NextAA @hidden,
   PrecursorId.PeptideId.PreviousAA @hidden,
   PrecursorId.PeptideId.PeptideModifiedSequence AS PeptideModifiedSequence,
   PrecursorId.PeptideId AS Id @hidden,
   PrecursorId.PeptideId.PeptideGroupId.RunId AS RunId @hidden,
   mods.Modification,
   SUM(TotalArea) AS TotalArea

FROM
     targetedms.precursorchrominfo pci LEFT OUTER JOIN
         -- Show the modifications and their locations
         (SELECT GROUP_CONCAT((StructuralModId.Name ||
             -- For now omit AA index info for anything but the first crosslinked peptide
                               CASE WHEN psm.PeptideIndex = 0 THEN (' @ ' ||
                                                                    SUBSTRING(p.Sequence, IndexAA + 1, 1) ||
                                                                    CAST(IndexAA + p.StartIndex + 1 AS VARCHAR)) ELSE '' END),
                              (', ' || CHR(10))) AS Modification,
          psm.PeptideId
          FROM targetedms.PeptideStructuralModification psm INNER JOIN targetedms.Peptide p ON psm.PeptideId = p.Id
          GROUP BY psm.PeptideId)
mods ON mods.PeptideId = pci.PrecursorId.PeptideId
GROUP BY
   PrecursorId.PeptideId.PeptideModifiedSequence,
   PrecursorId.NeutralMass,
   PrecursorId.PeptideId,
   PrecursorId.PeptideId.PeptideGroupId,
   PrecursorId.PeptideId.PeptideGroupId.Label,
   PrecursorId.PeptideId.PeptideGroupId.RunId,
   PrecursorId.PeptideId.Sequence,
   PrecursorId.PeptideId.NextAA,
   PrecursorId.PeptideId.PreviousAA,
   PrecursorId.PeptideId.StartIndex,
   PrecursorId.PeptideId.EndIndex,
   Modification
