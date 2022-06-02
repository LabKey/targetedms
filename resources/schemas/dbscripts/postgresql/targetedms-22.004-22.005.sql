-- Reparent table if it exists in PanoramaPremium schema
ALTER TABLE IF EXISTS PanoramaPremium.QCEmailNotifications SET SCHEMA targetedms;
DROP INDEX IF EXISTS PanoramaPremium.IX_PanoramaPremium_qcEmailNotifications_Container;
DROP SCHEMA IF EXISTS PanoramaPremium;

-- Create if we didn't have one to repurpose
CREATE TABLE IF NOT EXISTS targetedms.QCEmailNotifications
(
    userId          USERID,
    enabled         BOOLEAN,
    outliers        INT,
    samples         INT,

    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,
    Container       ENTITYID NOT NULL,

    CONSTRAINT PK_QCEmailNotifications PRIMARY KEY (userId, Container),
    CONSTRAINT FK_QCEmailNotifications FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

-- Create an index either way
CREATE INDEX IX_qcEmailNotifications_Container ON targetedms.QCEmailNotifications (Container);