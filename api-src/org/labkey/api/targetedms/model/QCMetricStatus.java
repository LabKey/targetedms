package org.labkey.api.targetedms.model;

public enum QCMetricStatus
{
    LeveyJennings,
    ValueCutoff,
    MeanDeviationCutoff,
    PlotOnly,
    Disabled,
    NoData;

    public final static QCMetricStatus DEFAULT = LeveyJennings;
}
