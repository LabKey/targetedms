-- Drop the AuditLog view so targetedms-create.sql (runs after this script) rebuilds it with the fixed recursive CTE.
SELECT core.fn_dropifexists('AuditLog', 'targetedms', 'VIEW', NULL);
