CREATE TABLE targetedms.InstrumentNickname
(
    Id              BIGINT IDENTITY(1, 1) NOT NULL,

    Container       entityid NOT NULL,
    Created         DATETIME,
    CreatedBy       USERID,
    Modified        DATETIME,
    ModifiedBy      USERID,

    SerialNumber    NVARCHAR(200),
    Model           NVARCHAR(300),
    Nickname        NVARCHAR(200),

    CONSTRAINT PK_InstrumentNickname PRIMARY KEY (Id)
);
CREATE INDEX IDX_InstrumentNickname_Container ON targetedms.InstrumentNickname(Container);

