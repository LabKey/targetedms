package org.labkey.api.targetedms;

public interface IGeneralPrecursor
{
    long getId();
    int getCharge();
    double getMz();
    RepresentativeDataState getRepresentativeDataState();
}
