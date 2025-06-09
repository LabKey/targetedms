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

