package org.labkey.api.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.services.ServiceRegistry;

import java.io.IOException;
import java.util.List;

public interface RawDataService
{
    static RawDataService get()
    {
        return ServiceRegistry.get().getService(RawDataService.class);
    }

    static void setInstance(RawDataService impl)
    {
        ServiceRegistry.get().registerService(RawDataService.class, impl);
    }

    List<ISampleFile> hasData(List<ISampleFile> sampleFiles, Container container) throws IOException;

    ExpData linkRawData(ISampleFile sampleFile, Container container) throws IOException;

    String getWebdavUrl(ISampleFile sampleFile, Container container) throws IOException;
}
