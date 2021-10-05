SELECT
SampleName,
ReplicateId.RunId.FileName,
ReplicateId.RunId.Id,
AcquiredTime,
CASE WHEN ReplicateId.RunId.SmallMoleculeCount = 0 THEN ReplicateId.RunId.PeptideGroupCount ELSE 0 END AS PeptideGroupCount,
CASE WHEN ReplicateId.RunId.SmallMoleculeCount > 0 THEN ReplicateId.RunId.PeptideGroupCount ELSE 0 END AS MoleculeListCount,
ReplicateId.RunId.PeptideCount,
ReplicateId.RunId.SmallMoleculeCount,
ReplicateId.RunId.PrecursorCount,
ReplicateId.RunId.TransitionCount,
ReplicateId.RunId.ReplicateCount
FROM SampleFile
WHERE ReplicateId.RunId.Deleted = FALSE