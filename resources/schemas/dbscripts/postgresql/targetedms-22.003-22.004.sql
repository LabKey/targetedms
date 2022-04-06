WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    ((select theIdentity from rootIdentity), 'Library dotp', 'Library dotp', 'targetedms', 'QCMetric_libraryDotp', true, 'QCMetricEnabled_libraryDotp', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    ((select theIdentity from rootIdentity), 'Isotope dotp', 'Isotope dotp', 'targetedms', 'QCMetric_isotopeDotp', true, 'QCMetricEnabled_isotopeDotp', 'targetedms');
