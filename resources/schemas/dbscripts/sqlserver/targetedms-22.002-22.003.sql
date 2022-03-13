
-- Junction table between Run and AuditLogEntry
CREATE TABLE targetedms.RunAuditLogEntry
(
    RowId                   INT IDENTITY(1, 1) NOT NULL,
    VersionId               INT NOT NULL,
    AuditLogEntryId         INT NOT NULL,
    Container               ENTITYID NOT NULL,
    Created                 DATETIME,
    CreatedBy               USERID,
    Modified                DATETIME,
    ModifiedBy              USERID,

    CONSTRAINT PK_RUNAUDITLOGENTRY PRIMARY KEY (RowId),
    CONSTRAINT FK_RUNAUDITLOGENTRY_CONTAINER FOREIGN KEY (Container) REFERENCES core.Containers (EntityId),
    CONSTRAINT FK_RUNAUDITLOGENTRY_RUN FOREIGN KEY (VersionId) REFERENCES targetedms.runs(id),
    CONSTRAINT FK_RUNAUDITLOGENTRY_AUDITLOGENTRY FOREIGN KEY (AuditLogEntryId) REFERENCES targetedms.AuditLogEntry(entryId)
);
GO

CREATE INDEX IX_TARGETEDMS_RUNAUDITLOGENTRY_CONTAINER ON targetedms.RunAuditLogEntry (Container);
CREATE UNIQUE INDEX UQ_TARGETEDMS_RUNAUDITLOGENTRY_RUN_AUDITLOGENTRY ON targetedms.RunAuditLogEntry(VersionId, AuditLogEntryId);
GO

-- Populate from existing AuditLogEntry and Run data
INSERT INTO targetedms.RunAuditLogEntry (AuditLogEntryId, VersionId, Container, Created, CreatedBy, Modified, ModifiedBy)
SELECT ale.EntryId AS AuditLogEntryId, ale.VersionId, r.Container, r.Created, r.CreatedBy, r.Modified, r.ModifiedBy FROM targetedms.AuditLogEntry ale                                                                                                                                  LEFT JOIN targetedms.Runs r ON ale.VersionId = r.id
WHERE ale.VersionId IS NOT NULL
    GO

-- Drop this view
EXEC core.fn_dropifexists 'AuditLog','targetedms','VIEW';
GO

-- VersionId no longer needed on AuditLogEntry
EXEC core.fn_dropifexists 'AuditLogEntry', 'targetedms', 'INDEX', 'uix_auditLogEntry_version';
EXEC core.fn_dropifexists 'AuditLogEntry', 'targetedms', 'CONSTRAINT', 'fk_auditLogEntry_runs';
GO

ALTER TABLE targetedms.AuditLogEntry DROP COLUMN VersionId;
GO

CREATE VIEW targetedms.AuditLog AS

WITH logTree as (
    SELECT entryId
         , entryHash
         , parentEntryHash
    FROM targetedms.AuditLogEntry e
    WHERE e.parentEntryHash = '(null)'

    UNION ALL

    SELECT nxt.entryId
         , nxt.entryHash
         , nxt.parentEntryHash
    FROM targetedms.AuditLogEntry nxt
             JOIN logTree prev
                  ON prev.parentEntryHash = nxt.entryHash
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
WHERE ra.VersionId IS NOT NULL
    );
