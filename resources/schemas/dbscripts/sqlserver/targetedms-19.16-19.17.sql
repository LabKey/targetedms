-- Increase the length of the Gene column. The gene field can contain all possible gene names that a protein product is associated with. This can get really long.
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene NVARCHAR(2000);
