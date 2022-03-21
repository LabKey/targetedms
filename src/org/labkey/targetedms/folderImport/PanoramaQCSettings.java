package org.labkey.targetedms.folderImport;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.admin.FolderImportContext;
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
import java.io.InputStream;
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
    QC_ENABLED_METRICS (TargetedMSSchema.TABLE_QC_ENABLED_METRICS, QCFolderConstants.QC_ENABLED_METRICS_FILE_NAME),
    GUIDE_SET (TargetedMSSchema.TABLE_GUIDE_SET, QCFolderConstants.GUIDE_SET_FILE_NAME),
    QC_METRIC_EXCLUSION (TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION, QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME),
    PRECURSOR_EXCLUSION (TargetedMSSchema.TABLE_PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION, QCFolderConstants.PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION_FILE_NAME),
    QC_ANNOTATION (TargetedMSSchema.TABLE_QC_ANNOTATION, QCFolderConstants.QC_ANNOTATION_FILE_NAME),
    QC_ANNOTATION_TYPE (TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, QCFolderConstants.QC_ANNOTATION_TYPE),
    REPLICATE_ANNOTATION (TargetedMSSchema.TABLE_REPLICATE_ANNOTATION, QCFolderConstants.REPLICATE_ANNOTATION_FILE_NAME),
    QC_PLOT_SETTINGS (null, QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME);

    private final @Nullable String _tableName;
    private final String _settingsFileName;
    private static final Logger LOG = LogHelper.getLogger(QCFolderWriterFactory.class, "Panorama QC Folder Settings");

    PanoramaQCSettings(String tableName, String fileName)
    {
        _tableName = tableName;
        _settingsFileName = fileName;
    }

    public static @Nullable PanoramaQCSettings getSetting(String fileName)
    {
        switch (fileName)
        {
            case QCFolderConstants.QC_METRIC_CONFIGURATION_FILE_NAME:
                return METRIC_CONFIG;
            case QCFolderConstants.QC_ENABLED_METRICS_FILE_NAME:
                return QC_ENABLED_METRICS;
            case QCFolderConstants.GUIDE_SET_FILE_NAME:
                return GUIDE_SET;
            case QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME:
                return QC_METRIC_EXCLUSION;
            case QCFolderConstants.PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION_FILE_NAME:
                return PRECURSOR_EXCLUSION;
            case QCFolderConstants.QC_ANNOTATION_FILE_NAME:
                return QC_ANNOTATION;
            case QCFolderConstants.QC_ANNOTATION_TYPE:
                return QC_ANNOTATION_TYPE;
            case QCFolderConstants.REPLICATE_ANNOTATION_FILE_NAME:
                return REPLICATE_ANNOTATION;
            case QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME:
                return QC_PLOT_SETTINGS;
            default:
                return null;
        }
    }

    protected void writeResults(String name, VirtualFile vf, Container container, User user)
    {
        if (name.equals(QC_PLOT_SETTINGS.name()))
        {
            writePlotSettingsToPropertiesFile(vf, container, user);
        }
        else
        {
            writeSettingsToTSV(name, vf, container, user);
        }
    }

    private void writeSettingsToTSV(String name, VirtualFile vf, Container container, User user)
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
                    PrintWriter out = vf.getPrintWriter(_settingsFileName);
                    tsvWriter.write(out);
                }
                catch (Exception e)
                {
                    LOG.error("Error writing results to " + _settingsFileName, e);
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

    protected void importSettings(FolderImportContext ctx, VirtualFile panoramaQCDir)
    {
        if (_settingsFileName.equalsIgnoreCase(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME))
        {
            importQCProperties(ctx, panoramaQCDir);
        }
        else
        {
            importFromTsv(ctx, panoramaQCDir);
        }
    }

    private void importFromTsv(FolderImportContext ctx, VirtualFile panoramaQCDir)
    {

    }

    private void importQCProperties(FolderImportContext ctx, VirtualFile panoramaQCDir)
    {
        try (InputStream is = panoramaQCDir.getInputStream(_settingsFileName))
        {
            Properties props = new Properties();
            props.load(is);
            if (!ctx.getUser().isGuest())
            {
                ctx.getLogger().info("Starting QC Plot settings import");

                PropertyManager.PropertyMap properties = PropertyManager.getWritableProperties(ctx.getUser(), ctx.getContainer(), QCFolderConstants.CATEGORY, true);
                for (Map.Entry<Object, Object> entry : props.entrySet())
                {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof String)
                    {
                        if(entry.getKey().toString().equalsIgnoreCase("metric"))
                        {
                            String metricRowId = getMetricRowIdFromName(ctx.getUser(), ctx.getContainer(), entry.getValue().toString());
                            properties.put(entry.getKey().toString(), metricRowId);
                        }
                        else
                        {
                            properties.put(entry.getKey().toString(), entry.getValue().toString());
                        }
                    }
                }
                properties.save();
                ctx.getLogger().info("Finished importing QC Plot settings");
            }
        }
        catch(Exception e)
        {
            LOG.error("Error importing QC Plot settings from " + QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME + ": " + e.getMessage(), e);
        }
    }

    private String getMetricRowIdFromName(User user, Container c, String metricName)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);
        TableInfo ti = schema.getTable("QCMetricConfiguration");
        assert ti != null;
        return new TableSelector(ti, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricName), null).getObject(String.class);
    }
}
