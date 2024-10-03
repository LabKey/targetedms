SELECT ci.ModifiedAreaProportion,
       ci.PeptideId AS Id,
       ci.PeptideId.PeptideModifiedSequence,
       ci.PeptideId.Sequence,
       ci.PeptideId.NextAA,
       ci.PeptideId.PreviousAA,
       ci.PeptideId.StartIndex,
       ci.SampleFileId,
       ci.PeptideId.PeptideGroupId,
       psm.IndexAA,
       psm.StructuralModId,
       -- Explicitly cast for SQLServer to avoid trying to add as numeric types
       (SUBSTRING(ci.PeptideId.Sequence, IndexAA + 1, 1)) || CAST(ci.PeptideId.StartIndex + IndexAA + 1 AS VARCHAR) AS SiteLocation
FROM
     targetedms.GeneralMoleculeChromInfo ci LEFT JOIN
         targetedms.PeptideStructuralModification psm ON ci.PeptideId = psm.PeptideId LEFT JOIN
         targetedms.TargetedMSRuns r ON ci.SampleFileId.ReplicateId.RunId.ExperimentRunLSID = r.LSID
WHERE
    -- Exclude data without modifications of interest
    StructuralModId.Name IS NOT NULL AND StructuralModId.Name != 'Carbamidomethyl (C)'
    -- Exclude data from Skyline files that have an updated version
  AND r.ReplacedByRun IS NULL
