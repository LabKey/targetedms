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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.Readers;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.writer.VirtualFile;
import org.labkey.targetedms.TargetedMSSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public enum PanoramaQCSettings
{
    METRIC_CONFIG (TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, QCFolderConstants.QC_METRIC_CONFIGURATION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = schema.getTable(getTableName());
                    assert ti != null;
                    List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
                    SimpleFilter filter = SimpleFilter.createContainerFilter(container); //only export the ones that are defined in current container (and not the ones from the root container)

                    ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, filter, null);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }
            },
    QC_ENABLED_METRICS (TargetedMSSchema.TABLE_QC_ENABLED_METRICS, QCFolderConstants.QC_ENABLED_METRICS_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = schema.getTable(getTableName());
                    assert ti != null;
                    SQLFragment sql = new SQLFragment("SELECT qcMetricConfig.Name AS metric, qcEnabledMetrics.enabled, qcEnabledMetrics.lowerBound, qcEnabledMetrics.upperBound, qcEnabledMetrics.cusumLimit ")
                            .append(" FROM qcEnabledMetrics")
                            .append(" INNER JOIN qcMetricConfig")
                            .append(" ON qcEnabledMetrics.metric = qcMetricConfig.Id");

                    Map<String, TableInfo> tableMap = new HashMap<>();
                    tableMap.put("qcEnabledMetrics", ti);
                    tableMap.put("qcMetricConfig", schema.getTable(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION));

                    ResultsFactory factory = ()-> QueryService.get().selectResults(schema, sql.getSQL(), tableMap, null, true, true);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }

                @Override
                public long importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> tsvDataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> {
                        String metricValue = (String) row.get("metric");
                        Integer metricId = getRowIdFromName(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricValue), ctx.getUser(), ctx.getContainer());
                        row.put("metric", metricId);
                        String logMsg = "Row with 'Metric: " + metricValue + ", and Container: " + ctx.getContainer().getName() + "' already exists. Skipping";
                        getDataWithoutDuplicates(ctx, ti, row, tsvDataWithoutDuplicates, logMsg);
                    });
                    return tsvDataWithoutDuplicates.size() > 0 ? qus.insertRows(ctx.getUser(), ctx.getContainer(), tsvDataWithoutDuplicates, errors, null, null).size() : 0;
                }
            },
    QC_ANNOTATION_TYPE (TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, QCFolderConstants.QC_ANNOTATION_TYPE)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    ContainerFilter cf = ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), container, user);
                    TableInfo ti = schema.getTable(getTableName(), cf);
                    assert ti != null;
                    List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
                    ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, null, null);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }

                @Override
                public long importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
                {
                    TableInfo tinfo = schema.getTable(getTableName(), ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), ctx.getContainer(), ctx.getUser()));
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();
                    tsvData.forEach(row -> {
                        String name = (String) row.get("Name");
                        String logMsg = "Row with '" + name + "' already exists. Skipping";
                        getDataWithoutDuplicates(ctx, tinfo, row, dataWithoutDuplicates, logMsg);
                    });
                    return dataWithoutDuplicates.size() > 0 ? qus.insertRows(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, null, null).size() : 0;
                }

            },
    QC_ANNOTATION (TargetedMSSchema.TABLE_QC_ANNOTATION, QCFolderConstants.QC_ANNOTATION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = schema.getTable(getTableName());
                    assert ti != null;
                    SQLFragment sql = new SQLFragment("SELECT qcAnnotationType.Name AS QCAnnotationTypeId, qcAnnotation.Description, qcAnnotation.Date ")
                            .append(" FROM qcAnnotation")
                            .append(" INNER JOIN qcAnnotationType")
                            .append(" ON qcAnnotation.QCAnnotationTypeId = qcAnnotationType.Id");

                    Map<String, TableInfo> tableMap = new HashMap<>();
                    tableMap.put("qcAnnotation", ti);
                    tableMap.put("qcAnnotationType", schema.getTable(TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE));

                    ResultsFactory factory = ()-> QueryService.get().selectResults(schema, sql.getSQL(), tableMap, null, true, true);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }

                @Override
                public long importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();
                    tsvData.forEach(row -> {
                        String nameValue = (String) row.get("QCAnnotationTypeId");
                        Integer qcAnnotationTypeId = getRowIdFromName(TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), nameValue), ctx.getUser(), ctx.getContainer());
                        row.put("QCAnnotationTypeId", qcAnnotationTypeId);
                        String logMsg = "Row with '" + row.values() + "' values already exists. Skipping";
                        getDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates, logMsg);
                    });

                    return dataWithoutDuplicates.size() > 0 ? qus.insertRows(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, null, null).size() : 0;
                }
            },
    REPLICATE_ANNOTATION (TargetedMSSchema.TABLE_REPLICATE_ANNOTATION, QCFolderConstants.REPLICATE_ANNOTATION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = schema.getTable(getTableName());
                    assert ti != null;
                    List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
                    SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Source"), "Skyline", CompareType.NEQ);

                    ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, filter, null);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }
            },
    QC_METRIC_EXCLUSION (TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION, QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = schema.getTable(getTableName());
                    assert ti != null;
                    SQLFragment sql = new SQLFragment("SELECT replicate.Name AS ReplicateId, runs.FileName AS File, qcMetricConfig.Name AS MetricId ")
                            .append(" FROM qcMetricExclusion")
                            .append(" LEFT JOIN replicate")
                            .append(" ON qcMetricExclusion.ReplicateId = replicate.Id")
                            .append(" LEFT JOIN qcMetricConfig")
                            .append(" ON qcMetricExclusion.MetricId = qcMetricConfig.Id")
                            .append(" INNER JOIN runs")
                            .append(" ON replicate.RunId = runs.Id");

                    Map<String, TableInfo> tableMap = new HashMap<>();
                    tableMap.put("qcMetricExclusion", ti);
                    tableMap.put("replicate", schema.getTable(TargetedMSSchema.TABLE_REPLICATE));
                    tableMap.put("qcMetricConfig", schema.getTable(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION));
                    tableMap.put("runs", schema.getTable(TargetedMSSchema.TABLE_RUNS));

                    ResultsFactory factory = ()-> QueryService.get().selectResults(schema, sql.getSQL(), tableMap, null, true, true);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }

                @Override
                public long importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();
                    tsvData.forEach(row -> {

                        String replicateName = (String) row.get("ReplicateId");
                        String fileName = (String) row.get("File");
                        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Name"), replicateName);
                        filter.addCondition(FieldKey.fromString("RunId/FileName"), fileName);
                        Integer replicateId = getRowIdFromName(TargetedMSSchema.TABLE_REPLICATE, Set.of("Id"), filter, ctx.getUser(), ctx.getContainer());
                        row.put("ReplicateId", replicateId);

                        String metricName = (String) row.get("MetricId");
                        Integer metricId = getRowIdFromName(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricName), ctx.getUser(), ctx.getContainer());
                        row.put("MetricId", metricId);

                        //filter on values being imported to identify duplicates
                        filter = new SimpleFilter();
                        for(String col : row.keySet())
                        {
                            if (null == row.get(col))
                                filter.addCondition(FieldKey.fromParts(col), row.get(col), CompareType.ISBLANK);
                            else if (!col.equalsIgnoreCase("File"))
                                filter.addCondition(FieldKey.fromParts(col), row.get(col));
                        }
                        if (new TableSelector(ti, row.keySet(), filter, null).getRowCount() > 0)
                        {
                            ctx.getLogger().warn("Row with 'Replicate: " + replicateName + ", and Metric: " + metricName + "' already exists. Skipping");
                        }
                        else
                        {
                            dataWithoutDuplicates.add(row);
                        }
                    });
                    return dataWithoutDuplicates.size() > 0 ? qus.insertRows(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, null, null).size() : 0;

                }
            },
    QC_PLOT_SETTINGS (null, QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user)
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

                    try (PrintWriter out = vf.getPrintWriter(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME)) {

                        Properties prop = new Properties();
                        for (String name : plotSettings.keySet())
                        {
                            if (name.equalsIgnoreCase("metric"))
                            {
                                String metricValue = getMetricNameFromRowId(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, user, container, Integer.valueOf(plotSettings.get(name)));
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

                @Override
                public long importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
                {
                    int numProps = 0;
                    try (InputStream is = panoramaQCDir.getInputStream(getSettingsFileName()))
                    {
                        Properties props = new Properties();
                        props.load(is);
                        if (!ctx.getUser().isGuest())
                        {
                            PropertyManager.PropertyMap properties = PropertyManager.getWritableProperties(ctx.getUser(), ctx.getContainer(), QCFolderConstants.CATEGORY, true);
                            for (Map.Entry<Object, Object> entry : props.entrySet())
                            {
                                if (entry.getKey() instanceof String && entry.getValue() instanceof String)
                                {
                                    if(entry.getKey().toString().equalsIgnoreCase("metric"))
                                    {
                                        Integer metricRowId = getRowIdFromName(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), entry.getValue().toString()), ctx.getUser(), ctx.getContainer());
                                        properties.put(entry.getKey().toString(), String.valueOf(metricRowId));
                                    }
                                    else
                                    {
                                        properties.put(entry.getKey().toString(), entry.getValue().toString());
                                    }
                                }
                            }
                            properties.save();
                            numProps = props.size();
                        }
                    }
                    return numProps;
                }
            },
    GUIDE_SET (TargetedMSSchema.TABLE_GUIDE_SET, QCFolderConstants.GUIDE_SET_FILE_NAME),
    PRECURSOR_EXCLUSION (TargetedMSSchema.TABLE_PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION, QCFolderConstants.PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION_FILE_NAME);

    private final @Nullable String _tableName;
    private final String _settingsFileName;
    private static final Logger LOG = LogHelper.getLogger(PanoramaQCSettings.class, "Panorama QC Folder Settings");

    PanoramaQCSettings(String tableName, String fileName)
    {
        _tableName = tableName;
        _settingsFileName = fileName;
    }

    public void writeSettings(VirtualFile vf, Container container, User user)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo ti = schema.getTable(getTableName());
        assert ti != null;
        List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
        ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, null, null);
        writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
    }

    public long importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
    {
        DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(_settingsFileName)), true);
        List<Map<String, Object>> tsvData = loader.load();
        List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

        tsvData.forEach(row -> {
            String logMsg = "Row with '" + row.values() + "' values already exists. Skipping";
            getDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates, logMsg);
        });
        return dataWithoutDuplicates.size() > 0 ? qus.insertRows(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, null, null).size() : 0;
    }

    public String getTableName()
    {
        return _tableName;
    }

    public String getSettingsFileName()
    {
        return _settingsFileName;
    }

    private static void writeSettingsToTSV(VirtualFile vf, ResultsFactory factory, String fileName, String tableName)
    {
        try
        {
            if (factory.get().countAll() > 0)
            {
                try (TSVGridWriter tsvWriter = new TSVGridWriter(factory))
                {
                    tsvWriter.setApplyFormats(false);
                    tsvWriter.setColumnHeaderType(ColumnHeaderType.FieldKey);
                    PrintWriter out = vf.getPrintWriter(fileName);
                    tsvWriter.write(out);
                }
                catch (Exception e)
                {
                    LOG.error("Error writing results to " + fileName, e);
                }
            }
        }
        catch (IOException | SQLException e)
        {
            LOG.error("Error getting results from " + tableName, e);
        }
    }

    private static String getMetricNameFromRowId(String tableName, User user, Container c, Integer rowId)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);
        TableInfo ti = schema.getTable(tableName);
        assert ti != null;
        return new TableSelector(ti, Set.of("Name"), new SimpleFilter(FieldKey.fromParts("Id"), rowId), null).getObject(String.class);
    }

    private static Integer getRowIdFromName(String tableName, Set<String> colNames, SimpleFilter filter, User user, Container c)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);
        TableInfo ti = schema.getTable(tableName);
        assert ti != null;
        return new TableSelector(ti, colNames, filter, null).getObject(Integer.class);
    }

    private static void getDataWithoutDuplicates(FolderImportContext ctx, @Nullable TableInfo ti, Map<String, Object> row, List<Map<String, Object>> dataWithoutDuplicates, String logMsg)
    {
        //filter on values being imported to identify duplicates
        SimpleFilter filter = new SimpleFilter();
        for(String col : row.keySet())
        {
            if (null == row.get(col))
            {
                filter.addCondition(FieldKey.fromParts(col), row.get(col), CompareType.ISBLANK);
            }
            else
            {
                filter.addCondition(FieldKey.fromParts(col), row.get(col));
            }
        }
        if (new TableSelector(ti, row.keySet(), filter, null).getRowCount() > 0)
        {
            ctx.getLogger().warn(logMsg);
        }
        else
        {
            dataWithoutDuplicates.add(row);
        }
    }
}
