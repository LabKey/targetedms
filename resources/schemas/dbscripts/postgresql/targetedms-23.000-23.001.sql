DELETE FROM targetedms.ReplicateAnnotation WHERE source = 'User';
ALTER TABLE targetedms.ReplicateAnnotation DROP COLUMN source;
