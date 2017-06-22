/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

package org.labkey.targetedms;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.ZipUtil;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSDataHandler extends AbstractExperimentDataHandler
{
    private static final Logger _log = Logger.getLogger(TargetedMSDataHandler.class);

    @Override
    public DataType getDataType()
    {
        return null;
    }

    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        String description = data.getFile().getName();
        SkylineDocImporter importer = new SkylineDocImporter(info.getUser(), context.getContainer(), description,
                                                             data, log, context, TargetedMSRun.RepresentativeDataState.NotRepresentative);
        try
        {
            SkylineDocImporter.RunInfo runInfo = importer.prepareRun();
            TargetedMSRun run = importer.importRun(runInfo);

            ExpRun expRun = data.getRun();
            if(expRun == null)
            {
                // At this point expRun should not be null
                throw new ExperimentException("ExpRun was null. An entry in the ExperimentRun table should already exist for this data.");
            }
            run.setExperimentRunLSID(expRun.getLSID());

            TargetedMSManager.updateRun(run, info.getUser());
        }
        catch (IOException | DataFormatException | XMLStreamException | PipelineJobException e)
        {
            throw new ExperimentException(e);
        }
    }

    @Override
    public ActionURL getContentURL(ExpData data)
    {
        TargetedMSRun run = TargetedMSManager.getRunByDataId(data.getRowId(), data.getContainer());
        if (run != null)
        {
            return TargetedMSController.getShowRunURL(data.getContainer(), run.getRunId());
        }
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        TargetedMSRun run = TargetedMSManager.getRunByDataId(data.getRowId(), container);
        if (run != null)
        {
            deleteRun(container, user, run);
        }
        data.delete(user);
    }

    private void deleteRun(Container container, User user, TargetedMSRun run)
    {
        TargetedMSManager.markDeleted(Arrays.asList(run.getRunId()), container, user);
        TargetedMSManager.purgeDeletedRuns();
        TargetedMSManager.deleteiRTscales(container);
    }

    @Override
    public void beforeDeleteData(List<ExpData> data) throws ExperimentException
    {
        for (ExpData expData : data)
        {
            Container container = expData.getContainer();
            TargetedMSRun run = TargetedMSManager.getRunByDataId(expData.getRowId(), container);
            // r26691 (jeckels@labkey.com  5/30/2013 1:28:42 PM) -- Fix container delete when a TargetedMS run failed to import correctly.
            //        -- FK violation happens if a container with failed targeted ms run imports is deleted.
            //
            // Issue #21935 Add support to move targeted MS runs between folders
            // beforeDeleteData() is called prior to actual deletion of data. It is also called when moving a run
            // to another container: ExperimentServiceImpl.deleteExperimentRunForMove() -> deleteRun() -> deleteProtocolApplications()
            // Do not delete the run if it was successfully imported.  If the run was successfully imported
            // it will be deleted via the deleteData() method, in the case of actual run deletion.
            if (run != null && run.getStatusId() != SkylineDocImporter.STATUS_SUCCESS)
            {
                deleteRun(container, null, run);
            }
        }
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        TargetedMSModule.FolderType sourceFolderType = TargetedMSManager.getFolderType(container);
        TargetedMSModule.FolderType targetFolderType = TargetedMSManager.getFolderType(targetContainer);

        if(sourceFolderType != TargetedMSModule.FolderType.Experiment || targetFolderType != TargetedMSModule.FolderType.Experiment)
        {
            StringBuilder error = new StringBuilder();
            if(sourceFolderType != TargetedMSModule.FolderType.Experiment)
            {
                error.append("Source folder \"").append(container.getPath()).append("\" is a \"").append(sourceFolderType.name()).append("\" folder. ");
            }
            if(targetFolderType != TargetedMSModule.FolderType.Experiment)
            {
                error.append("Target folder \"").append(targetContainer.getPath()).append("\" is a \"").append(targetFolderType.name()).append("\" folder. ");
            }
            error.append("Runs can only be moved between \"Experimental Data\" folders. For other folder types please delete the run in the source folder and import it in the target folder.");
            throw new ExperimentException(error.toString());
        }

        File sourceFile = newData.getFile();

        // Fail if a Skyline document with the same source file name exists in the new location.
        if(skylineDocExistsInTarget(sourceFile.getName(), targetContainer))
        {
            throw new ExperimentException("A run with filename " + sourceFile.getName() + " already exists in the target container " + targetContainer.getPath());
        }

        TargetedMSRun run = TargetedMSManager.getRunByLsid(oldRunLSID, container);
        if(run == null)
        {
            throw new ExperimentException("Run with lsid " + oldRunLSID + " was not found in container " + container.getPath());
        }

        NetworkDrive.ensureDrive(sourceFile.getPath());

        PipeRoot targetRoot = PipelineService.get().findPipelineRoot(targetContainer);
        if(targetRoot == null)
        {
            throw new ExperimentException("Could not find pipeline root for target container " + targetContainer.getPath());
        }

        String srcFileExt = FileUtil.getExtension(sourceFile.getName());

        if(SkylineFileUtils.EXT_ZIP.equalsIgnoreCase(srcFileExt))
        {
            // Copy the source Skyline zip file to the new location.
            File destFile = new File(targetRoot.getRootPath(), sourceFile.getName());
            // It is only meaningful to copy the file it is a shared zip file that may contain spectrum and/or irt libraries.
            // When rendering the MS/MS spectrum we read scan peaks directly from the .blib (spectrum library) files.
            // The contents of these files are not stored in the database.
            // TODO: Should we only allow import of shared zip files?  If a user uploads and imports a .sky file they have to upload
            //       the .skyd file and any .blib files as well to the same folder. This use case must be quite rare.

            // When using a pipeline root, it's possible that both the source and destination containers are pointing
            // at the same file system
            if (!sourceFile.equals(destFile))
            {
                try
                {
                    FileUtils.copyFile(sourceFile, destFile);
                }
                catch (IOException e)
                {
                    throw new ExperimentException("Could not copy " + sourceFile + " to destination directory " + targetRoot.getRootPath());
                }
            }

            // Expand the zip file in the new location
            File zipDir = new File(destFile.getParent(), SkylineFileUtils.getBaseName(destFile.getName()));
            try
            {
                ZipUtil.unzipToDirectory(destFile, zipDir, null);
            }
            catch (IOException e)
            {
                throw new ExperimentException("Unable to unzip file " + destFile.getPath(), e);
            }

            // Update the file path in ExpData
            newData.setDataFileURI(destFile.toURI());
            newData.save(user);
        }

        // Update the run
        TargetedMSManager.moveRun(run, targetContainer, newRunLSID, newData.getRowId(), user);

        // Delete the old entry in exp.data -- it is no longer linked to the run.
        ExpData oldData = ExperimentService.get().getExpData(oldDataRowID);
        if(oldData != null)
        {
            oldData.delete(user);
        }

        if(SkylineFileUtils.EXT_ZIP.equalsIgnoreCase(srcFileExt))
        {
            // Delete the Skyline file in the old location
            sourceFile.delete();

            // Delete the unzipped directory in the old location
            File oldZipDir = new File(sourceFile.getParent(), SkylineFileUtils.getBaseName(sourceFile.getName()));
            FileUtil.deleteDir(oldZipDir);
        }

    }

    private boolean skylineDocExistsInTarget(String fileName, Container targetContainer)
    {
        return TargetedMSManager.getRunByFileName(fileName, targetContainer) != null;
    }

    @Override
    public Priority getPriority(ExpData data)
    {
        String url = data.getDataFileUrl();
        if (url == null)
            return null;
        String ext = FileUtil.getExtension(url);
        if (ext == null)
            return null;
        ext = ext.toLowerCase();
        // we handle only *.sky or .zip files
        return "sky".equals(ext) ||
                ("zip".equals(ext) &&
                        (TargetedMSManager.getRunByDataId(data.getRowId(), data.getContainer()) != null ||
                        zipContainsSkyFile(data.getFile()))) ? Priority.HIGH : null;
    }

    /** @return true if the zip file contains a .sky file, which means that we can import it (after extracting) */
    private boolean zipContainsSkyFile(File f)
    {
        if (f == null || !f.exists())
        {
            return false;
        }

        try (ZipFile zipFile = new ZipFile(f))
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry zEntry = entries.nextElement();
                if ("sky".equalsIgnoreCase(FileUtil.getExtension(zEntry.getName())))
                {
                    return true;
                }
            }
        }
        catch (IOException | IllegalArgumentException e)
        {
            //ignore - see issue 29485 for IllegalArgumentException case
            _log.warn("Failed to open zip file " + f + " to check if it contains .sky files" + e.getMessage());
        }

        return false;
    }
}
