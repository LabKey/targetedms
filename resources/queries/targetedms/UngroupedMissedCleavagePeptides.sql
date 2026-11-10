/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Missed-cleavage peptides whose Attribute Group ID doesn't match any shorter peptide they contain, so PTM reports split the site across rows
SELECT
  l.RunId,
  l.ProteinId,
  l.Protein,
  l.Sequence AS MissedCleavageSequence,
  l.StartIndex,
  l.EndIndex,
  l.NumMissedCleavages,
  l.GroupKey AS CurrentGroup,
  GROUP_CONCAT(s.Sequence, ', ') AS ContainedPeptides
FROM (
  SELECT DISTINCT p.PeptideGroupId.RunId AS RunId, p.PeptideGroupId AS ProteinId, p.PeptideGroupId.Label AS Protein,
    p.Sequence, p.StartIndex, p.EndIndex, p.NumMissedCleavages,
    -- Skyline groups by unmodified sequence unless Attribute Group ID is set
    COALESCE(NULLIF(p.AttributeGroupId, ''), p.Sequence) AS GroupKey
  FROM Peptide p
  WHERE p.Decoy IS NOT TRUE
    AND p.NumMissedCleavages > 0
    AND p.PeptideGroupId.RunId.ReplacedByRun IS NULL
) l
INNER JOIN (
  SELECT DISTINCT p.PeptideGroupId AS ProteinId, p.Sequence, p.StartIndex, p.EndIndex, p.NumMissedCleavages,
    COALESCE(NULLIF(p.AttributeGroupId, ''), p.Sequence) AS GroupKey
  FROM Peptide p
  WHERE p.Decoy IS NOT TRUE
) s
  ON s.ProteinId = l.ProteinId
 AND s.Sequence <> l.Sequence
 AND s.StartIndex >= l.StartIndex
 AND s.EndIndex <= l.EndIndex
 AND s.NumMissedCleavages < l.NumMissedCleavages
GROUP BY l.RunId, l.ProteinId, l.Protein, l.Sequence, l.StartIndex, l.EndIndex, l.NumMissedCleavages, l.GroupKey
-- A missed-cleavage peptide spans several cleaved peptides but can only join one group
HAVING SUM(CASE WHEN s.GroupKey = l.GroupKey THEN 1 ELSE 0 END) = 0
