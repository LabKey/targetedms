CREATE VIEW targetedms.AuditLog AS
(

WITH RECURSIVE logTree as (
    SELECT e.entryId
         , e.entryHash
         , e.parentEntryHash
    FROM targetedms.AuditLogEntry e
    WHERE e.parentEntryHash = '(null)'

    UNION

    SELECT nxt.entryId
         , nxt.entryHash
         , nxt.parentEntryHash
    FROM targetedms.AuditLogEntry nxt
             JOIN logTree prev ON prev.parentEntryHash = nxt.entryHash
)
SELECT t.entryId
     , e.documentguid
     , e.entryHash
     , ra.VersionId AS RunId
     , e.createtimestamp
     , e.timezoneoffset
     , e.username
     , e.formatversion
     , e.parentEntryHash
     , e.reason
     , e.extrainfo

FROM logTree t
         INNER JOIN targetedms.AuditLogEntry e ON (t.EntryId = e.EntryId)
         LEFT JOIN targetedms.RunAuditLogEntry ra ON t.entryId = ra.AuditLogEntryId
    );