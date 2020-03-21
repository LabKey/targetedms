SELECT
    CASE WHEN PeptideCount = 0 THEN 0 ELSE (Area / PeptideCount) END AS MeanArea,
    PeptideCount,
    SampleFileId,
    SampleFileId.SampleName,
    SampleFileId.ReplicateId.Name AS ReplicateName,
    PeptideGroupId,
    ifdefined(PeptideGroupId."Standard (ppm)") AS PPM,
       RowId
FROM (
         SELECT SUM(ci.PeptideArea) AS Area,
                COUNT(ci.PeptideId)      AS PeptideCount,
                ci.SampleFileId,
                ci.PeptideId.PeptideGroupId,
                r.RowId
         FROM (SELECT SampleFileId, PrecursorId.PeptideId, SUM(TotalArea) AS PeptideArea FROM targetedms.PrecursorChromInfo GROUP BY SampleFileId, PrecursorId.PeptideId) ci
                  LEFT JOIN
              targetedms.TargetedMSRuns r ON ci.SampleFileId.ReplicateId.RunId.ExperimentRunLSID = r.LSID
                  LEFT OUTER JOIN
              targetedms.GeneralMoleculeAnnotation gma
              ON gma.GeneralMoleculeId = ci.PeptideId AND gma.Name = 'Exclude'
         WHERE r.ReplacedByRun IS NULL
           AND (gma.Value IS NULL OR gma.Value = 'false')
         GROUP BY ci.PeptideId.PeptideGroupId, ci.SampleFileId, r.RowId
     ) x
