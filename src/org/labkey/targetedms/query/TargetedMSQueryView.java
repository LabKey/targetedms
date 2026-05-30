/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.targetedms.TargetedMSSchema;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.util.Map;

public class TargetedMSQueryView extends QueryView
{
    public TargetedMSQueryView(TargetedMSSchema schema, @NotNull QuerySettings settings, BindException errors)
    {
        super(schema, settings, errors);
    }

    /**
     * Ensures that the Excel export uses shared strings to support rich text formatting, as the expense of memory use during export
     */
    @Override
    public ExcelWriter getExcelWriter(ExcelWriter.ExcelDocumentType docType, @Nullable Map<String, String> renameColumnMap) throws IOException
    {
        if (docType == ExcelWriter.ExcelDocumentType.xlsx)
        {
            docType = ExcelWriter.ExcelDocumentType.xlsxSharedStrings;
        }
        return super.getExcelWriter(docType, renameColumnMap);
    }
}
