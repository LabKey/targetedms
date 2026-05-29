/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE targetedms.InstrumentNickname
(
    Id              BIGSERIAL NOT NULL,

    Container       entityid NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,

    SerialNumber    VARCHAR(200),
    Model           VARCHAR(300),
    Nickname        VARCHAR(200),

    CONSTRAINT PK_InstrumentNickname PRIMARY KEY (Id)
);
CREATE INDEX IDX_InstrumentNickname_Container ON targetedms.InstrumentNickname(Container);

