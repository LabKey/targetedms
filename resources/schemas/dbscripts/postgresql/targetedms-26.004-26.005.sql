/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Dropping idx_instrumentusagepayment_instrumentscheduleid [instrumentScheduleId] because it overlaps with pk_instrumentusagepayment [instrumentScheduleId, paymentMethod]
DROP INDEX targetedms.idx_instrumentusagepayment_instrumentscheduleid;
-- Dropping ix_qcannotationtype_container [Container] because it overlaps with uq_qcannotationtype_containername [Container, Name]
DROP INDEX targetedms.ix_qcannotationtype_container;
-- Dropping ix_precursor_id [Id] because it overlaps with pk_precursor_id [Id]
DROP INDEX targetedms.ix_precursor_id;
-- Dropping ix_qcmetricexclusion_replicateid [ReplicateId] because it overlaps with uq_qcmetricexclusion_replicate_metric [ReplicateId, MetricId]
DROP INDEX targetedms.ix_qcmetricexclusion_replicateid;
-- Dropping ix_irtpeptide_irtscaleid [iRTScaleId] because it overlaps with uq_irtpeptide_sequenceandscale [iRTScaleId, ModifiedSequence]
DROP INDEX targetedms.ix_irtpeptide_irtscaleid;
