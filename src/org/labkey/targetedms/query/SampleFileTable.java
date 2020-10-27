/*
 * Copyright (c) 2015-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.query;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Link;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.PopupMenu;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.SampleFile;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleFileTable extends TargetedMSTable
{
    @Nullable
    private TargetedMSRun _run;

    public SampleFileTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        this(schema, cf, null);
    }

    public SampleFileTable(TargetedMSSchema schema, ContainerFilter cf, @Nullable TargetedMSRun run)
    {
        super(TargetedMSManager.getTableInfoSampleFile(), schema, cf, TargetedMSSchema.ContainerJoinType.ReplicateFK);

        _run = run;
        SQLFragment runIdSQL = new SQLFragment("(SELECT r.RunId FROM ").
                append(TargetedMSManager.getTableInfoReplicate(), "r").
                append(" WHERE r.Id = ").append(ExprColumn.STR_TABLE_ALIAS).append(".ReplicateId)");
        addColumn(new ExprColumn(this, "RunId", runIdSQL, JdbcType.INTEGER));

        if (_run != null)
        {
            addCondition(new SQLFragment("ReplicateId IN (SELECT Id FROM ").
                    append(TargetedMSManager.getTableInfoReplicate(), "r").
                    append(" WHERE RunId = ?)").add(run.getId()), FieldKey.fromParts("ReplicateId"));
        }

        SQLFragment excludedSQL = new SQLFragment("CASE WHEN ReplicateId IN (SELECT ReplicateId FROM ");
        excludedSQL.append(TargetedMSManager.getTableInfoQCMetricExclusion(), "x");
        excludedSQL.append(" WHERE x.MetricId IS NULL) THEN ? ELSE ? END");
        excludedSQL.add(true);
        excludedSQL.add(false);
        ExprColumn excludedColumn = new ExprColumn(this, "Excluded", excludedSQL, JdbcType.BOOLEAN);
        addColumn(excludedColumn);

        ActionURL url = new ActionURL(TargetedMSController.ShowSampleFileAction.class, getContainer());
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("id", "Id");
        DetailsURL detailsURL = new DetailsURL(url, urlParams);
        setDetailsURL(detailsURL);
        getMutableColumn("ReplicateId").setURL(detailsURL);

        SqlDialect dialect = TargetedMSManager.getSqlDialect();
        SQLFragment filePathColSql = new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FilePath");

        SQLFragment filePathLengthSql = new SQLFragment(dialect.getVarcharLengthFunction()).append("(").append(filePathColSql).append(")");
        SQLFragment fileNameSql = new SQLFragment("CASE")
                .append(" WHEN ").append(filePathColSql).append(" IS NOT NULL AND ").append(dialect.getStringIndexOfFunction(new SQLFragment("'\\'"), filePathColSql)).append(" > 0")
                .append(" THEN ").append(dialect.getSubstringFunction(
                                                filePathColSql,
                                                new SQLFragment(filePathLengthSql).append(" - ")
                                                               .append(dialect.getStringIndexOfFunction(new SQLFragment("'\\'"), new SQLFragment(" REVERSE(").append(filePathColSql)).append(") + 2 ")),
                                                filePathLengthSql))
                .append(" ELSE ").append(filePathColSql).append(" END");
        ExprColumn fileNameCol = new ExprColumn(this, "File", fileNameSql, JdbcType.VARCHAR);
        addColumn(fileNameCol);

        var downloadCol = addWrapColumn("Download", getRealTable().getColumn("Id"));
        downloadCol.setKeyField(false);
        downloadCol.setTextAlign("left");
        downloadCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DataColumn(colInfo)
                {
                    @Override
                    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                    {
                        Long sampleFileId = ctx.get(this.getColumnInfo().getFieldKey(), Long.class);
                        if (sampleFileId != null)
                        {
                            SampleFile sampleFile = ReplicateManager.getSampleFile(sampleFileId);

                            if(sampleFile != null)
                            {
//                                String webdavUrl = fileAvailable(sampleFile);
//                                if(webdavUrl != null)
//                                {
//                                    out.write("<nobr>&nbsp;");
//                                    out.write(new Link.LinkBuilder("Download").iconCls("fa fa-download").href(webdavUrl).toString());
//                                    out.write("&nbsp;");
//                                    out.write("</nobr>");
//                                    return;
//                                }
                            }
                            out.write("<em>Not available</em>");
                        }
                    }

                    @Override
                    public void addQueryFieldKeys(Set<FieldKey> keys)
                    {
//                        FieldKey parentFK = this.getColumnInfo().getFieldKey().getParent();
//                        keys.add(new FieldKey(parentFK, "ExperimentRunLSID"));
//                        keys.add(FieldKey.fromParts("Folder"));
                        super.addQueryFieldKeys(keys);
                    }
                };
            }
        });


    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        if (_defaultVisibleColumns == null && _run != null)
        {
            // Always include these columns
            List<FieldKey> defaultCols = new ArrayList<>(Arrays.asList(
                    FieldKey.fromParts("ReplicateId"),
                    FieldKey.fromParts("File"),
                    FieldKey.fromParts("Download"),
                    // FieldKey.fromParts("FilePath"),
                    FieldKey.fromParts("AcquiredTime")));

            // Find the columns that have values for the run of interest, and include them in the set of columns in the default
            // view. We don't really care what the value is for the columns, just that it exists, so we arbitrarily use
            // MAX as the aggregate.
            List<Aggregate> aggregates = new ArrayList<>();
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "CePredictorId"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "DpPredictorId"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "SampleType"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "AnalyteConcentration"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "SampleDilutionFactor"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("InstrumentId"), Aggregate.BaseType.MAX));

            // Also search for values for any replicate annotations being used in this container
            for (AnnotatedTargetedMSTable.AnnotationSettingForTyping annotation : AnnotatedTargetedMSTable.getAnnotationSettings("replicate", getUserSchema(), ContainerFilter.current(getUserSchema().getContainer())))
            {
                aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", annotation.getName()), Aggregate.BaseType.MAX));
            }

            TableSelector ts = new TableSelector(this);
            Map<String, List<Aggregate.Result>> aggValues = ts.getAggregates(aggregates);

            for (Aggregate aggregate: aggregates)
            {
                List<Aggregate.Result> result = aggValues.get(aggregate.getFieldKey().toString());
                if(result != null)
                {
                    Aggregate.Result aggResult = result.get(0);
                    if (aggResult.getValue() != null)
                    {
                        defaultCols.add(aggResult.getAggregate().getFieldKey());
                    }
                }
            }
            // setDefaultVisibleColumns() will call checkLocked(), however this is just being done lazily for perf (I think) so should be legal
            _defaultVisibleColumns = defaultCols;
        }

        return super.getDefaultVisibleColumns();
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // only allow delete of targetedms.SampleFile table for QC folder type
        TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(getContainer());
        boolean allowDelete = folderType == TargetedMSService.FolderType.QC && DeletePermission.class.equals(perm);

        return (ReadPermission.class.equals(perm) || allowDelete) && getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            protected Map<String, Object> deleteRow(User user, Container container, Map<String, Object> oldRowMap) throws QueryUpdateServiceException, SQLException, InvalidKeyException
            {
                // Need to cascade the delete
                Object id = oldRowMap.get("id");
                if (id != null)
                {
                    int convertedId = Integer.parseInt(id.toString());
                    TargetedMSManager.purgeDeletedSampleFiles(convertedId);
                }
                return super.deleteRow(user, container, oldRowMap);
            }
        };
    }

//    private String fileAvailable(SampleFile sampleFile)
//    {
//        String filePath = sampleFile.getFilePath();
//        String fileName = FilenameUtils.getName(filePath);
//        TargetedMSRun
//        if(hasEx)
//    }

//    private static void checkExists(String filePath)
//    {
//        String fileName = FilenameUtils.getName(filePath);
//        if (!hasExpData(fileName, run.getContainer(), rawFilesDir, expSvc, false))
//        {
//            // If no matching row was found in exp.data and this is NOT a cloud container check for the file on the file system.
//            if(!FileContentService.get().isCloudRoot(rootExpContainer))
//            {
//                if (Files.exists(rawFilesDir) && findInDirectoryTree(rawFilesDir, fileName, rootExpContainer))
//                {
//                    existingRawFiles.add(filePath);
//                    return;
//                }
//            }
//            missingFiles.add(filePath);
//        }
//        else
//        {
//            existingRawFiles.add(filePath);
//        }
//    }

    private static boolean hasExpData(String sampleFileName, Container container, Path rawFilesDir, ExperimentService svc, boolean allowBasenameOnly)
    {
        if(svc == null)
        {
            return false;
        }

        String nameNoExt = FileUtil.getBaseName(sampleFileName);

        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        // Look for files that start with the base filename of the sample file.
        filter.addCondition(FieldKey.fromParts("Name"), nameNoExt, CompareType.STARTS_WITH);
        // Look for data under @files/RawFiles.
        // Use FileUtil.pathToString(); this will remove the access ID is this is a S3 path
        String prefix = FileUtil.pathToString(rawFilesDir);
        if (!prefix.endsWith("/"))
        {
            prefix = prefix + "/";
        }
        filter.addCondition(FieldKey.fromParts("datafileurl"), prefix, CompareType.STARTS_WITH);

        List<String> files = new TableSelector(svc.getTinfoData(), Collections.singleton("Name"), filter, null).getArrayList(String.class);

        for (String expDataFile: files)
        {
            if(accept(sampleFileName, expDataFile, allowBasenameOnly))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean accept(String sampleFileName, String uploadedFileName)
    {
        return accept(sampleFileName, uploadedFileName, false);
    }

    private static boolean accept(String sampleFileName, String uploadedFileName, boolean allowBasenameOnly)
    {
        // Accept QC_10.9.17.raw OR for QC_10.9.17.raw.zip OR QC_10.9.17.zip
        // 170428_DBS_cal_7a.d OR 170428_DBS_cal_7a.d.zip OR 170428_DBS_cal_7a.zip
        String nameNoExt = FileUtil.getBaseName(sampleFileName);
        return sampleFileName.equalsIgnoreCase(uploadedFileName)
                || (sampleFileName + ".zip").equalsIgnoreCase(uploadedFileName)
                || (nameNoExt + ".zip").equalsIgnoreCase(uploadedFileName)
                || (allowBasenameOnly && nameNoExt.equalsIgnoreCase(FileUtil.getBaseName(uploadedFileName)));
    }
}
