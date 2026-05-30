/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE targetedms.PTMPercentsGroupedPrepivotCache
(
    Id                  BIGINT NOT NULL, -- PeptideId lookup, not a primary key
    Container           ENTITYID NOT NULL,
    RunId               BIGINT NOT NULL,
    Modification        VARCHAR(300) NOT NULL,
    TotalPercentModified REAL,
    PercentModified     REAL,
    MaxPercentModified  REAL,
    ModificationCount   INT,
    PeptideModifiedSequence VARCHAR(300),
    Sequence            VARCHAR(300),
    PreviousAA          VARCHAR(2),
    NextAA              VARCHAR(2),
    SampleFileId        BIGINT NOT NULL,
    ReplicateName       VARCHAR(200),
    AminoAcid           VARCHAR(5),
    SiteLocation        VARCHAR(50),
    Location            INT,
    PeptideGroupId      BIGINT NOT NULL,

    CONSTRAINT FK_PTMPercentsGroupedPrepivotCache_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT FK_PTMPercentsGroupedPrepivotCache_Id FOREIGN KEY (Id) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT FK_PTMPercentsGroupedPrepivotCache_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
    CONSTRAINT FK_PTMPercentsGroupedPrepivotCache_SampleFileId FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id),
    CONSTRAINT FK_PTMPercentsGroupedPrepivotCache_PeptideGroupId FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
);

CREATE INDEX IDX_PTMPercentsGroupedPrepivotCache_Id ON targetedms.PTMPercentsGroupedPrepivotCache(Id);
CREATE INDEX IDX_PTMPercentsGroupedPrepivotCache_RunId ON targetedms.PTMPercentsGroupedPrepivotCache(RunId);
CREATE INDEX IDX_PTMPercentsGroupedPrepivotCache_Container ON targetedms.PTMPercentsGroupedPrepivotCache(Container);
CREATE INDEX IDX_PTMPercentsGroupedPrepivotCache_SampleFileId ON targetedms.PTMPercentsGroupedPrepivotCache(SampleFileId);
CREATE INDEX IDX_PTMPercentsGroupedPrepivotCache_PeptideGroupId ON targetedms.PTMPercentsGroupedPrepivotCache(PeptideGroupId);

SELECT core.executeJavaUpgradeCode('populatePTMPercentsGroupedPrepivotCache');
