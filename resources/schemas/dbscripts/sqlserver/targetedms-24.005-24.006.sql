CREATE TABLE targetedms.msProject
(
    Id                  INT IDENTITY(1, 1) NOT NULL ,
    affiliation         NVARCHAR(15) DEFAULT NULL,
    blocked             BIT NOT NULL DEFAULT 0,
    title               NVARCHAR(255),
    type                INT,
    submitDate          DATETIME NOT NULL,
    collaborationStatus INT,
    collaborationWith   NVARCHAR(255) NOT NULL,
    organization        INT,
    labDirector         INT,
    scientificQuestion  NVARCHAR(255) NOT NULL,
    abstract            TEXT NOT NULL,
    results             TEXT NOT NULL,

    Container           entityid NOT NULL,
    Created             DATETIME,
    CreatedBy           USERID,
    Modified            DATETIME,
    ModifiedBy          USERID,

    CONSTRAINT PK_msProject PRIMARY KEY (Id)
);
CREATE INDEX IDX_msProject_Container ON targetedms.msProject(container);

CREATE TABLE targetedms.projectResearcher
(
    Id                  INT IDENTITY(1, 1) NOT NULL ,
    project             INT NOT NULL,
    researcher          INT NOT NULL,

    Container           entityid NOT NULL,
    Created             DATETIME,
    CreatedBy           USERID,
    Modified            DATETIME,
    ModifiedBy          USERID,

    CONSTRAINT PK_projectResearcher PRIMARY KEY (Id),
    CONSTRAINT FK_projectResearcher_project FOREIGN KEY (project) REFERENCES targetedms.msProject(Id)
);
CREATE INDEX IDX_projectResearcher_Project ON targetedms.projectResearcher(project);
CREATE INDEX IDX_projectResearcher_Container ON targetedms.projectResearcher(container);

CREATE TABLE targetedms.msInstrument
(
    id          INT IDENTITY(1, 1) NOT NULL ,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(255) DEFAULT NULL,
    active      BIT NOT NULL DEFAULT 1,
    color       NVARCHAR(10) DEFAULT NULL,
    massSpec    BIT DEFAULT 1,
    instrument  NVARCHAR(200),

    Container   entityid NOT NULL,
    Created     DATETIME,
    CreatedBy   USERID,
    Modified    DATETIME,
    ModifiedBy  USERID,

    CONSTRAINT PK_msInstrument PRIMARY KEY (id)
);
CREATE INDEX IDX_msInstrument_Container ON targetedms.msInstrument(container);

CREATE TABLE targetedms.paymentMethod
(
    Id                          INT IDENTITY(1, 1) NOT NULL ,
    UWBudgetNumber              NVARCHAR(50) DEFAULT NULL,
    budgetExpirationDate        DATETIME DEFAULT NULL,
    PONumber                    NVARCHAR(50) DEFAULT NULL,
    contactNameFirst            NVARCHAR(50) DEFAULT NULL,
    contactNameLast             NVARCHAR(50) DEFAULT NULL,
    contactEmail                NVARCHAR(50) DEFAULT NULL,
    contactPhone                NVARCHAR(20) DEFAULT NULL,
    organization                INT DEFAULT NULL,
    addressLine1                NVARCHAR(50) DEFAULT NULL,
    addressLine2                NVARCHAR(50) DEFAULT NULL,
    city                        NVARCHAR(50) DEFAULT NULL,
    state                       char(2) DEFAULT NULL,
    zip                         NVARCHAR(11) DEFAULT NULL,
    country                     NVARCHAR(50) DEFAULT NULL,
    isCurrent                   BIT NOT NULL DEFAULT 0,
    federalFunding              BIT NOT NULL DEFAULT 0,
    poAmount                    decimal(11,2) DEFAULT NULL,
    name                        NVARCHAR(500) DEFAULT NULL,
    worktag                     NVARCHAR(10) DEFAULT NULL,
    resourceWorktag             NVARCHAR(10) DEFAULT NULL,
    resourceWorktagDescription  text DEFAULT NULL,
    assigneeWorktag             NVARCHAR(10) DEFAULT NULL,
    assigneeWorktagDescription  text DEFAULT NULL,
    activityWorktag             NVARCHAR(10) DEFAULT NULL,
    activityWorktagDescription  text DEFAULT NULL,

    Container                   entityid NOT NULL,
    Created                     DATETIME,
    CreatedBy                   USERID,
    Modified                    DATETIME,
    ModifiedBy                  USERID,

    CONSTRAINT PK_paymentMethod PRIMARY KEY (Id)
);
CREATE INDEX IDX_paymentMethod_Container ON targetedms.paymentMethod(container);

CREATE TABLE targetedms.projectPaymentMethod
(
    Id              INT IDENTITY(1, 1) NOT NULL ,
    paymentMethod   INT NOT NULL,
    project         INT NOT NULL,

    Container       entityid NOT NULL,
    Created         DATETIME,
    CreatedBy       USERID,
    Modified        DATETIME,
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
    Id              INT IDENTITY(1, 1) NOT NULL ,
    instrument      INT NOT NULL,
    project         INT NOT NULL,
    startTime       timestamp NOT NULL,
    endTime         timestamp NOT NULL,
    notes           TEXT DEFAULT NULL,
    name            NVARCHAR(255) DEFAULT NULL,

    Container       entityid NOT NULL,
    Created         DATETIME,
    CreatedBy       USERID,
    Modified        DATETIME,
    ModifiedBy      USERID,

    CONSTRAINT PK_instrumentSchedule PRIMARY KEY (Id),
    CONSTRAINT FK_instrumentSchedule_instrument FOREIGN KEY (instrument) REFERENCES targetedms.msInstrument(id),
    CONSTRAINT FK_instrumentSchedule_project FOREIGN KEY (project) REFERENCES targetedms.msProject(Id)
);
CREATE INDEX IDX_instrumentSchedule_Instrument ON targetedms.instrumentSchedule(instrument);
CREATE INDEX IDX_instrumentSchedule_Project ON targetedms.instrumentSchedule(project);
CREATE INDEX IDX_instrumentSchedule_Container ON targetedms.instrumentSchedule(container);