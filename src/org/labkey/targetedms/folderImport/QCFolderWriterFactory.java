package org.labkey.targetedms.folderImport;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.admin.BaseFolderWriter;
import org.labkey.api.admin.FolderWriter;
import org.labkey.api.admin.FolderWriterFactory;
import org.labkey.api.admin.ImportContext;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ResultsFactory;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.writer.VirtualFile;
import org.labkey.folder.xml.FolderDocument;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

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
        public boolean show(Container c)
        {
            TargetedMSManager.getFolderType(c);
            TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(c);
            return folderType == TargetedMSService.FolderType.QC;
        }

        @Override
        public void write(Container object, ImportContext<FolderDocument.Folder> ctx, VirtualFile root) throws Exception
        {
            VirtualFile vf = root.getDir(QCFolderImporter.QC_FOLDER_DIR);
            TargetedMSSchema schema = new TargetedMSSchema(ctx.getUser(), object);

            //1. New custom metrics and trace metrics - only export metrics defined in the current container
            writeResultsToTSV(schema,"QCMetricConfiguration", vf, "QCMetricConfiguration.tsv", SimpleFilter.createContainerFilter(object), null);

            //2. Metric configurations and settings
            writeResultsToTSV(schema,"QCEnabledMetrics", vf, "QCEnabledMetrics.tsv", null, null);

            //3. Guide sets
            writeResultsToTSV(schema,"GuideSet", vf, "GuideSet.tsv", null, null);

            //4. Excluded samples and the metric(s) the samples are being excluded from Peptide/molecule groups
            writeResultsToTSV(schema,"QCMetricExclusion", vf, "QCMetricExclusion.tsv", null, null);

            //5. Excluded Precursors
            writeResultsToTSV(schema,"ExcludedPrecursors", vf, "ExcludedPrecursors.tsv", null, null);

            //6. Annotations

            //QC Annotation
            writeResultsToTSV(schema,"QCAnnotation", vf, "QCAnnotation.tsv", null, null);

            //QC Annotation Type - get from both shared and current containers
            writeResultsToTSV(schema,"QCAnnotationType", vf, "QCAnnotationType.tsv", null,
                    ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), object, ctx.getUser()));

            //QC Replicate Annotation - filter on Source != "Skyline"
            SimpleFilter sourceFilter = new SimpleFilter(FieldKey.fromString("Source"), "Skyline", CompareType.NEQ);
            writeResultsToTSV(schema,"ReplicateAnnotation", vf, "ReplicateAnnotation.tsv", sourceFilter, null);

            //7. Plot settings from default view:
            // Plot metric,
            // Date Range,
            // Plot size,
            // QC Plot type,
            // Y-Axis Scale
            // Plot Display oOptions:
            //-Group X-Axis Values by Date - checkbox value
            //-Show All Series in a Single Plot - checkbox value
            //-Show Excluded Points - checkbox value
            //-Show Reference Guide Set - checkbox value
            //-Show Excluded Precursors– checkbox value


        }
    }

    private void writeResultsToTSV(TargetedMSSchema schema, String queryName, VirtualFile vf, String fileName, @Nullable SimpleFilter filter, @Nullable ContainerFilter cf) throws IOException
    {
        TableInfo ti = schema.getTable(queryName, cf);
        List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
        ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, filter, null, null, false);

        try (TSVGridWriter tsvWriter = new TSVGridWriter(factory))
        {
            tsvWriter.setApplyFormats(false);
            tsvWriter.setColumnHeaderType(ColumnHeaderType.FieldKey);
            PrintWriter out = vf.getPrintWriter(fileName);
            tsvWriter.write(out);
        }
    }
}
