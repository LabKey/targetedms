/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.targetedms.datasource;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.UnexpectedException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

class MsDataFileSource extends MsDataSource
{
    MsDataFileSource(String instrument, List<String> extensions)
    {
        super(instrument, extensions);
    }

    MsDataFileSource(String instrument, String extension)
    {
        super(instrument, Collections.singletonList(extension));
    }

    public MsDataFileSource(List<String> extensions)
    {
        super("Unknown", extensions);
    }

    @Override
    public boolean isFileSource()
    {
        return true;
    }

    @Override
    boolean isValidPath(@NotNull Path path)
    {
        if(Files.exists(path) && !Files.isDirectory(path))
        {
            return !isZip(FileUtil.getFileName(path)) || isValidZip(path);
        }
        return false;
    }

    private boolean isValidZip(@NotNull Path path)
    {
        try
        {
            for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                    .getRootDirectories())
            {
                String basename = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                Path file = root.resolve(basename);
                // Make sure that a file with the name exists in the zip archive
                if(Files.exists(file) && !Files.isDirectory(file))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            throw UnexpectedException.wrap(e, "Error validating zip source for " + name() + ". Path: " + path);
        }
        return false;
    }

    @Override
    boolean isValidData(@NotNull ExpData expData, ExperimentService expSvc)
    {
        return isNotDirInExpData(expData, expSvc);
    }

    static boolean isNotDirInExpData(@NotNull ExpData expData, ExperimentService expSvc)
    {
        String pathPrefix = expData.getDataFileUrl();
        TableInfo expDataTInfo = expSvc.getTinfoData();
        // Instead of a !Files.isDirectory() check, we will look for any rows in exp.data where the dataFileUrl starts with the pathPrefix
        // Example: if dataFileUrl for given ExpData is file://folder/thermo_file.raw
        //          we shouldn't find any rows where the dataFileUrl is like file://folder/thermo_file.raw/file.txt
        return !(new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"),
                MsDataSource.getExpDataFilter(expData.getContainer(), pathPrefix), null).exists());
    }
}
