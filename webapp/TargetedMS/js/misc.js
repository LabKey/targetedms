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