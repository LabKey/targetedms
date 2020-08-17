ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsolationWindow ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.Instrument ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.SampleFile ALTER COLUMN InstrumentId TYPE bigint;

ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItem ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItemValue ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.NistLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionLoss ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN Id TYPE bigint;