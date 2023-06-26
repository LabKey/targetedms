ALTER TABLE targetedms.QCAnnotation ADD EndDate DATETIME;

-- Poke a new row annotation type into the /Shared project
EXEC core.executeJavaInitializationCode 'addInstrumentDowntimeAnnotationType';
