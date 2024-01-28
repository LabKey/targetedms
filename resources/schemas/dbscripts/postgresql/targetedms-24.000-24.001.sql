UPDATE targetedms.QCMetricConfiguration SET
                                            EnabledQueryName = 'QCMetricEnabled_massErrorPrecursor',
                                            Series1QueryName = 'QCMetric_massErrorPrecursor',
                                            Name = 'Precursor Mass Error',
                                            Series1Label = 'Precursor Mass Error',
                                            YAxisLabel1 = 'Mass Error PPM'
WHERE Name = 'Mass Accuracy';

WITH rootIdentity AS (SELECT EntityId AS theIdentity FROM core.Containers WHERE Parent IS NULL)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, EnabledSchemaName, EnabledQueryName, YAxisLabel1) VALUES
    ((SELECT theIdentity FROM rootIdentity), 'Transition Mass Error','Transition Mass Error','targetedms','QCMetric_massErrorTransition', 'targetedms', 'QCMetricEnabled_massErrorTransition', 'Mass Error PPM')
;
