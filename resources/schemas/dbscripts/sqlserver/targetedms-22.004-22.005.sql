-- Reparent table if it exists in PanoramaPremium schema
IF OBJECT_ID(N'PanoramaPremium.QCEmailNotifications', N'U') IS NOT NULL BEGIN
   DROP INDEX IX_PanoramaPremium_qcEmailNotifications_Container ON PanoramaPremium.QCEmailNotifications;
   ALTER SCHEMA targetedms TRANSFER PanoramaPremium.QCEmailNotifications;
   DROP SCHEMA PanoramaPremium;
END;
GO

-- Create it if we didn't have one to repurpose
IF OBJECT_ID(N'targetedms.QCEmailNotifications', N'U') IS NULL BEGIN
    CREATE TABLE targetedms.QCEmailNotifications
    (
        userId          USERID,
        enabled         BIT,
        outliers        INTEGER,
        samples         INTEGER,

        Created         DATETIME,
        CreatedBy       USERID,
        Modified        DATETIME,
        ModifiedBy      USERID,
        Container       ENTITYID NOT NULL,

        CONSTRAINT PK_QCEmailNotifications PRIMARY KEY (userId, Container),
        CONSTRAINT FK_QCEmailNotifications FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
    );
END;

-- Create an index either way
CREATE INDEX IX_qcEmailNotifications_Container ON targetedms.QCEmailNotifications (Container);