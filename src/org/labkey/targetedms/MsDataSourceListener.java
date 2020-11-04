package org.labkey.targetedms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentListener;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileListener;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.security.User;
import org.labkey.targetedms.query.ReplicateManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class MsDataSourceListener implements ExperimentListener, FileListener
{
    @Override
    public String getSourceName()
    {
        return "MsDataSourceListener";
    }

    @Override
    public void fileCreated(@NotNull File created, @Nullable User user, @Nullable Container container)
    {
        fileCreated(created.toPath(), user, container);
    }

    @Override
    public void fileCreated(@NotNull Path created, @Nullable User user, @Nullable Container container)
    {
        if(!isTargetedmsContainer(container))
        {
            return;
        }
        if(!Files.isDirectory(created))
        {
            // We will ignore directories; assuming that the directory-based data sources (Waters, Bruker and Agilent) will
            // get automatically zipped up when uploaded through the FWP.
            ExpData data = ExperimentService.get().getExpDataByURL(created, container);
            if(data != null)
            {
                TargetedMSManager.addIfDataSource(data, user, container);
            }
        }
    }

    @Override
    public void fileMoved(@NotNull File src, @NotNull File dest, @Nullable User user, @Nullable Container container)
    {

    }

    @Override
    public Collection<File> listFiles(@Nullable Container container)
    {
        return null;
    }

    @Override
    public SQLFragment listFilesQuery()
    {
        return null;
    }

    @Override
    public void beforeDataDelete(Container c, User user, List<? extends ExpData> datas)
    {
        TargetedMSManager.removeFromMsDataSource(datas);
    }

    private boolean isTargetedmsContainer(@Nullable Container container)
    {
        return container.getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class));
    }
}
