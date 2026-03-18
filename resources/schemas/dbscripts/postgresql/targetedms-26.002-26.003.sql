ALTER TABLE targetedms.QCAnnotationType ADD COLUMN Shareable BOOLEAN DEFAULT FALSE;
ALTER TABLE targetedms.QCAnnotation ADD COLUMN instrumentModel VARCHAR(255);
ALTER TABLE targetedms.QCAnnotation ADD COLUMN instrumentSerialNumber VARCHAR(255);

UPDATE targetedms.QCAnnotationType SET Shareable = TRUE WHERE Name = 'Instrumentation Change';