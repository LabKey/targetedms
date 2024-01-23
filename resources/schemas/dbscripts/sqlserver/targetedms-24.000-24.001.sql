UPDATE targetedms.QCMetricConfiguration SET
                                            EnabledQueryName = 'QCMetricEnabled_massErrorPrecursor',
                                            Series1QueryName = 'QCMetric_massErrorPrecursor',
                                            Name = 'Precursor Mass Error',
                                            Series1Label = 'Mass Error (PPM)'
WHERE Name = 'Mass Accuracy';

WITH rootIdentity AS (SELECT EntityId AS theIdentity FROM core.Containers WHERE Parent IS NULL)

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, EnabledSchemaName, EnabledQueryName) VALUES
    ((SELECT theIdentity FROM rootIdentity), 'Transition Mass Error','Mass Error (PPM)','targetedms','QCMetric_massErrorTransition', 'targetedms', 'QCMetricEnabled_massErrorTransition')
;
