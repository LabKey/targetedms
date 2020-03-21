
SELECT
  SUM(EstimatedPPM) AS EstimatedPPM,
  Abundance,
  PeptideGroupId.Description,
  PeptideGroupId.Accession,
  PeptideGroupId.SequenceId.Mass

FROM HCPPercentsPrepivot
GROUP BY
  Abundance,
  PeptideGroupId.Accession,
  PeptideGroupId.Description,
  PeptideGroupId.SequenceId.Mass
PIVOT EstimatedPPM BY Abundance