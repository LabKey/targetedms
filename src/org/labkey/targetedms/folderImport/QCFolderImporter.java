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
import java.util.List;

public class QCFolderImporter implements FolderImporter
{
    @Override
    public String getDataType()
    {
        return QCFolderConstants.QC_FOLDER_DATA_TYPE;
    }

    @Override
    public String getDescription()
    {
        return QCFolderConstants.QC_FOLDER_DATA_TYPE;
    }

    @Override
    public void process(@Nullable PipelineJob job, FolderImportContext ctx, VirtualFile root) throws Exception
    {
        if (null != job)
        {
            job.setStatus("IMPORT " + getDescription());
        }

        VirtualFile panoramaQCDir = root.getDir(QCFolderConstants.QC_FOLDER_DIR);
        List<String> filesToImport = root.getDir(QCFolderConstants.QC_FOLDER_DIR).list();
        for (String fileName : filesToImport)
        {
            PanoramaQCSettings qcSetting = PanoramaQCSettings.getSetting(fileName);
            if (null != qcSetting)
            {
                qcSetting.importSettings(ctx, panoramaQCDir);
            }
        }
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
