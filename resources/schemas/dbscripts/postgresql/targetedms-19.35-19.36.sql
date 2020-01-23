/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- Add the columns we need to populate
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN ModifiedAreaProportion REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN PrecursorModifiedAreaProportion REAL;

-- Create temp tables for perf
CREATE TABLE targetedms.PrecursorGroupings (Grouping VARCHAR(300), PrecursorId INT);
CREATE TABLE targetedms.MoleculeGroupings (Grouping VARCHAR(300), GeneralMoleculeId INT);
CREATE TABLE targetedms.areas (Grouping VARCHAR(300), SampleFileId INT, Area REAL);

-- Populate the temp tables
INSERT INTO targetedms.PrecursorGroupings (Grouping, PrecursorId)
    (SELECT DISTINCT
        COALESCE(gm.AttributeGroupId, p.Sequence, m.CustomIonName, m.IonFormula) AS Grouping,
        pci.PrecursorId
     FROM targetedms.PrecursorChromInfo pci INNER JOIN
              targetedms.GeneralPrecursor gp ON gp.Id = pci.PrecursorId INNER JOIN
              targetedms.GeneralMolecule gm ON gp.GeneralMoleculeId = gm.Id LEFT OUTER JOIN
              targetedms.Molecule m ON gm.Id = m.Id LEFT OUTER JOIN
              targetedms.Peptide p ON p.id = gp.GeneralMoleculeId);

INSERT INTO targetedms.MoleculeGroupings (Grouping, GeneralMoleculeId)
    (SELECT DISTINCT
       g.grouping,
       gp.GeneralMoleculeId
     FROM targetedms.PrecursorGroupings g INNER JOIN
            targetedms.GeneralPrecursor gp ON gp.Id = g.PrecursorId);

INSERT INTO targetedms.areas (Grouping, SampleFileId, Area)
    (SELECT
            g.grouping,
            pci.SampleFileId,
            SUM(pci.TotalArea) AS Area
     FROM targetedms.PrecursorChromInfo pci INNER JOIN
              targetedms.PrecursorGroupings g ON pci.PrecursorId =  g.PrecursorId
     GROUP BY g.grouping, pci.SampleFileId);

-- Create indices to make querying efficient
CREATE INDEX IX_PrecursorGroupings ON targetedms.PrecursorGroupings (PrecursorId, Grouping);
CREATE INDEX IX_MoleculeGroupings ON targetedms.MoleculeGroupings (GeneralMoleculeId, Grouping);
CREATE INDEX IX_areas ON targetedms.areas (Grouping, SampleFileId);

-- Set a value so that we don't have to resize the table's row pages at the same time as the calculations
-- UPDATE targetedms.PrecursorChromInfo SET PrecursorModifiedAreaProportion = 0;
-- UPDATE targetedms.GeneralMoleculeChromInfo SET ModifiedAreaProportion = 0;
