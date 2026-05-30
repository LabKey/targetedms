/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

if (!LABKEY.targetedms.MetricStatus) {
    LABKEY.targetedms.MetricStatus = {
        LeveyJennings: 'LeveyJennings',
        ValueCutoff: 'ValueCutoff',
        MeanDeviationCutoff: 'MeanDeviationCutoff',
        PlotOnly: 'PlotOnly',
        Disabled: 'Disabled',
        NoData: 'NoData'
    };
}