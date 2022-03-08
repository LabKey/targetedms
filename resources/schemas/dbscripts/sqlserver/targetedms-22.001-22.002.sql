CREATE TABLE targetedms.ExcludedPrecursors
(
    rowId INT IDENTITY(1, 1) NOT NULL,
    ModifiedSequence NVARCHAR(300),
    Mz FLOAT,
    Charge INT,
    CustomIonName NVARCHAR(100),
    IonFormula NVARCHAR(100),
    MassMonoisotopic FLOAT,
    MassAverage FLOAT,
    Container ENTITYID NOT NULL,

    CONSTRAINT PK_EXCLUDEDPRECURSORS_ROWID PRIMARY KEY (rowId),
    CONSTRAINT FK_EXCLUDEDPRECURSORS_CONTAINER FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
GO

CREATE INDEX IX_EXCLUDEDPRECURSORS_CONTAINER ON targetedms.ExcludedPrecursors (Container);
GO