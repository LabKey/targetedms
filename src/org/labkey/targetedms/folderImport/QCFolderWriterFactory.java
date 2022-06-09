package org.labkey.targetedms.folderImport;

import org.labkey.api.admin.BaseFolderWriter;
import org.labkey.api.admin.FolderWriter;
import org.labkey.api.admin.FolderWriterFactory;
import org.labkey.api.admin.ImportContext;
import org.labkey.api.data.Container;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.writer.VirtualFile;
import org.labkey.folder.xml.FolderDocument;
import org.labkey.targetedms.TargetedMSManager;

public class QCFolderWriterFactory implements FolderWriterFactory
{
    @Override
    public FolderWriter create()
    {
        return new QCFolderWriter();
    }

    private static class QCFolderWriter extends BaseFolderWriter
    {
        @Override
        public String getDataType()
        {
            return TargetedMSService.QC_FOLDER_DATA_TYPE;
        }

        @Override
        public boolean show(Container c)
        {
            TargetedMSManager.getFolderType(c);
            TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(c);
            return folderType == TargetedMSService.FolderType.QC;
        }

        @Override
        public void write(Container object, ImportContext<FolderDocument.Folder> ctx, VirtualFile root) throws Exception
        {
            VirtualFile vf = root.getDir(QCFolderConstants.QC_FOLDER_DIR);

            for (PanoramaQCSettings setting : PanoramaQCSettings.values())
            {
                setting.exportSettings(vf, object, ctx.getUser());
            }
        }
    }


}
