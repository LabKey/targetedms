ALTER TABLE targetedms.QCAnnotationType ADD COLUMN isShareable BOOLEAN DEFAULT FALSE;
ALTER TABLE targetedms.QCAnnotation ADD COLUMN instrument VARCHAR(255);