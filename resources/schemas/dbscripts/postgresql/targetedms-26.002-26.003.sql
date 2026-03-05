ALTER TABLE targetedms.QCAnnotationType ADD COLUMN isShareable BOOLEAN DEFAULT FALSE;
ALTER TABLE targetedms.QCAnnotation ADD COLUMN instrumentModel VARCHAR(255);
ALTER TABLE targetedms.QCAnnotation ADD COLUMN instrumentSerialNumber VARCHAR(255);