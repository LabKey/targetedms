CREATE TABLE targetedms.rateType
(
    Id              INT IDENTITY(1, 1) NOT NULL ,
    name            NVARCHAR(100) NOT NULL,
    description     text DEFAULT NULL,
    setupFee        decimal(5,2) DEFAULT 0.00,

    Container       entityid NOT NULL,
    Created         DATETIME,
    CreatedBy       USERID,
    Modified        DATETIME,
    ModifiedBy      USERID,

    CONSTRAINT PK_rateType PRIMARY KEY (Id)
);
CREATE INDEX IDX_rateType_Container ON targetedms.rateType(container);


CREATE TABLE targetedms.instrumentRate
(
    Id              INT IDENTITY(1, 1) NOT NULL ,
    instrument      INT NOT NULL,
    rateType        INT NOT NULL,
    fee             decimal(7,2) NOT NULL,

    Container       entityid NOT NULL,
    Created         DATETIME,
    CreatedBy       USERID,
    Modified        DATETIME,
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
    Id                          INT IDENTITY(1, 1) NOT NULL ,
    instrumentScheduleId        INT NOT NULL,
    instrumentRate              INT NOT NULL,
    paymentMethod               INT NOT NULL,
    percentPayment              decimal(5,2) NOT NULL,

    Container       entityid NOT NULL,
    Created         DATETIME,
    CreatedBy       USERID,
    Modified        DATETIME,
    ModifiedBy      USERID,

    CONSTRAINT PK_instrumentUsagePayment PRIMARY KEY (Id),
    CONSTRAINT FK_instrumentUsagePayment_instrumentScheduleId FOREIGN KEY (instrumentScheduleId) REFERENCES targetedms.instrumentSchedule(id),
    CONSTRAINT FK_instrumentUsagePayment_instrumentRate FOREIGN KEY (instrumentRate) REFERENCES targetedms.instrumentRate(Id),
);
CREATE INDEX IDX_instrumentUsagePayment_InstrumentScheduleId ON targetedms.instrumentUsagePayment(instrumentScheduleId);
CREATE INDEX IDX_instrumentUsagePayment_InstrumentRate ON targetedms.instrumentUsagePayment(instrumentRate);
CREATE INDEX IDX_instrumentUsagePayment_PaymentMethod ON targetedms.instrumentUsagePayment(paymentMethod);
CREATE INDEX IDX_instrumentUsagePayment_Container ON targetedms.instrumentUsagePayment(container);

ALTER TABLE targetedms.projectResearcher ALTER COLUMN researcher USERID;
ALTER TABLE targetedms.msProject ALTER COLUMN labDirector USERID;

ALTER TABLE targetedms.instrumentSchedule ADD instrumentOperator USERID;
CREATE INDEX IDX_instrumentSchedule_InstrumentOperator ON targetedms.instrumentSchedule(instrumentOperator);
