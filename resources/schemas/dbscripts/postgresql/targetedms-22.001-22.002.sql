CREATE TABLE targetedms.ExcludedPrecursors
(
    rowId SERIAL NOT NULL,
    ModifiedSequence VARCHAR(300),
    Mz DOUBLE PRECISION,
    Charge INT,
    CustomIonName VARCHAR(100),
    IonFormula VARCHAR(100),
    MassMonoisotopic DOUBLE PRECISION,
    MassAverage DOUBLE PRECISION,
    Container ENTITYID NOT NULL,

    CONSTRAINT PK_EXCLUDEDPRECURSORS_ROWID PRIMARY KEY (rowId),
    CONSTRAINT FK_EXCLUDEDPRECURSORS_CONTAINER FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)

);

CREATE INDEX IX_EXCLUDEDPRECURSORS_CONTAINER ON targetedms.ExcludedPrecursors (Container);