SELECT 1 AS E WHERE EXISTS (SELECT Id FROM targetedms.TransitionChromInfo WHERE MassErrorPPM IS NOT NULL AND TransitionId.Charge IS NULL)