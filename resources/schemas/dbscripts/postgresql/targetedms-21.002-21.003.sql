ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TraceValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN Trace VARCHAR(300);
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN YAxisLabel VARCHAR(200);

CREATE TABLE targetedms.QCTraceMetricValues
(
    Id              SERIAL NOT NULL ,
    metric          INT,
    value           REAL,
    sampleFile      INT,

    CONSTRAINT PK_QCTraceMetricValues PRIMARY KEY (Id),
    CONSTRAINT FK_QCTraceMetricValues_Metric FOREIGN KEY (metric) REFERENCES targetedms.QCMetricConfiguration(Id),
    CONSTRAINT FK_QCTraceMetricValues_SampleFile FOREIGN KEY (sampleFile) REFERENCES targetedms.SampleFile(Id)
);

CREATE INDEX IX_QCTraceMetricValues_SampleFile ON targetedms.QCTraceMetricValues(sampleFile);
CREATE INDEX IX_QCTraceMetricValues_Metric ON targetedms.QCTraceMetricValues(metric);
