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
