package org.labkey.targetedms.folderImport;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.admin.FolderImportContext;
import org.labkey.api.admin.ImportException;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.Results;
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
import org.labkey.api.writer.VirtualFile;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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
                public void exportSettings(VirtualFile vf, Container container, User user) throws Exception
                {
                    TableInfo ti = getTableInfo(user, container, null);
                    List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ColumnInfo::isUserEditable).collect(Collectors.toList());
                    SimpleFilter filter = SimpleFilter.createContainerFilter(container); //only export the ones that are defined in current container (and not the ones from the root container)

                    try(Results results = QueryService.get().select(ti, userEditableCols, filter, null))
                    {
                        exportSettingsToTSV(vf, results, getSettingsFileName(), getTableName());
                    }
                }
            },
    QC_ENABLED_METRICS (TargetedMSSchema.TABLE_QC_ENABLED_METRICS, QCFolderConstants.QC_ENABLED_METRICS_FILE_NAME)
            {
                @Override
                public void exportSettings(VirtualFile vf, Container container, User user) throws Exception
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = getTableInfo(user, container, null);
                    SQLFragment sql = new SQLFragment("SELECT qcMetricConfig.Name AS metric, qcEnabledMetrics.enabled, qcEnabledMetrics.lowerBound, qcEnabledMetrics.upperBound, qcEnabledMetrics.cusumLimit FROM ")
                            .append(ti, "qcEnabledMetrics")
                            .append(" INNER JOIN ")
                            .append(TargetedMSManager.getTableInfoQCMetricConfiguration(), "qcMetricConfig")
                            .append(" ON qcEnabledMetrics.metric = qcMetricConfig.Id");

                    try (Results results = QueryService.get().selectResults(schema, sql.getSQL(), null, null, true, true))
                    {
                        exportSettingsToTSV(vf, results, getSettingsFileName(), getTableName());
                    }
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    List<Map<String, Object>> tsvData = getTsvData(panoramaQCDir, getSettingsFileName());
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> {
                        String metricValue = (String) row.get("metric");
                        Integer metricId = getRowIdFromName(metricValue, getSettingsFileName(), getTableName(), TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Collections.singleton("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricValue), ctx.getUser(), ctx.getContainer(), null);
                        row.put("metric", metricId);
                        getDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates);
                    });
                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    QC_ANNOTATION_TYPE (TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, QCFolderConstants.QC_ANNOTATION_TYPE)
            {
                @Override
                public void exportSettings(VirtualFile vf, Container container, User user) throws Exception
                {
                    ContainerFilter cf = ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), container, user);
                    TableInfo ti = getTableInfo(user, container, cf);
                    List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ColumnInfo::isUserEditable).collect(Collectors.toList());

                    try (Results results = QueryService.get().select(ti, userEditableCols, null, null))
                    {
                        exportSettingsToTSV(vf, results, getSettingsFileName(), getTableName());
                    }
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    TableInfo tinfo = getTableInfo(ctx.getUser(), ctx.getContainer(), ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), ctx.getContainer(), ctx.getUser()));
                    return importSettings(ctx, tinfo, getSettingsFileName(), panoramaQCDir, errors, qus);
                }
            },
    QC_ANNOTATION (TargetedMSSchema.TABLE_QC_ANNOTATION, QCFolderConstants.QC_ANNOTATION_FILE_NAME)
            {
                @Override
                public void exportSettings(VirtualFile vf, Container container, User user) throws Exception
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = getTableInfo(user, container, null);
                    SQLFragment sql = new SQLFragment("SELECT qcAnnotationType.Name AS QCAnnotationTypeId, qcAnnotation.Description, qcAnnotation.Date FROM ")
                            .append(ti, "qcAnnotation")
                            .append(" INNER JOIN ")
                            .append(" (SELECT * FROM targetedms.qcAnnotationType ")
                            .append(" UNION ")
                            .append(" SELECT * FROM \"Shared\".targetedms.qcannotationtype) qcAnnotationType")
                            .append(" ON qcAnnotation.QCAnnotationTypeId = qcAnnotationType.Id");

                    try (Results results = QueryService.get().selectResults(schema, sql.getSQL(), null, null, true, true))
                    {
                        exportSettingsToTSV(vf, results, getSettingsFileName(), getTableName());
                    }
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    List<Map<String, Object>> tsvData = getTsvData(panoramaQCDir, getSettingsFileName());
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();
                    ContainerFilter cf = ContainerFilter.getContainerFilterByName(ContainerFilter.Type.CurrentPlusProjectAndShared.name(), ctx.getContainer(), ctx.getUser());

                    tsvData.forEach(row -> {
                        String nameValue = (String) row.get("QCAnnotationTypeId");
                        Integer qcAnnotationTypeId = getRowIdFromName(nameValue, getSettingsFileName(), getTableName(), TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, Collections.singleton("Id"), new SimpleFilter(FieldKey.fromParts("Name"), nameValue), ctx.getUser(), ctx.getContainer(), cf);
                        row.put("QCAnnotationTypeId", qcAnnotationTypeId);
                        getDataWithoutDuplicates(ctx, ti, row, dataWithoutDuplicates);
                    });

                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    QC_METRIC_EXCLUSION (TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION, QCFolderConstants.QC_METRIC_EXCLUSION_FILE_NAME)
            {
                @Override
                public void exportSettings(VirtualFile vf, Container container, User user) throws Exception
                {
                    TargetedMSSchema schema = new TargetedMSSchema(user, container);
                    TableInfo ti = getTableInfo(user, container, null);
                    SQLFragment sql = new SQLFragment("SELECT replicate.Name AS ReplicateId, runs.FileName AS File, qcMetricConfig.Name AS MetricId FROM ")
                            .append(ti, "qcMetricExclusion")
                            .append(" LEFT JOIN ")
                            .append(TargetedMSManager.getTableInfoReplicate(), "replicate")
                            .append(" ON qcMetricExclusion.ReplicateId = replicate.Id")
                            .append(" LEFT JOIN ")
                            .append(TargetedMSManager.getTableInfoQCMetricConfiguration(), "qcMetricConfig")
                            .append(" ON qcMetricExclusion.MetricId = qcMetricConfig.Id")
                            .append(" INNER JOIN ")
                            .append(TargetedMSManager.getTableInfoRuns(), "runs")
                            .append(" ON replicate.RunId = runs.Id");

                    try (Results results = QueryService.get().selectResults(schema, sql.getSQL(), null, null, true, true))
                    {
                        exportSettingsToTSV(vf, results, getSettingsFileName(), getTableName());
                    }
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws Exception
                {
                    List<Map<String, Object>> tsvData = getTsvData(panoramaQCDir, getSettingsFileName());
                    List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

                    tsvData.forEach(row -> {
                        String metricName = (String) row.get("MetricId");
                        Integer metricId = null;
                        if (StringUtils.isNotBlank(metricName)) //Metric is not a required field and can be null
                        {
                            metricId = getRowIdFromName(metricName, getSettingsFileName(), getTableName(), TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Collections.singleton("Id"), new SimpleFilter(FieldKey.fromParts("Name"), metricName), ctx.getUser(), ctx.getContainer(), null);
                        }
                        row.put("MetricId", metricId);
                        getReplicateDataWithoutDuplicates(getSettingsFileName(), getTableName(), ctx, ti, row, dataWithoutDuplicates);
                    });
                    return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
                }
            },
    QC_PLOT_SETTINGS (null, QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME)
            {
                @Override
                public void exportSettings(VirtualFile vf, Container container, User user) throws IOException
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

                    // Get the saved defaults for the container
                    Map<String, String> plotSettings = PropertyManager.getProperties(container, QCFolderConstants.CATEGORY);

                    // Don't bother saving if there aren't any default settings
                    if (!plotSettings.isEmpty())
                    {
                        try (PrintWriter out = vf.getPrintWriter(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME))
                        {

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
                }

                @Override
                public int importSettingsFromFile(FolderImportContext ctx, VirtualFile panoramaQCDir, @Nullable TargetedMSSchema schema, @Nullable TableInfo ti, @Nullable QueryUpdateService qus, @Nullable BatchValidationException errors) throws IOException
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
                                        Integer metricRowId = getRowIdFromName(entry.getValue().toString(), getSettingsFileName(), null, TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, Collections.singleton("Id"), new SimpleFilter(FieldKey.fromParts("Name"), entry.getValue().toString()), ctx.getUser(), ctx.getContainer(), null);
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

    public @NotNull TableInfo getTableInfo(User user, Container container, @Nullable ContainerFilter cf)
    {
        return getTableInfo(user, container, getTableName(), cf);
    }

    public @NotNull TableInfo getTableInfo(User user, Container container, String tableName, @Nullable ContainerFilter cf)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo ti = schema.getTable(tableName, cf);
        if (null == ti)
            throw new IllegalArgumentException(schema.getSchemaName() + "." + getTableName() + " not found in '" + container.getPath() + "'");
        return ti;
    }

    public void exportSettings(VirtualFile vf, Container container, User user) throws Exception
    {
        TableInfo ti = getTableInfo(user, container, null);
        List<ColumnInfo> userEditableCols = ti.getColumns().stream().filter(ColumnInfo::isUserEditable).collect(Collectors.toList());

        try (Results results = QueryService.get().select(ti, userEditableCols, null, null))
        {
            exportSettingsToTSV(vf, results, getSettingsFileName(), getTableName());
        }
    }

    protected void exportSettingsToTSV(VirtualFile vf, Results results, String fileName, String tableName) throws Exception
    {
        try
        {
            if (results.countAll() > 0)

                try (TSVGridWriter tsvWriter = new TSVGridWriter(()-> results))
                {
                    tsvWriter.setApplyFormats(false);
                    tsvWriter.setColumnHeaderType(ColumnHeaderType.FieldKey);
                    try (PrintWriter out = vf.getPrintWriter(fileName))
                    {
                        tsvWriter.write(out);
                    }
                }
        }
        catch (IOException | SQLException e)
        {
            throw new ImportException("Error getting results from " + tableName, e);
        }
    }

    protected String getMetricNameFromRowId(User user, Container c, Integer rowId)
    {
        TableInfo ti = getTableInfo(user, c, TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, null);
        String value = new TableSelector(ti, Collections.singleton("Name"), new SimpleFilter(FieldKey.fromParts("Id"), rowId), null).getObject(String.class);
        if (null == value)
        {
            throw new IllegalArgumentException("Id with value '" + rowId + "' not found in " + TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION + ". Unable to export QC properties.");
        }
        return value;
    }

    protected Integer getRowIdFromName(String name, String fileName, String targetTable, String lookupTableName, Set<String> colNames, SimpleFilter filter, User user, Container c, ContainerFilter cf)
    {
        TableInfo ti = getTableInfo(user, c, lookupTableName, cf);
        Integer value = new TableSelector(ti, colNames, filter, null).getObject(Integer.class);
        if (null == value)
        {
            String msg = "Error resolving '" + name + "' to its corresponding Id. Id not found in lookup table targetedms." + lookupTableName + ". ";
            if (null == targetTable)
            {
                msg += "Unable to save QC Properties from " + fileName + ".";
            }
            else
                msg += "Unable to import data from " + fileName + " into targetedms." + targetTable;

            throw new IllegalArgumentException(msg);
        }
        return value;
    }

    protected void getReplicateDataWithoutDuplicates(String settingsFileName, String targetTable, FolderImportContext ctx, TableInfo ti, Map<String, Object> row, List<Map<String, Object>> dataWithoutDuplicates)
    {
        String replicateName = (String) row.get("ReplicateId");
        String file = (String) row.get("File");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Name"), replicateName);
        filter.addCondition(FieldKey.fromString("RunId/FileName"), file);
        Integer replicateId = getRowIdFromName(replicateName, settingsFileName, targetTable, TargetedMSSchema.TABLE_REPLICATE, Collections.singleton("Id"), filter, ctx.getUser(), ctx.getContainer(), null);
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
            {
                // Without this check, getting an error if there is a mismatch between a Text column type and value (Ex. if col type is a varchar and value is numeric):
                // ERROR: ExecutingSelector; bad SQL grammar []; nested exception is org.postgresql.util.PSQLException:
                // ERROR: operator does not exist: character varying = integer
                if (ti.getColumn(col).getJdbcType().isText() && !(row.get(col) instanceof String))
                    filter.addCondition(FieldKey.fromParts(col), "'" + row.get(col) + "'");
                else
                    filter.addCondition(FieldKey.fromParts(col), row.get(col));
            }

            logMsg += col + ": '" + row.get(col) + "'" + (count == row.size()-2 ? "" : ", ") ;
            ++count;
        }

        if (new TableSelector(ti, filter, null).exists())
        {
            logMsg += "] values already exists. Skipping";
            ctx.getLogger().info(logMsg);
        }
        else
        {
            dataWithoutDuplicates.add(row);
        }
    }

    protected void getDataWithoutDuplicates(FolderImportContext ctx, TableInfo ti, Map<String, Object> row, List<Map<String, Object>> dataWithoutDuplicates)
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
                // Without this check, getting an error if there is a mismatch between a Text column type and value (Ex. if col type is a varchar and value is numeric):
                // ERROR: ExecutingSelector; bad SQL grammar []; nested exception is org.postgresql.util.PSQLException:
                // ERROR: operator does not exist: character varying = integer
                if (ti.getColumn(col).getJdbcType().isText() && !(row.get(col) instanceof String))
                    filter.addCondition(FieldKey.fromParts(col), "'" + row.get(col) + "'");
                else
                    filter.addCondition(FieldKey.fromParts(col), row.get(col));
            }
            logMsg += col + ": '" + row.get(col) + "'" + (count == row.size()-1 ? "" : ", ");
            ++count;
        }
        if (new TableSelector(ti, row.keySet(), filter, null).getRowCount() > 0)
        {
            logMsg += "] values already exists. Skipping";
            ctx.getLogger().info(logMsg);
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
        List<Map<String, Object>> tsvData = getTsvData(panoramaQCDir, settingsFileName);
        List<Map<String, Object>> dataWithoutDuplicates = new ArrayList<>();

        tsvData.forEach(row -> getDataWithoutDuplicates(ctx, tinfo, row, dataWithoutDuplicates));
        return insertData(ctx.getUser(), ctx.getContainer(), dataWithoutDuplicates, errors, qus, getSettingsFileName(), getTableName());
    }

    protected List<Map<String, Object>> getTsvData(VirtualFile panoramaQCDir, String settingsFile) throws IOException
    {
        try (BufferedReader reader = Readers.getReader(panoramaQCDir.getInputStream(settingsFile)); DataLoader loader = new TabLoader(reader, true))
        {
            return loader.load();
        }
    }

    protected int insertData(User user, Container container, List<Map<String, Object>> dataWithoutDuplicates, BatchValidationException errors,
                             QueryUpdateService qus, String settingsFileName, String tableName) throws Exception
    {
        if (dataWithoutDuplicates.size() > 0)
        {
            if (qus != null)
            {
                List<Map<String,Object>> insertedRows;
                try
                {
                    insertedRows = qus.insertRows(user, container, dataWithoutDuplicates, errors, null, null);
                }
                catch (DuplicateKeyException | BatchValidationException | QueryUpdateServiceException | SQLException e)
                {
                    throw new ImportException("Data from " + settingsFileName + " did not get imported into targetedms." + tableName, e);
                }
                return insertedRows.size();
            }
            else
            {
                throw new IllegalArgumentException("Query update service for targetedms." + tableName + " not found.");
            }
        }
        return 0;
    }
}
