ALTER TABLE targetedms.QCAnnotation ADD COLUMN EndDate TIMESTAMP;

-- Poke a new row annotation type into the /Shared project
SELECT core.executeJavaInitializationCode('addInstrumentDowntimeAnnotationType');
