-- Adding missing foreign keys to core.Containers(EntityId)
SELECT core.executeJavaUpgradeCode('reparentOrphanedTargetedMSData');

-- Adding missing indices on Container
CREATE INDEX IX_QCAnnotationType_Container ON targetedms.QCAnnotationType(Container);
CREATE INDEX IX_QCAnnotation_Container ON targetedms.QCAnnotation(Container);
CREATE INDEX IX_GuideSet_Container ON targetedms.GuideSet(Container);
CREATE INDEX IX_QCMetricConfiguration_Container ON targetedms.QCMetricConfiguration(Container);