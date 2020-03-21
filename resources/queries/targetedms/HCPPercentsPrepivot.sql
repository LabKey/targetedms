SELECT
       s1.MeanArea,
       CASE WHEN s2.MeanArea = 0 THEN NULL ELSE ((s1.MeanArea  / s2.MeanArea) * s2.PPM) END AS EstimatedPPM,
       s1.SampleName AS Abundance,
       s2.SampleName AS StandardSampleName,
       s1.PeptideGroupId,
       s1.RowId
FROM
     HCPPercentsSubtotals s1 INNER JOIN HCPPercentsSubtotals s2 ON
        s1.SampleFileId = s2.SampleFileId AND s1.PPM IS NULL AND s2.PPM IS NOT NULL