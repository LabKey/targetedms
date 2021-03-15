ALTER TABLE targetedms.QCMetricConfiguration ADD TraceValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD TimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD Trace BIGINT;
ALTER TABLE targetedms.QCMetricConfiguration ADD YAxisLabel VARCHAR(200);

ALTER TABLE targetedms.QCMetricConfiguration
    ADD CONSTRAINT FK_QCMetricConfiguration_Trace
        FOREIGN KEY (Trace) REFERENCES targetedms.SampleFileChromInfo(Id)
GO

CREATE TABLE targetedms.QCTraceMetricValues
(
    Id              INT IDENTITY(1, 1) NOT NULL ,
    metric          INT,
    value           REAL,
    sampleFile      BIGINT,

    CONSTRAINT PK_QCTraceMetricValues PRIMARY KEY (Id),
    CONSTRAINT FK_QCTraceMetricValues_Metric FOREIGN KEY (metric) REFERENCES targetedms.QCMetricConfiguration(Id),
    CONSTRAINT FK_QCTraceMetricValues_SampleFile FOREIGN KEY (sampleFile) REFERENCES targetedms.SampleFile(Id)
);

CREATE INDEX IX_QCTraceMetricValues_SampleFile ON targetedms.QCTraceMetricValues(sampleFile);
CREATE INDEX IX_QCTraceMetricValues_Metric ON targetedms.QCTraceMetricValues(metric);
GO