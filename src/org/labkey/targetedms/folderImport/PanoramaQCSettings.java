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
                public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
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
                public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
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
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> {
                        String metricValue = (String) row.get("metric");
                        Integer metricId = getRowIdFromName(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricValue), ctx.getUser(), ctx.getContainer());
                        row.put("metric", metricId);
                        getDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates);
                    });
                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    QC_ANNOTATION_TYPE (TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, QCFolderConstants.QC_ANNOTATION_TYPE)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
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
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    assert schema != null;
                    TableInfo tinfo = schema.getTable(getTableName(), ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), ctx.getContainer(), ctx.getUser()));
                    return importSettings(ctx, tinfo, getSettingsFileName(), panoramaQCDir, errors, qus);
                }
            },
    QC_ANNOTATION (TargetedMSSchema.TABLE_QC_ANNOTATION, QCFolderConstants.QC_ANNOTATION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
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
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> {
                        String nameValue = (String) row.get("QCAnnotationTypeId");
                        Integer qcAnnotationTypeId = getRowIdFromName(TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), nameValue), ctx.getUser(), ctx.getContainer());
                        row.put("QCAnnotationTypeId", qcAnnotationTypeId);
                        getDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates);
                    });

                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    REPLICATE_ANNOTATION (TargetedMSSchema.TABLE_REPLICATE_ANNOTATION, QCFolderConstants.REPLICATE_ANNOTATION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = schema.getTable(getTableName());
                    assert ti != null;

                    SQLFragment sql = new SQLFragment("SELECT replicateAnnotation.ReplicateId.Name AS ReplicateId, runs.FileName AS File, replicateAnnotation.Name, replicateAnnotation.Value, replicateAnnotation.Source ")
                            .append(" FROM replicateAnnotation")
                            .append(" INNER JOIN replicate")
                            .append(" ON replicateAnnotation.replicateId = replicate.Id")
                            .append(" INNER JOIN runs")
                            .append(" ON replicate.RunId = runs.Id")
                            .append(" WHERE Source != 'Skyline' ");

                    Map<String, TableInfo> tableMap = new HashMap<>();
                    tableMap.put("replicateAnnotation", ti);
                    tableMap.put("replicate", schema.getTable(TargetedMSSchema.TABLE_REPLICATE));
                    tableMap.put("runs", schema.getTable(TargetedMSSchema.TABLE_RUNS));

                    ResultsFactory factory = ()-> QueryService.get().selectResults(schema, sql.getSQL(), tableMap, null, true, true);
                    writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> getReplicateDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates));
                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    QC_METRIC_EXCLUSION (TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION, QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
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
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(getSettingsFileName())), true);
                    List<Map<String, Object>> tsvData = loader.load();
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> {
                        String metricName = (String) row.get("MetricId");
                        Integer metricId = getRowIdFromName(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Set.of("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricName), ctx.getUser(), ctx.getContainer());
                        row.put("MetricId", metricId);
                        getReplicateDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates);
                    });
                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    QC_PLOT_SETTINGS (null, QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME)
            {
                @Override
                public void writeSettings(VirtualFile vf, Container container, User user) throws IOException
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
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws SQLException, QueryUpdateServiceException, BatchValidationException, DuplicateKeyException, IOException
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

    PanoramaQCSettings(@Nullable String tableName, String fileName)
    {
        _tableName = tableName;
        _settingsFileName = fileName;
    }

    public @Nullable String getTableName()
    {
        return _tableName;
    }

    public String getSettingsFileName()
    {
        return _settingsFileName;
    }

    public void writeSettings(VirtualFile vf, Container container, User user) throws Exception
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo ti = schema.getTable(getTableName());
        assert ti != null;
        List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ci -> ci.isUserEditable()).collect(Collectors.toList());
        ResultsFactory factory = ()-> QueryService.get().select(ti, userEditableCols, null, null);
        writeSettingsToTSV(vf, factory, getSettingsFileName(), getTableName());
    }

    protected void writeSettingsToTSV(VirtualFile vf, ResultsFactory factory, String fileName, String tableName) throws Exception
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
                catch (IOException e)
                {
                    throw new IOException("Error writing results to " + fileName, e);
                }
            }
        }
        catch (IOException | SQLException e)
        {
            throw new Exception("Error getting results from " + tableName, e);
        }
    }

    protected String getMetricNameFromRowId(User user, Container c, Integer rowId)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);
        TableInfo ti = schema.getTable(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION);
        assert ti != null;
        return new TableSelector(ti, Set.of("Name"), new SimpleFilter(FieldKey.fromParts("Id"), rowId), null).getObject(String.class);
    }

    protected Integer getRowIdFromName(String tableName, Set<String> colNames, SimpleFilter filter, User user, Container c)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);
        TableInfo ti = schema.getTable(tableName);
        assert ti != null;
        return new TableSelector(ti, colNames, filter, null).getObject(Integer.class);
    }

    protected void getReplicateDataWithoutDuplicates(FolderImportContext ctx, @Nullable TableInfo ti, Map<String, Object> row, List<Map<String, Object>> dataWithoutDuplicates)
    {
        String replicateName = (String) row.get("ReplicateId");
        String fileName = (String) row.get("File");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Name"), replicateName);
        filter.addCondition(FieldKey.fromString("RunId/FileName"), fileName);
        Integer replicateId = getRowIdFromName(TargetedMSSchema.TABLE_REPLICATE, Set.of("Id"), filter, ctx.getUser(), ctx.getContainer());
        row.put("ReplicateId", replicateId);

        //filter on values being imported to identify duplicates
        filter = new SimpleFilter();
        String logMsg = "A row containing [";
        int count = 0;
        for(String col : row.keySet())
        {
            if (col.equalsIgnoreCase("File"))
                continue;

            if (null == row.get(col))
                filter.addCondition(FieldKey.fromParts(col), row.get(col), CompareType.ISBLANK);
            else
                filter.addCondition(FieldKey.fromParts(col), row.get(col));

            logMsg += col + ": '" + row.get(col) + "'" + (count == row.size()-2 ? "" : ", ") ;
            ++count;
        }

        assert ti != null;
        if (new TableSelector(ti, row.keySet(), filter, null).getRowCount() > 0)
        {
            logMsg += "] values already exists. Skipping";
            ctx.getLogger().warn(logMsg);
        }
        else
        {
            dataWithoutDuplicates.add(row);
        }
    }

    protected void getDataWithoutDuplicates(FolderImportContext ctx, @Nullable TableInfo ti, Map<String, Object> row, List<Map<String, Object>> dataWithoutDuplicates)
    {
        //filter on values being imported to identify duplicates
        SimpleFilter filter = new SimpleFilter();
        String logMsg = "A row containing [";
        int count = 0;
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
            logMsg += col + ": '" + row.get(col) + "'" + (count == row.size()-1 ? "" : ", ");
            ++count;
        }
        assert ti != null;
        if (new TableSelector(ti, row.keySet(), filter, null).getRowCount() > 0)
        {
            logMsg += "] values already exists. Skipping";
            ctx.getLogger().warn(logMsg);
        }
        else
        {
            dataWithoutDuplicates.add(row);
        }
    }

    public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
    {
        return importSettings(ctx, ti, _settingsFileName, panoramaQCDir, errors, qus);
    }

    protected int importSettings(FolderImportContext ctx, TableInfo tinfo, String settingsFileName, VirtualFile panoramaQCDir, @Nullable BatchValidationException errors, @Nullable QueryUpdateService qus) throws Exception
    {
        DataLoader loader = new TabLoader(Readers.getReader(panoramaQCDir.getInputStream(settingsFileName)), true);
        List<Map<String, Object>> tsvData = loader.load();
        List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

        tsvData.forEach(row -> getDataWithoutDuplicates(ctx, tinfo, row, dataWithoutDuplicates));
        return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
    }

    protected int insertData(User user, Container container, List<Map<String, Object>> dataWithoutDuplicates, BatchValidationException errors,
                             QueryUpdateService qus, String settingsFileName, String tableName) throws Exception
    {
        if (dataWithoutDuplicates.size() > 0)
        {
            assert qus != null;
            List<Map<String,Object>> insertedRows;
            try
            {
                insertedRows = qus.insertRows(user, container, dataWithoutDuplicates, errors, null, null);
            }
            catch (DuplicateKeyException | BatchValidationException | QueryUpdateServiceException | SQLException e)
            {
                throw new Exception("Data from " + settingsFileName + " did not get imported into targetedms." + tableName, e);
            }

            return insertedRows.size();
        }
        return 0;
    }
}
