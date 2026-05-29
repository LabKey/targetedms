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

import org.labkey.api.admin.BaseFolderWriter;
import org.labkey.api.admin.FolderExportContext;
import org.labkey.api.admin.FolderWriter;
import org.labkey.api.admin.FolderWriterFactory;
import org.labkey.api.data.Container;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.writer.VirtualFile;
import org.labkey.targetedms.TargetedMSManager;

public class QCFolderWriterFactory implements FolderWriterFactory
{
    @Override
    public FolderWriter create()
    {
        return new QCFolderWriter();
    }

    private static class QCFolderWriter extends BaseFolderWriter
    {
        @Override
        public String getDataType()
        {
            return TargetedMSService.QC_FOLDER_DATA_TYPE;
        }

        @Override
        public boolean show(Container c)
        {
            TargetedMSManager.getFolderType(c);
            TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(c);
            return folderType == TargetedMSService.FolderType.QC;
        }

        @Override
        public void write(Container object, FolderExportContext ctx, VirtualFile root) throws Exception
        {
            VirtualFile vf = root.getDir(QCFolderConstants.QC_FOLDER_DIR);

            for (PanoramaQCSettings setting : PanoramaQCSettings.values())
            {
                setting.exportSettings(vf, object, ctx.getUser());
            }
        }
    }


}
