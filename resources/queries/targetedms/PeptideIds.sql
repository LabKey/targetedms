SELECT
   PeptideGroupId @hidden,
   ROUND(AVG(BestRetentionTime), 1) AS RetentionTime,
   MIN((mz - 1.00727647) * Charge * (1 + (AverageMassErrorPPM / 1000000 ))) AS MinObservedPeptideMass,
   MAX((mz - 1.00727647) * Charge * (1 + (AverageMassErrorPPM / 1000000 ))) AS MaxObservedPeptideMass,
   NeutralMass AS ExpectedPeptideMass,
   Chain,
   (StartIndex + 1) || '-' || (EndIndex) AS PeptideLocation,
   Sequence,
   NextAA @hidden,
   PreviousAA @hidden,
   ModifiedSequence AS PeptideModifiedSequence @hidden,
   PeptideId AS Id @hidden,
   -- Show the modifications and their locations
   GROUP_CONCAT(
       (StructuralModName || ' @ ' || SUBSTRING(Sequence, ModIndexAA + 1, 1) || CAST(ModIndexAA + StartIndex AS VARCHAR)),
       (', ' || CHR(10)))
   AS Modification
FROM (
    SELECT
        pci.PrecursorId.PeptideId.PeptideGroupId,
        pci.BestRetentionTime,
        pci.PrecursorId.mz,
        pci.PrecursorId.Charge,
        pci.AverageMassErrorPPM,
        pci.PrecursorId.NeutralMass,
        -- Concatenate the empty string so that the protein DisplayColumn doesn't get propagated too
        pci.PrecursorId.PeptideId || '' AS Chain,
        pci.PrecursorId.PeptideId,
        pci.PrecursorId.PeptideId.StartIndex,
        pci.PrecursorId.PeptideId.EndIndex,
        pci.PrecursorId.PeptideId.Sequence,
        pci.PrecursorId.PeptideId.NextAA,
        pci.PrecursorId.PeptideId.PreviousAA,
        pci.PrecursorId.ModifiedSequence,
        psm.StructuralModId.Name AS StructuralModName,
        psm.IndexAA AS ModIndexAA
    FROM
         targetedms.precursorchrominfo pci
     LEFT OUTER JOIN
         targetedms.PeptideStructuralModification psm ON psm.PeptideId = pci.PrecursorId.PeptideId
    ) x
GROUP BY
   ModifiedSequence,
   NeutralMass,
   PeptideId,
   PeptideGroupId,
   Chain,
   Sequence,
   NextAA,
   PreviousAA,
   StartIndex,
   EndIndex
