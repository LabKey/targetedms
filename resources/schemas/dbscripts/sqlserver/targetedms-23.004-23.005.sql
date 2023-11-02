ALTER TABLE targetedms.molecule ALTER COLUMN customionname NVARCHAR(MAX);
ALTER TABLE targetedms.moleculeprecursor ALTER COLUMN customionname NVARCHAR(MAX);
ALTER TABLE targetedms.moleculetransition ALTER COLUMN customionname NVARCHAR(MAX);
ALTER TABLE targetedms.excludedprecursors ALTER COLUMN customionname NVARCHAR(MAX);