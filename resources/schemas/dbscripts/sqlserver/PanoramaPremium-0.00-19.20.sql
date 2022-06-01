/* panoramapremium-19.10-19.20.sql */

CREATE SCHEMA PanoramaPremium;
GO

CREATE TABLE PanoramaPremium.QCEmailNotifications
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
GO

CREATE INDEX IX_PanoramaPremium_qcEmailNotifications_Container ON PanoramaPremium.QCEmailNotifications (Container);
GO