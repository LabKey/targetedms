CREATE TABLE targetedms.QCGroup
(
    rowId SERIAL NOT NULL,
    label VARCHAR(100),

    CONSTRAINT PK_QCGROUP_ROWID PRIMARY KEY (rowId)
);

INSERT INTO targetedms.QCGroup (label) VALUES ('Included');
INSERT INTO targetedms.QCGroup (label) VALUES ('Excluded');

CREATE FUNCTION targetedms.getQCGroupRowId(OUT rowId int) AS
    'SELECT rowId FROM targetedms.QCGroup WHERE label = ''Included'' '
LANGUAGE SQL;

ALTER TABLE targetedms.Peptide ADD COLUMN QCGroupId INT NOT NULL DEFAULT targetedms.getQCGroupRowId();
ALTER TABLE targetedms.Molecule ADD COLUMN QCGroupId INT NOT NULL DEFAULT targetedms.getQCGroupRowId();