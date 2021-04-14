package org.labkey.api.targetedms;

public interface IPeptideGroup
{
    long getId();
    long getRunId();
    Integer getSequenceId();
    String getLabel();
    String getName();
    RepresentativeDataState getRepresentativeDataState();
}
