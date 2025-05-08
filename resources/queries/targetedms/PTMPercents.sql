
SELECT
  StructuralModId AS Modification,
  SUM(ModifiedAreaProportion) AS PercentModified,
  MIN(Id) AS Id @hidden,
  PeptideModifiedSequence,
  Sequence @hidden,
  PreviousAA @hidden,
  NextAA @hidden,
  SampleFileId.ReplicateId.Name AS ReplicateName,
  SiteLocation,
  SUBSTRING(Sequence, IndexAA + 1, 1) AS AminoAcid,
  StartIndex + IndexAA + 1 AS Location,
  PeptideGroupId
FROM PTMPercentsPrepivot
GROUP BY
  SampleFileId.ReplicateId.Name,
  Sequence,
  PreviousAA,
  NextAA,
  PeptideModifiedSequence,
  StartIndex,
  PeptideGroupId,
  IndexAA,
  StructuralModId,
  SiteLocation
PIVOT PercentModified BY ReplicateName