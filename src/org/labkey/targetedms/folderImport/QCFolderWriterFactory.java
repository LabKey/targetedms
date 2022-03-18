package org.labkey.targetedms.folderImport;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
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
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.ResultsFactory;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.writer.VirtualFile;
import org.labkey.folder.xml.FolderDocument;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class QCFolderWriterFactory implements FolderWriterFactory
{
    private static final Logger LOG = LogHelper.getLogger(QCFolderWriterFactory.class, "Panorama QC Folder Settings Export");

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
            return QCFolderConstants.QC_FOLDER_DATA_TYPE;
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
            TargetedMSSchema schema = new TargetedMSSchema(ctx.getUser(), object);

            //1. New custom metrics and trace metrics - only export metrics defined in the current container
            writeResultsToTSV(schema, TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, vf, QCFolderConstants.QC_METRIC_CONFIGURATION_FILE_NAME, SimpleFilter.createContainerFilter(object), null);

            //2. Metric configurations and settings
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_QC_ENABLED_METRICS, vf, QCFolderConstants.QC_ENABLED_METRICS_FILE_NAME, null, null);

            //3. Guide sets
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_GUIDE_SET, vf, QCFolderConstants.GUIDE_SET_FILE_NAME, null, null);

            //4. Excluded samples and the metric(s) the samples are being excluded from Peptide/molecule groups
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION, vf, QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME, null, null);

            //5. Excluded Precursors
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION, vf, QCFolderConstants.PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION_FILE_NAME, null, null);

            //6. Annotations:

            //QC Annotation
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_QC_ANNOTATION, vf, QCFolderConstants.QC_ANNOTATION_FILE_NAME, null, null);

            //QC Annotation Type - get from both shared and current containers
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, vf, QCFolderConstants.QC_ANNOTATION_TYPE, null,
                    ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), object, ctx.getUser()));

            //QC Replicate Annotation - filter on Source != "Skyline"
            SimpleFilter sourceFilter = new SimpleFilter(FieldKey.fromString("Source"), "Skyline", CompareType.NEQ);
            writeResultsToTSV(schema,TargetedMSSchema.TABLE_REPLICATE_ANNOTATION, vf, QCFolderConstants.REPLICATE_ANNOTATION_FILE_NAME, sourceFilter, null);

            //7. Plot settings from default view
            writePlotSettingsToPropertiesFile(ctx.getUser(), object, vf);
        }
    }

    private void writePlotSettingsToPropertiesFile(User user, Container c, VirtualFile vf)
    {
        // Plot metric,
        // Date Range,
        // Plot size,
        // QC Plot type,
        // Y-Axis Scale
        // Plot Display Options:
        //-Group X-Axis Values by Date - checkbox value
        //-Show All Series in a Single Plot - checkbox value
        //-Show Excluded Points - checkbox value
        //-Show Reference Guide Set - checkbox value
        //-Show Excluded Precursors– checkbox value

        Map<String, String> plotSettings = PropertyManager.getProperties(user, c, QCFolderConstants.CATEGORY);

        try (PrintWriter out = vf.getPrintWriter(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME);) {

            Properties prop = new Properties();
            for (String name : plotSettings.keySet())
            {
                prop.put(name, plotSettings.get(name));
            }
            prop.store(out, null);

        }
        catch (Exception e)
        {
            LOG.log(Level.ERROR, "Error exporting 'default view QC Plot settings'", e);
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
