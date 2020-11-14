package org.labkey.api.targetedms;

import org.jetbrains.annotations.NotNull;
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

    /**
     * @param sampleFiles list of sample files for which we should check if data exists
     * @param container container where we should look for the data
     * @return list of sample files for which data was found
     */
    @NotNull
    List<? extends ISampleFile> hasData(@NotNull List<? extends ISampleFile> sampleFiles, @NotNull Container container);

//    /**
//     * @param fileNames list of file names for which we should check if data exists
//     * @param container container where we should look for the data
//     * @param allowBaseNameMatch Set to true if the input files names do not have an extension and we want to find files that match the base name.
//     * @return list of files for which data was found
//     */
//    List<String> hasData(List<String> fileNames, Container container, boolean allowBaseNameMatch);
}
