/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.targetedms.folderImport;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.admin.AbstractFolderImportFactory;
import org.labkey.api.admin.FolderImportContext;
import org.labkey.api.admin.FolderImporter;
import org.labkey.api.admin.ImportException;
import org.labkey.api.data.TableInfo;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.writer.VirtualFile;
import org.labkey.targetedms.TargetedMSSchema;

import java.io.IOException;
import java.util.List;

public class QCFolderImporter implements FolderImporter
{
    @Override
    public String getDataType()
    {
        return TargetedMSService.QC_FOLDER_DATA_TYPE;
    }

    @Override
    public String getDescription()
    {
        return TargetedMSService.QC_FOLDER_DATA_TYPE;
    }

    @Override
    public void process(@Nullable PipelineJob job, FolderImportContext ctx, VirtualFile root) throws Exception
    {
        //if 'PanoramaQC' folder is present in the archive
        if (!root.listDirs().isEmpty() && root.listDirs().stream().anyMatch(name -> name.equalsIgnoreCase(QCFolderConstants.QC_FOLDER_DIR)))
        {
            VirtualFile panoramaQCDir = root.getDir(QCFolderConstants.QC_FOLDER_DIR);
            List<String> filesToImport = root.getDir(QCFolderConstants.QC_FOLDER_DIR).list();

            // if 'PanoramaQC' folder has files to import
            if (!filesToImport.isEmpty())
            {
                if (null != job)
                {
                    job.setStatus("IMPORT " + getDescription());
                }

                TargetedMSSchema schema = new TargetedMSSchema(ctx.getUser(), ctx.getContainer());

                //iterate through PanoramaQCSettings enum values so that files get imported in that order/ordinal, since the lookup tables need to get populated first
                for (PanoramaQCSettings qcSetting : PanoramaQCSettings.values())
                {
                    if (filesToImport.stream().filter(f -> f.equalsIgnoreCase(qcSetting.getSettingsFileName())).count() == 1)
                    {
                        try
                        {
                            long numRows;

                            if (qcSetting.getSettingsFileName().equalsIgnoreCase(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME))
                            {
                                ctx.getLogger().info("Starting QC Plot settings import");
                                numRows = qcSetting.importSettingsFromFile(ctx, panoramaQCDir, null, null, null, null);
                                ctx.getLogger().info("Finished importing {} QC Plot settings from {} as properties.", numRows, qcSetting.getSettingsFileName());
                            }
                            else
                            {
                                TableInfo ti = qcSetting.getTableInfo(ctx.getUser(), ctx.getContainer(), null);
                                QueryUpdateService qus = ti.getUpdateService();
                                BatchValidationException errors = new BatchValidationException();

                                ctx.getLogger().info("Starting data import from {} into targetedms.{}", qcSetting.getSettingsFileName(), qcSetting.getTableName());
                                numRows = qcSetting.importSettingsFromFile(ctx, panoramaQCDir, schema, ti, qus, errors);
                                ctx.getLogger().info("Finished importing {} rows from {} into targetedms.{}", numRows, qcSetting.getSettingsFileName(), qcSetting.getTableName());
                            }
                        }
                        catch (IOException e)
                        {
                            if (qcSetting.getSettingsFileName().equalsIgnoreCase(QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME))
                            {
                                throw new ImportException("Error importing QC Plot settings from " + QCFolderConstants.QC_PLOT_SETTINGS_PROPS_FILE_NAME + ": " + e.getMessage(), e);
                            }
                            else
                            {
                                throw new ImportException("Error importing panorama qc settings from " + qcSetting.getSettingsFileName() + " into targetedms." + qcSetting.getTableName() + ": " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    public static class Factory extends AbstractFolderImportFactory
    {
        @Override
        public FolderImporter create()
        {
            return new QCFolderImporter();
        }

        @Override
        public int getPriority()
        {
            return 100; //this ensures skyline files from 'xar\experiments_and_runs\Runs' gets imported first, then the QC settings
        }
    }
}
