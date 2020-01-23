-- Populate with the real values
UPDATE targetedms.PrecursorChromInfo SET PrecursorModifiedAreaProportion =
                                             (SELECT CASE WHEN X.PrecursorAreaInReplicate = 0 THEN NULL ELSE TotalArea / X.PrecursorAreaInReplicate END
                                              FROM
                                                   (SELECT Area AS PrecursorAreaInReplicate
                                                    FROM targetedms.areas a INNER JOIN
                                                             targetedms.PrecursorGroupings g ON a.grouping = g.grouping
                                                    WHERE g.PrecursorId = targetedms.PrecursorChromInfo.PrecursorId AND
                                                          a.SampleFileId = targetedms.PrecursorChromInfo.SampleFileId) X);

UPDATE targetedms.GeneralMoleculeChromInfo SET ModifiedAreaProportion =
                                                   (SELECT CASE WHEN X.MoleculeAreaInReplicate = 0 THEN NULL ELSE
                                                               (SELECT SUM(TotalArea) FROM targetedms.PrecursorChromInfo pci
                                                                WHERE pci.GeneralMoleculeChromInfoId = targetedms.GeneralMoleculeChromInfo.Id)
                                                                 / X.MoleculeAreaInReplicate END
                                                    FROM (
                                                         SELECT SUM(a.Area) AS MoleculeAreaInReplicate
                                                         FROM targetedms.areas a INNER JOIN
                                                                  targetedms.MoleculeGroupings g ON a.grouping = g.grouping
                                                         WHERE g.GeneralMoleculeId = targetedms.GeneralMoleculeChromInfo.GeneralMoleculeId
                                                           AND a.SampleFileId = targetedms.GeneralMoleculeChromInfo.SampleFileId) X);

-- Get rid of the temp tables
DROP TABLE targetedms.PrecursorGroupings;
DROP TABLE targetedms.MoleculeGroupings;
DROP TABLE targetedms.Areas;