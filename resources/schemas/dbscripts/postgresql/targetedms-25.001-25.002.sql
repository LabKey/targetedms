/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- script from 24.11
CREATE FUNCTION targetedms.ensureColumns() RETURNS void AS $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'qcmetricconfiguration' AND COLUMN_NAME = 'mintimevalue') THEN
    EXECUTE 'ALTER TABLE targetedms.QCMetricConfiguration RENAME COLUMN TimeValue To MinTimeValue';
    EXECUTE 'ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN MaxTimeValue REAL';
    EXECUTE 'ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TimeValueOption VARCHAR(10)';
    EXECUTE 'UPDATE targetedms.QCMetricConfiguration SET MaxTimeValue = 1000 WHERE MinTimeValue IS NOT NULL';
    EXECUTE 'UPDATE targetedms.QCMetricConfiguration SET TimeValueOption = ''First'' WHERE MinTimeValue IS NOT NULL';
END IF;
END
$$ LANGUAGE plpgsql;

SELECT targetedms.ensureColumns();
DROP FUNCTION targetedms.ensureColumns;

-- script from develop
CREATE FUNCTION targetedms.ensureSchedulerTables() RETURNS void AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'targetedms' AND table_name = 'msinstrument') THEN
       EXECUTE '
CREATE TABLE targetedms.msProject
(
    Id                  SERIAL NOT NULL ,
    affiliation         varchar(15) DEFAULT NULL,
    blocked             BOOLEAN NOT NULL DEFAULT ''0'',
    title               varchar(255),
    type                integer,
    submitDate          timestamp NOT NULL,
    collaborationStatus integer,
    collaborationWith   varchar(255) NOT NULL,
    organization        integer,
    labDirector         integer,
    scientificQuestion  varchar(255) NOT NULL,
    abstract            TEXT NOT NULL,
    results             TEXT NOT NULL,

    Container           entityid NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,

    CONSTRAINT PK_msProject PRIMARY KEY (Id)
);
CREATE INDEX IDX_msProject_Container ON targetedms.msProject(container);

CREATE TABLE targetedms.projectResearcher
(
    Id                  SERIAL NOT NULL ,
    project             integer NOT NULL,
    researcher          integer NOT NULL,

    Container           entityid NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,

    CONSTRAINT PK_projectResearcher PRIMARY KEY (Id),
    CONSTRAINT FK_projectResearcher_project FOREIGN KEY (project) REFERENCES targetedms.msProject(Id)
);
CREATE INDEX IDX_projectResearcher_Project ON targetedms.projectResearcher(project);
CREATE INDEX IDX_projectResearcher_Container ON targetedms.projectResearcher(container);

CREATE TABLE targetedms.msInstrument
(
    id          SERIAL NOT NULL ,
    name        varchar(100) NOT NULL,
    description varchar(255) DEFAULT NULL,
    active      BOOLEAN NOT NULL DEFAULT ''1'',
    color       varchar(10) DEFAULT NULL,
    massSpec    BOOLEAN DEFAULT ''1'',
    instrument  varchar(200),

    Container   entityid NOT NULL,
    Created     TIMESTAMP,
    CreatedBy   USERID,
    Modified    TIMESTAMP,
    ModifiedBy  USERID,

    CONSTRAINT PK_msInstrument PRIMARY KEY (id)
);
CREATE INDEX IDX_msInstrument_Container ON targetedms.msInstrument(container);

CREATE TABLE targetedms.paymentMethod
(
    Id                          SERIAL NOT NULL ,
    UWBudgetNumber              varchar(50) DEFAULT NULL,
    budgetExpirationDate        timestamp DEFAULT NULL,
    PONumber                    varchar(50) DEFAULT NULL,
    contactNameFirst            varchar(50) DEFAULT NULL,
    contactNameLast             varchar(50) DEFAULT NULL,
    contactEmail                varchar(50) DEFAULT NULL,
    contactPhone                varchar(20) DEFAULT NULL,
    organization                integer DEFAULT NULL,
    addressLine1                varchar(50) DEFAULT NULL,
    addressLine2                varchar(50) DEFAULT NULL,
    city                        varchar(50) DEFAULT NULL,
    state                       char(2) DEFAULT NULL,
    zip                         varchar(11) DEFAULT NULL,
    country                     varchar(50) DEFAULT NULL,
    isCurrent                   BOOLEAN NOT NULL DEFAULT ''0'',
    federalFunding              BOOLEAN NOT NULL DEFAULT ''0'',
    poAmount                    decimal(11,2) DEFAULT NULL,
    name                        varchar(500) DEFAULT NULL,
    worktag                     varchar(10) DEFAULT NULL,
    resourceWorktag             varchar(10) DEFAULT NULL,
    resourceWorktagDescription  text DEFAULT NULL,
    assigneeWorktag             varchar(10) DEFAULT NULL,
    assigneeWorktagDescription  text DEFAULT NULL,
    activityWorktag             varchar(10) DEFAULT NULL,
    activityWorktagDescription  text DEFAULT NULL,

    Container                   entityid NOT NULL,
    Created                     TIMESTAMP,
    CreatedBy                   USERID,
    Modified                    TIMESTAMP,
    ModifiedBy                  USERID,

    CONSTRAINT PK_paymentMethod PRIMARY KEY (Id)
);
CREATE INDEX IDX_paymentMethod_Container ON targetedms.paymentMethod(container);

CREATE TABLE targetedms.projectPaymentMethod
(
    Id              SERIAL NOT NULL ,
    paymentMethod   integer NOT NULL,
    project         integer NOT NULL,

    Container       entityid NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,

    CONSTRAINT PK_projectPaymentMethod PRIMARY KEY (Id),
    CONSTRAINT FK_projectPaymentMethod_paymentMethod FOREIGN KEY (paymentMethod) REFERENCES targetedms.paymentMethod(Id),
    CONSTRAINT FK_projectPaymentMethod_project FOREIGN KEY (project) REFERENCES targetedms.msProject(Id)
);
CREATE INDEX IDX_projectPaymentMethod_PaymentMethod ON targetedms.projectPaymentMethod(paymentMethod);
CREATE INDEX IDX_projectPaymentMethod_Project ON targetedms.projectPaymentMethod(project);
CREATE INDEX IDX_projectPaymentMethod_Container ON targetedms.projectPaymentMethod(container);

CREATE TABLE targetedms.instrumentSchedule
(
    Id              SERIAL NOT NULL ,
    instrument      integer NOT NULL,
    project         integer NOT NULL,
    startTime       timestamp NOT NULL,
    endTime         timestamp NOT NULL,
    notes           TEXT DEFAULT NULL,
    name            varchar(255) DEFAULT NULL,

    Container       entityid NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,

    CONSTRAINT PK_instrumentSchedule PRIMARY KEY (Id),
    CONSTRAINT FK_instrumentSchedule_instrument FOREIGN KEY (instrument) REFERENCES targetedms.msInstrument(id),
    CONSTRAINT FK_instrumentSchedule_project FOREIGN KEY (project) REFERENCES targetedms.msProject(Id)
);
CREATE INDEX IDX_instrumentSchedule_Instrument ON targetedms.instrumentSchedule(instrument);
CREATE INDEX IDX_instrumentSchedule_Project ON targetedms.instrumentSchedule(project);
CREATE INDEX IDX_instrumentSchedule_Container ON targetedms.instrumentSchedule(container);
';
    END IF;
END
$$ LANGUAGE plpgsql;

SELECT targetedms.ensureSchedulerTables();
DROP FUNCTION targetedms.ensureSchedulerTables;

