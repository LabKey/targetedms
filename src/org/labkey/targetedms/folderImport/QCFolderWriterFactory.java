package org.labkey.targetedms.folderImport;

import org.labkey.api.admin.BaseFolderWriter;
import org.labkey.api.admin.FolderWriter;
import org.labkey.api.admin.FolderWriterFactory;
import org.labkey.api.admin.ImportContext;
import org.labkey.api.data.Container;
import org.labkey.api.writer.VirtualFile;
import org.labkey.folder.xml.FolderDocument;

public class QCFolderWriterFactory implements FolderWriterFactory
{
    @Override
    public FolderWriter create()
    {
        return new QCFolderWriter();
    }

    public class QCFolderWriter extends BaseFolderWriter
    {
        @Override
        public String getDataType()
        {
            return QCFolderImporter.QC_FOLDER_DATA_TYPE;
        }

        @Override
        public void write(Container object, ImportContext<FolderDocument.Folder> ctx, VirtualFile root) throws Exception
        {
            //TODO: Get these QC settings from appropriate tables and write it to a .tsv or .properties (if applicable) into QCFolderImporter.QC_FOLDER_DIR:
            //1. New custom metrics and trace metrics
            //2. Metric configurations and settings
            //3. Guide sets
            //4. Excluded samples and the metric(s) the samples are being excluded from Peptide/molecule groups
            //5. Annotations - QC Annotation, QC Annotation Type, Replicate Annotation
            //6. Plot settings from default view - Plot metric, Date Range, Plot size, QC Plot type, Y-Axis Scale
            //7. Replicate Annotation filter, if any

            //8. plot display options:
            //-Group X-Axis Values by Date - checkbox value
            //-Show All Series in a Single Plot - checkbox value
            //-Show Excluded Points - checkbox value
            //-Show Reference Guide Set - checkbox value
            //-Show Excluded Precursors– checkbox value
        }

        // TODO: override other methods, if needed
    }
}
