-- Drop the AuditLog view so targetedms-create.sql (runs after this script) rebuilds it with the fixed recursive CTE.
DROP VIEW IF EXISTS targetedms.AuditLog;
