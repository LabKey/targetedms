UPDATE targetedms.QCAnnotation SET Description = 'None' WHERE Description IS NULL;

ALTER TABLE targetedms.QCAnnotation ALTER COLUMN Description VARCHAR(MAX) NOT NULL;