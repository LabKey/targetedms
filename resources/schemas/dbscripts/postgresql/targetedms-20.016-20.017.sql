CREATE TABLE targetedms.DataSource
(
    Id serial NOT NULL,
    Container ENTITYID NOT NULL,
    Created TIMESTAMP,
    CreatedBy INT,

    DataId INT NOT NULL,
    Name VARCHAR(200) NOT NULL,
    Size BIGINT NOT NULL,
    InstrumentType VARCHAR(20) NULL,


    CONSTRAINT PK_DataSource PRIMARY KEY (Id),
    CONSTRAINT FK_DataSource_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT FK_DataSource_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
);