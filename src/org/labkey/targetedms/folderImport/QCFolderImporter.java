package org.labkey.targetedms.folderImport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.admin.AbstractFolderImportFactory;
import org.labkey.api.admin.FolderImportContext;
import org.labkey.api.admin.FolderImporter;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobWarning;
import org.labkey.api.writer.VirtualFile;

import java.util.Collection;
import java.util.Collections;

public class QCFolderImporter implements FolderImporter
{
    protected static final String QC_FOLDER_DATA_TYPE = "TargetedMS QC Metrics"; //TODO: is there a preference on naming this?
    protected static final String QC_FOLDER_DIR = "targetedms";

    @Override
    public String getDataType()
    {
        return QC_FOLDER_DATA_TYPE;
    }

    @Override
    public String getDescription()
    {
        return QC_FOLDER_DATA_TYPE;
    }

    @Override
    public void process(@Nullable PipelineJob job, FolderImportContext ctx, VirtualFile root) throws Exception
    {
        // TODO: iterate through qc properties in QC_FOLDER_DIR and insert into appropriate tables
    }

    @Override
    public @NotNull Collection<PipelineJobWarning> postProcess(FolderImportContext ctx, VirtualFile root) throws Exception
    {
        return Collections.emptyList();
    }

    public static class Factory extends AbstractFolderImportFactory
    {
        @Override
        public FolderImporter create()
        {
            return new QCFolderImporter();
        }
    }
}
