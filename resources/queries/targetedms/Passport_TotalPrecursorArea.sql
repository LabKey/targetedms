SELECT PrecursorId.Id,
  SampleFileId.ReplicateId.Name AS Replicate,
  SampleFileId.AcquiredTime AS AcquiredTime,
  COALESCE(ifdefined(SampleFileId.ReplicateId.Day),
      ifdefined(SampleFileId.ReplicateId.SampleGroup),
      YEAR(SampleFileId.AcquiredTime) || '-' || MONTH(SampleFileId.AcquiredTime) || '-' || DAYOFMONTH(SampleFileId.AcquiredTime))
      AS Timepoint,
  COALESCE(ifdefined(SampleFileId.ReplicateId.SampleGroup2)) AS Grouping,
  PrecursorId.PeptideId.PeptideGroupId.Label AS ProteinName,
  PrecursorId.PeptideId.PeptideGroupId.SequenceId.SeqId AS seq,
  PrecursorId.PeptideId.Sequence AS PeptideSequence,
  PrecursorId.PeptideId.Id AS PeptideId,
  PrecursorId.Id AS PrecursorId,
  Id AS PanoramaPrecursorId,
  PrecursorId.PeptideId.StandardType,
  PrecursorId.PeptideId.StartIndex,
  PrecursorId.PeptideId.EndIndex,
  TotalArea,
  SampleFileId,
  PrecursorId.PeptideId.PeptideGroupId.id as PepGroupId,
  (SELECT SUM(pci.TotalArea) AS SumArea
   FROM precursorchrominfo AS pci
   WHERE pci.PrecursorId.PeptideId.StandardType='Normalization'
         AND pci.SampleFileId = precursorchrominfo.SampleFileId) AS SumArea
FROM precursorchrominfo
