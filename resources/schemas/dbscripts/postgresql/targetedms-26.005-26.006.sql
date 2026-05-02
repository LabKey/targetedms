-- When reparenting the QCEmailNotifications table from PanoramaPremium schema to targetedms, an attempt was made to
-- drop this index, but the attempt failed because it targeted the old schema. This index is redundant with
-- IX_qcEmailNotifications_Container.
DROP INDEX IF EXISTS targetedms.IX_PanoramaPremium_qcEmailNotifications_Container;
