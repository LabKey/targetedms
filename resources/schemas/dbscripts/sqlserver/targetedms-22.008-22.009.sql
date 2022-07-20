DROP TABLE targetedms.Protein;
GO

CREATE TABLE targetedms.Protein
(
    Id BIGINT IDENTITY(1, 1) NOT NULL,
    PeptideGroupId BIGINT NOT NULL,
    Label NVARCHAR(MAX) NOT NULL,
    Description NVARCHAR(MAX),
    SequenceId INTEGER,
    Note NVARCHAR(MAX),
    Name NVARCHAR(MAX),
    Accession NVARCHAR(50),
    PreferredName NVARCHAR(MAX),
    Gene NVARCHAR(2000),
    Species NVARCHAR(255),
    AltDescription NVARCHAR(MAX),

    CONSTRAINT PK_Protein PRIMARY KEY (Id)
);
GO

INSERT INTO targetedms.Protein (PeptideGroupId, Label, Description, SequenceId, Note, Name, Accession,
                                PreferredName, Gene, Species, AltDescription)
SELECT
    pg.Id,
    pg.Label,
    pg.Description,
    pg.SequenceId,
    pg.Note,
    pg.Name,
    pg.Accession,
    pg.PreferredName,
    pg.Gene,
    pg.Species,
    pg.AltDescription
FROM
    targetedms.PeptideGroup pg
WHERE pg.Id IN (
    SELECT gm.PeptideGroupId FROM
        targetedms.GeneralMolecule gm
    INNER JOIN
        targetedms.Peptide p ON gm.Id = p.Id
);

ALTER TABLE targetedms.PeptideGroup
    DROP COLUMN SequenceId, Accession, PreferredName, Gene, Species;

ALTER TABLE targetedms.PeptideGroup
    ADD DecoyMatchProportion FLOAT;

ALTER TABLE targetedms.PeptideGroup
    ALTER COLUMN Name NVARCHAR(MAX);
ALTER TABLE targetedms.PeptideGroup
    ALTER COLUMN Label NVARCHAR(MAX);

ALTER TABLE targetedms.Protein ADD CONSTRAINT FK_Protein_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);

CREATE INDEX IX_Protein_PeptideGroupId ON targetedms.Protein(PeptideGroupId);