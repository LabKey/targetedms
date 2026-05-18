ALTER TABLE targetedms.Runs ADD COLUMN ProteinCount INT DEFAULT 0;

UPDATE targetedms.Runs r
SET ProteinCount = (
    SELECT COUNT(*)
    FROM targetedms.Protein p
    JOIN targetedms.PeptideGroup pg ON p.PeptideGroupId = pg.Id
    WHERE pg.RunId = r.Id
);

-- Redefine PeptideGroupCount: groups containing at least one peptide
UPDATE targetedms.Runs r
SET PeptideGroupCount = (
    SELECT COUNT(DISTINCT pg.Id)
    FROM targetedms.PeptideGroup pg
    JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id
    JOIN targetedms.Peptide p ON p.Id = gm.Id
    WHERE pg.RunId = r.Id
);

ALTER TABLE targetedms.Runs ADD COLUMN MoleculeGroupCount INT NOT NULL DEFAULT 0;

UPDATE targetedms.Runs r
SET MoleculeGroupCount = (
    SELECT COUNT(DISTINCT pg.Id)
    FROM targetedms.PeptideGroup pg
    JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id
    JOIN targetedms.Molecule m ON m.Id = gm.Id
    WHERE pg.RunId = r.Id
);
