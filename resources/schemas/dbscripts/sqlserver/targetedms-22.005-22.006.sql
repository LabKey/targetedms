ALTER TABLE targetedms.StructuralModification ADD CrossLinker BIT;
GO
UPDATE targetedms.StructuralModification SET CrossLinker = 0;
ALTER TABLE targetedms.StructuralModification ALTER COLUMN CrossLinker BIT NOT NULL;

ALTER TABLE targetedms.PeptideStructuralModification ADD PeptideIndex SMALLINT;
GO
UPDATE targetedms.PeptideStructuralModification SET PeptideIndex = 0;
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN PeptideIndex SMALLINT NOT NULL;
