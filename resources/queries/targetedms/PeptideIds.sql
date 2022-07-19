SELECT
   PrecursorId.PeptideId.PeptideGroupId @hidden,
   ROUND(AVG(BestRetentionTime), 1) AS RetentionTime,
   MIN((PrecursorId.mz - 1.00727647) * PrecursorId.Charge * (1 + (pci.AverageMassErrorPPM / 1000000 ))) AS MinObservedPeptideMass,
   MAX((PrecursorId.mz - 1.00727647) * PrecursorId.Charge * (1 + (pci.AverageMassErrorPPM / 1000000 ))) AS MaxObservedPeptideMass,
   PrecursorId.NeutralMass AS ExpectedPeptideMass,
   -- Concatenate the empty string so that the protein DisplayColumn doesn't get propagated too
   PrecursorId.PeptideId.PeptideGroupId.Label || '' AS Chain,
   CAST(PrecursorId.PeptideId.StartIndex + 1 AS VARCHAR) || '-' || CAST(PrecursorId.PeptideId.EndIndex AS VARCHAR) AS PeptideLocation,
   -- Value is calculated in Java in CrossLinkedPeptideDisplayColumn
   CAST(NULL AS VARCHAR) AS BondLocation,
   PrecursorId.PeptideId.Sequence @hidden,
   PrecursorId.PeptideId.NextAA @hidden,
   PrecursorId.PeptideId.PreviousAA @hidden,
   PrecursorId.PeptideId.PeptideModifiedSequence AS PeptideModifiedSequence,
   PrecursorId.PeptideId AS Id @hidden,
   PrecursorId.PeptideId.PeptideGroupId.RunId AS RunId @hidden,
   -- Show the modifications and their locations
   (SELECT GROUP_CONCAT((StructuralModId.Name ||
                         -- For now omit AA index info for anything but the first crosslinked peptide
                         CASE WHEN psm.PeptideIndex = 0 THEN (' @ ' ||
                         SUBSTRING(p.Sequence, IndexAA + 1, 1) ||
                         CAST(IndexAA + p.StartIndex + 1 AS VARCHAR)) ELSE '' END),
       (', ' || CHR(10)))
   FROM targetedms.PeptideStructuralModification psm INNER JOIN targetedms.Peptide p ON psm.PeptideId = p.Id WHERE psm.PeptideId = pci.PrecursorId.PeptideId) AS Modification,
   SUM(TotalArea) AS TotalArea

FROM
     targetedms.precursorchrominfo pci
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
   PrecursorId.PeptideId.EndIndex
