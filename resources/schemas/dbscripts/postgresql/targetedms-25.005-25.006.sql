/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE targetedms.QCMetricCache
(
    Container       entityid NOT NULL,
    MetricId        INT NOT NULL,
    PrecursorChromInfoId BIGINT,
    SampleFileId    BIGINT NOT NULL,
    MetricValue     REAL,
    SeriesLabel     VARCHAR(200),

    CONSTRAINT PK_QCMetricCache PRIMARY KEY (Container, MetricId, PrecursorChromInfoId),
    CONSTRAINT FK_QCMetricCache_PrecursorChromInfoId FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id),
    CONSTRAINT FK_QCMetricCache_SampleFileId FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id),
    CONSTRAINT FK_QCMetricCache_MetricIdId FOREIGN KEY (MetricId) REFERENCES targetedms.QCMetricConfiguration(Id),
    CONSTRAINT FK_QCMetricCache_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE INDEX IDX_QCMetricCache_PrecursorChromInfoId ON targetedms.QCMetricCache(PrecursorChromInfoId);
CREATE INDEX IDX_QCMetricCache_SampleFileId ON targetedms.QCMetricCache(SampleFileId);
CREATE INDEX IDX_QCMetricCache_MetricId ON targetedms.QCMetricCache(MetricId);

ALTER TABLE targetedms.QCMetricConfiguration DROP COLUMN EnabledQueryName;