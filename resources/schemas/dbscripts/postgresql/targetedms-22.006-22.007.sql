-- Accommodate new Skyline protein group features that create very long preferred names
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN PreferredName TYPE VARCHAR(500);