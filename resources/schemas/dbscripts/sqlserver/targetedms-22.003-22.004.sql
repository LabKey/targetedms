declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    (@rootIdentity, 'Library dotp', 'Library dotp', 'targetedms', 'QCMetric_libraryDotp', 1, 'QCMetricEnabled_libraryDotp', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    (@rootIdentity, 'Isotope dotp', 'Isotope dotp', 'targetedms', 'QCMetric_isotopeDotp', 1, 'QCMetricEnabled_isotopeDotp', 'targetedms');
