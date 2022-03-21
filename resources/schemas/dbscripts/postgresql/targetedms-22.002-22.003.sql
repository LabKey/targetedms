
-- Junction table between Run and AuditLogEntry
CREATE TABLE targetedms.RunAuditLogEntry
(
    RowId                   SERIAL NOT NULL,
    VersionId               BIGINT NOT NULL,
    AuditLogEntryId         INT NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_RUNAUDITLOGENTRY PRIMARY KEY (RowId),
    CONSTRAINT FK_RUNAUDITLOGENTRY_RUN FOREIGN KEY (VersionId) REFERENCES targetedms.runs(id),
    CONSTRAINT FK_RUNAUDITLOGENTRY_AUDITLOGENTRY FOREIGN KEY (AuditLogEntryId) REFERENCES targetedms.AuditLogEntry(entryId)

);
CREATE UNIQUE INDEX UQ_TARGETEDMS_RUNAUDITLOGENTRY_RUN_AUDITLOGENTRY ON targetedms.RunAuditLogEntry(VersionId, AuditLogEntryId);

-- Populate from existing AuditLogEntry and Run data
INSERT INTO targetedms.RunAuditLogEntry (AuditLogEntryId, VersionId, Created, CreatedBy, Modified, ModifiedBy)
SELECT ale.EntryId AS AuditLogEntryId, ale.VersionId, r.Created, r.CreatedBy, r.Modified, r.ModifiedBy FROM targetedms.AuditLogEntry ale                                                                                                                                  LEFT JOIN targetedms.Runs r ON ale.VersionId = r.id
WHERE ale.VersionId IS NOT NULL;

-- VersionId Column no longer used on AuditLogEntry
SELECT core.fn_dropifexists('AuditLogEntry', 'targetedms', 'INDEX', 'uix_auditLogEntry_version');
SELECT core.fn_dropifexists('AuditLogEntry', 'targetedms', 'CONSTRAINT', 'fk_auditLogEntry_runs');
ALTER TABLE targetedms.AuditLogEntry
DROP COLUMN VersionId;

