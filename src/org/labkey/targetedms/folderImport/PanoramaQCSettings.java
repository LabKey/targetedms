package org.labkey.targetedms.folderImport;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
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
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.writer.VirtualFile;
import org.labkey.targetedms.TargetedMSSchema;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public enum PanoramaQCSettings
{
    METRIC_CONFIG (TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, QCFolderConstants.QC_METRIC_CONFIGURATION_FILE_NAME),
    QC_ENABLED_METRICS( TargetedMSSchema.TABLE_QC_ENABLED_METRICS, QCFolderConstants.QC_ENABLED_METRICS_FILE_NAME),
    GUIDE_SET (TargetedMSSchema.TABLE_GUIDE_SET, QCFolderConstants.GUIDE_SET_FILE_NAME),
    QC_METRIC_EXCLUSION (TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION, QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME),
    PRECURSOR_EXCLUSION (TargetedMSSchema.TABLE_PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION, QCFolderConstants.PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION_FILE_NAME),
    QC_ANNOTATION (TargetedMSSchema.TABLE_QC_ANNOTATION, QCFolderConstants.QC_ANNOTATION_FILE_NAME),
    QC_ANNOTATION_TYPE (TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, QCFolderConstants.QC_ANNOTATION_TYPE),
    REPLICATE_ANNOTATION (TargetedMSSchema.TABLE_REPLICATE_ANNOTATION, QCFolderConstants.REPLICATE_ANNOTATION_FILE_NAME),
    QC_PLOT_SETTINGS (null, QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME);

    private final @Nullable String _tableName;
    private final String _settingFileName;
    private static final Logger LOG = LogHelper.getLogger(QCFolderWriterFactory.class, "Panorama QC Folder Settings");

    PanoramaQCSettings(String tableName, String fileName)
    {
        _tableName = tableName;
        _settingFileName = fileName;
    }

    protected void writeResults(String name, VirtualFile vf, Container container, User user)
    {
        if (name.equals(QC_PLOT_SETTINGS.name()))
        {
            writePlotSettingsToPropertiesFile(vf, container, user);
        }
        else
        {
            writeResultsToTSV(name, vf, container, user);
        }
    }

    private void writeResultsToTSV(String name, VirtualFile vf, Container container, User user)
    {
        SimpleFilter filter = null;
        ContainerFilter cf = QC_ANNOTATION_TYPE.name().equals(name) ? ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), container, user) : null;

        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo ti = schema.getTable(_tableName, cf);

        if (METRIC_CONFIG.name().equals(name))
            filter = SimpleFilter.createContainerFilter(container);
        else if (REPLICATE_ANNOTATION.name().equals(name))
            filter = new SimpleFilter(FieldKey.fromString("Source"), "Skyline", CompareType.NEQ);

        List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
        final SimpleFilter finalFilter = filter;
        ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, finalFilter, null, null, false);

        try
        {
            if (factory.get().countAll() > 0)
            {
                try (TSVGridWriter tsvWriter = new TSVGridWriter(factory))
                {
                    tsvWriter.setApplyFormats(false);
                    tsvWriter.setColumnHeaderType(ColumnHeaderType.FieldKey);
                    PrintWriter out = vf.getPrintWriter(_settingFileName);
                    tsvWriter.write(out);
                }
                catch (Exception e)
                {
                    LOG.error("Error writing results to " + _settingFileName, e);
                }
            }
        }
        catch (IOException | SQLException e)
        {
            LOG.error("Error getting results from " + _tableName, e);
        }
    }

    private static void writePlotSettingsToPropertiesFile(VirtualFile vf, Container container, User user)
    {
        // Should get these settings if present:
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

        Map<String, String> plotSettings = PropertyManager.getProperties(user, container, QCFolderConstants.CATEGORY);

        try (PrintWriter out = vf.getPrintWriter(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME);) {

            Properties prop = new Properties();
            for (String name : plotSettings.keySet())
            {
                if (name.equalsIgnoreCase("metric"))
                {
                    String metricValue = getMetricNameFromRowId(user, container, Integer.valueOf(plotSettings.get(name)));
                    prop.put(name, metricValue);
                }
                else
                {
                    prop.put(name, plotSettings.get(name));
                }
            }
            prop.store(out, null);

        }
        catch (Exception e)
        {
            LOG.error("Error exporting 'default view QC Plot settings'", e);
        }
    }

    private static String getMetricNameFromRowId(User user, Container c, Integer rowId)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);
        TableInfo ti = schema.getTable("QCMetricConfiguration");
        assert ti != null;
        return new TableSelector(ti, Set.of("Name"), new SimpleFilter(FieldKey.fromParts("Id"), rowId), null).getObject(String.class);
    }
}
