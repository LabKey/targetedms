SELECT
SampleIdentifier AS SampleName,
ReplicateId.RunId.FileName,
ReplicateId.Name AS ReplicateName,
AcquiredTime,
FilePath,
InstrumentId.Model AS InstrumentModel,
InstrumentSerialNumber AS InstrumentSerialNumber,
ReplicateId.RunId.Container,
ReplicateId.RunId.Id AS RunId,
ReplicateId.RunId.PeptideGroupCount,
ReplicateId.RunId.PeptideCount,
ReplicateId.RunId.SmallMoleculeCount,
ReplicateId.RunId.PrecursorCount,
ReplicateId.RunId.TransitionCount,
Id @Hidden
FROM SampleFile
WHERE ReplicateId.RunId.Deleted = FALSE