
SELECT
  gm.PeptideId,
  gm.PeptideId.PeptideModifiedSequence as PeptideName,
  gm.MoleculeId,
  gm.MoleculeId.CustomIonName as MoleculeName,
  gm.SampleFileId,
  gm.PeakCountRatio,
  gm.RetentionTime,
  gm.SampleFileId.ReplicateId,
  AVG(gm.CalculatedConcentration) as ReplicateConcentration,
  gm.SampleFileId.ReplicateId.AnalyteConcentration,
  100 * (AVG(gm.CalculatedConcentration) - gm.SampleFileId.ReplicateId.AnalyteConcentration) / gm.SampleFileId.ReplicateId.AnalyteConcentration as Bias,
  gm.SampleFileId.ReplicateId.SampleType,
  gm.SampleFileId.ReplicateId.RunId.Id as RunId,
  CAST(qs.Units AS VARCHAR) as Units
FROM generalmoleculechrominfo gm
JOIN QuantificationSettings qs ON gm.SampleFileId.ReplicateId.RunId.Id = qs.RunId.Id

GROUP BY
  gm.PeptideId,
  gm.PeptideId.PeptideModifiedSequence,
  gm.MoleculeId,
  gm.MoleculeId.CustomIonName,
  gm.SampleFileId,
  gm.PeakCountRatio,
  gm.RetentionTime,
  gm.SampleFileId.ReplicateId,
  gm.SampleFileId.ReplicateId.AnalyteConcentration,
  gm.SampleFileId.ReplicateId.SampleType,
  gm.SampleFileId.ReplicateId.RunId.Id,
  CAST(qs.Units AS VARCHAR)

