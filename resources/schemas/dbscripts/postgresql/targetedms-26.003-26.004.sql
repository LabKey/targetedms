/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT core.executeJavaUpgradeCode('reparentOrphanedTargetedMSData');

-- Adding missing foreign keys to core.Containers(EntityId)
ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.QCAnnotation ADD CONSTRAINT FK_QCAnnotation_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.GuideSet ADD CONSTRAINT FK_GuideSet_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.AutoQCPing ADD CONSTRAINT FK_AutoQCPing_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.SampleFileChromInfo ADD CONSTRAINT FK_SampleFileChromInfo_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);

-- Adding missing indices on Container
CREATE INDEX IX_QCAnnotationType_Container ON targetedms.QCAnnotationType(Container);
CREATE INDEX IX_QCAnnotation_Container ON targetedms.QCAnnotation(Container);
CREATE INDEX IX_GuideSet_Container ON targetedms.GuideSet(Container);
CREATE INDEX IX_QCMetricConfiguration_Container ON targetedms.QCMetricConfiguration(Container);
