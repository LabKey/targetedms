/* panoramapremium-19.10-19.20.sql */

CREATE SCHEMA PanoramaPremium;

CREATE TABLE PanoramaPremium.QCEmailNotifications
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

CREATE INDEX IX_PanoramaPremium_qcEmailNotifications_Container ON PanoramaPremium.QCEmailNotifications (Container);