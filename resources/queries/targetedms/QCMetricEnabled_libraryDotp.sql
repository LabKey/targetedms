SELECT 1 AS E WHERE EXISTS (SELECT Id FROM targetedms.PrecursorChromInfo WHERE LibraryDotp IS NOT NULL)