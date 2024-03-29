package org.labkey.api.targetedms;

import java.util.Date;

public interface ISampleFile
{
    long getId();
    long getReplicateId();
    String getFilePath();
    String getSampleName();
    Date getAcquiredTime();
    Date getModifiedTime();
    Long getInstrumentId();
    String getSkylineId();
    Double getTicArea();
    String getInstrumentSerialNumber();
    String getSampleId();
    Double getExplicitGlobalStandardArea();
    String getIonMobilityType();
    String getFileName();
}
