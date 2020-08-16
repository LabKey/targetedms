ALTER TABLE targetedms.AnnotationSettings DROP CONSTRAINT PK_AnnotationSettings;
GO

ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.AnnotationSettings ADD CONSTRAINT PK_AnnotationSettings PRIMARY KEY (Id);
GO