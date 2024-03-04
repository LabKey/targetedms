DROP TABLE targetedms.Protein;

CREATE TABLE targetedms.Protein
(
    Id BIGSERIAL NOT NULL,
    PeptideGroupId BIGINT NOT NULL,
    Label VARCHAR(512) NOT NULL,
    Description TEXT,
    SequenceId INTEGER,
    Note TEXT,
    Name VARCHAR(512),
    Accession VARCHAR(200),
    PreferredName TEXT,
    Gene VARCHAR(2000),
    Species VARCHAR(255),
    AltDescription TEXT,

    CONSTRAINT PK_Protein PRIMARY KEY (Id)
);

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

DROP INDEX targetedms.IX_PeptideGroup_Label;

ALTER TABLE targetedms.PeptideGroup
    DROP COLUMN SequenceId,
    DROP COLUMN Accession,
    DROP COLUMN PreferredName,
    DROP COLUMN Gene,
    DROP COLUMN Species,

    ADD COLUMN DecoyMatchProportion FLOAT,

    ALTER COLUMN Name TYPE TEXT,
    ALTER COLUMN Label TYPE TEXT;

CREATE INDEX IX_Protein_Label ON targetedms.Protein(Label);
CREATE INDEX IX_Protein_PeptideGroupId ON targetedms.Protein(PeptideGroupId);
CREATE INDEX IX_Protein_SequenceId ON targetedms.Protein(SequenceId);
ALTER TABLE targetedms.Protein ADD CONSTRAINT FK_Protein_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
ALTER TABLE targetedms.Protein ADD CONSTRAINT FK_Protein_Sequences FOREIGN KEY(SequenceId) REFERENCES prot.Sequences (seqid);