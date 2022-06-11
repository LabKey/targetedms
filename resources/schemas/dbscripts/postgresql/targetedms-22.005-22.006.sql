ALTER TABLE targetedms.StructuralModification ADD COLUMN CrossLinker BOOLEAN;
UPDATE targetedms.StructuralModification SET CrossLinker = false;
ALTER TABLE targetedms.StructuralModification ALTER COLUMN CrossLinker SET NOT NULL;

ALTER TABLE targetedms.PeptideStructuralModification ADD COLUMN PeptideIndex SMALLINT;
UPDATE targetedms.PeptideStructuralModification SET PeptideIndex = 0;
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN PeptideIndex SET NOT NULL;
