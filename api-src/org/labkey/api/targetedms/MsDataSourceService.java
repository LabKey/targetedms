package org.labkey.api.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.services.ServiceRegistry;

import java.util.List;

public interface MsDataSourceService
{
    static MsDataSourceService get()
    {
        return ServiceRegistry.get().getService(MsDataSourceService.class);
    }

    static void setInstance(MsDataSourceService impl)
    {
        ServiceRegistry.get().registerService(MsDataSourceService.class, impl);
    }

    List<? extends ISampleFile> hasData(List<? extends ISampleFile> sampleFiles, Container container);
}
