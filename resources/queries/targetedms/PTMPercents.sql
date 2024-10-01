
SELECT
  StructuralModId AS Modification,
  SUM(ModifiedAreaProportion) AS PercentModified,
  MIN(Id) AS Id @hidden,
  PeptideModifiedSequence,
  Sequence @hidden,
  PreviousAA @hidden,
  NextAA @hidden,
  SampleFileId.SampleName,
  -- Explicitly cast for SQLServer to avoid trying to add as numeric types
  SUBSTRING(Sequence, IndexAA + 1, 1) || CAST(StartIndex + IndexAA + 1 AS VARCHAR) AS SiteLocation,
  SUBSTRING(Sequence, IndexAA + 1, 1) AS AminoAcid,
  StartIndex + IndexAA + 1 AS Location,
  PeptideGroupId
FROM PTMPercentsPrepivot
GROUP BY
  SampleFileId.SampleName,
  Sequence,
  PreviousAA,
  NextAA,
  PeptideModifiedSequence,
  StartIndex,
  PeptideGroupId,
  IndexAA,
  StructuralModId
PIVOT PercentModified BY SampleName