DELETE FROM targetedms.ReplicateAnnotation WHERE source = 'User';
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT DF_ReplicateAnnotation_Source;
ALTER TABLE targetedms.ReplicateAnnotation DROP COLUMN source;
