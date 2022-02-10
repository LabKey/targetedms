CREATE TABLE targetedms.QCGroup
(
    rowId INT IDENTITY(1, 1) NOT NULL,
    label VARCHAR(100),

    CONSTRAINT PK_QCGROUP_ROWID PRIMARY KEY (rowId)
);
GO

INSERT INTO targetedms.QCGroup (label) VALUES ('Included');
GO
INSERT INTO targetedms.QCGroup (label) VALUES ('Excluded');
GO

CREATE FUNCTION targetedms.getQCGroupRowId() RETURNS INT AS
BEGIN
        DECLARE @retVal INT;
SELECT @retVal = rowId FROM targetedms.QCGroup WHERE label = 'Included'
    RETURN @retVal;
END;
GO

ALTER TABLE targetedms.Peptide ADD QCGroupId INT NOT NULL DEFAULT targetedms.getQCGroupRowId();
GO

ALTER TABLE targetedms.Molecule ADD QCGroupId INT NOT NULL DEFAULT targetedms.getQCGroupRowId();
GO