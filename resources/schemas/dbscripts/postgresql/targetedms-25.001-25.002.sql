CREATE TABLE targetedms.rateType
(
    Id              SERIAL NOT NULL ,
    name            varchar(100) NOT NULL,
    description     text DEFAULT NULL,
    setupFee        decimal(5,2) DEFAULT 0.00,

    Container       entityid NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,

    CONSTRAINT PK_rateType PRIMARY KEY (Id)
);
CREATE INDEX IDX_rateType_Container ON targetedms.rateType(container);


CREATE TABLE targetedms.instrumentRate
(
    Id              SERIAL NOT NULL ,
    instrument      integer NOT NULL,
    rateType        integer NOT NULL,
    fee             decimal(7,2) NOT NULL,

    Container       entityid NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,

    CONSTRAINT PK_instrumentRate PRIMARY KEY (Id),
    CONSTRAINT FK_instrumentRate_instrument FOREIGN KEY (instrument) REFERENCES targetedms.msInstrument(id),
    CONSTRAINT FK_instrumentRate_rateType FOREIGN KEY (rateType) REFERENCES targetedms.rateType(Id)
);
CREATE INDEX IDX_instrumentRate_Instrument ON targetedms.instrumentRate(instrument);
CREATE INDEX IDX_instrumentRate_RateType ON targetedms.instrumentRate(rateType);
CREATE INDEX IDX_instrumentRate_Container ON targetedms.instrumentRate(container);

CREATE TABLE targetedms.instrumentUsagePayment
(
    Id              SERIAL NOT NULL ,
    instrument      integer NOT NULL,
    instrumentRate  integer NOT NULL,
    paymentMethod   integer NOT NULL,
    percentPayment  decimal(5,2) NOT NULL,
    project         integer NOT NULL,
    startTime       TIMESTAMP NOT NULL,
    endTime         TIMESTAMP NOT NULL,

    Container       entityid NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,

    CONSTRAINT PK_instrumentUsagePayment PRIMARY KEY (Id),
    CONSTRAINT FK_instrumentUsagePayment_instrument FOREIGN KEY (instrument) REFERENCES targetedms.msInstrument(id),
    CONSTRAINT FK_instrumentUsagePayment_instrumentRate FOREIGN KEY (instrumentRate) REFERENCES targetedms.instrumentRate(Id),
    CONSTRAINT FK_instrumentUsagePayment_project FOREIGN KEY (project) REFERENCES targetedms.msProject(Id)
);
CREATE INDEX IDX_instrumentUsagePayment_Instrument ON targetedms.instrumentUsagePayment(instrument);
CREATE INDEX IDX_instrumentUsagePayment_InstrumentRate ON targetedms.instrumentUsagePayment(instrumentRate);
CREATE INDEX IDX_instrumentUsagePayment_PaymentMethod ON targetedms.instrumentUsagePayment(paymentMethod);
CREATE INDEX IDX_instrumentUsagePayment_Project ON targetedms.instrumentUsagePayment(project);
CREATE INDEX IDX_instrumentUsagePayment_Container ON targetedms.instrumentUsagePayment(container);

ALTER TABLE targetedms.projectResearcher ALTER COLUMN researcher TYPE USERID;



