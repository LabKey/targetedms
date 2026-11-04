/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.ResultsFactory;

import java.util.List;
import java.util.Map;

/**
 * Writes each peptide of a cross-linked peptide on its own sheet row. {@link PerPeptideColumn}s that split the current
 * row get one value per sheet row; all other columns are written once and merged across the rows.
 */
public class CrossLinkedPeptideExcelWriter extends ExcelWriter
{
    /** RenderContext key holding the 0-based index of the peptide being written, present only while splitting a row */
    public static final String PEPTIDE_INDEX = "~~crosslinked-peptide-index~~";

    /** A display column whose value has one entry per peptide in a cross-linked peptide */
    public interface PerPeptideColumn
    {
        int getPeptideCount(RenderContext ctx);

        /** @return false to write the current row's value once, merged across the peptides' rows */
        default boolean isSplit(RenderContext ctx)
        {
            return true;
        }
    }

    /** Longest line of text written to each column, in characters */
    private int[] _maxLineLengths;

    public CrossLinkedPeptideExcelWriter(ResultsFactory factory, List<DisplayColumn> displayColumns, ExcelDocumentType docType, Map<String, String> renameColumnMap)
    {
        super(factory, displayColumns, docType, renameColumnMap);
    }

    @Override
    protected void renderGridRow(Sheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws MaxRowsExceededException
    {
        int firstRow = getCurrentRow();
        try
        {
            renderPeptideRows(sheet, ctx, columns);
        }
        finally
        {
            // Measure now because the streaming workbook flushes rows to disk before adjustColumnWidths() runs
            trackLineLengths(sheet, columns, firstRow, getCurrentRow() - 1);
        }
    }

    private void trackLineLengths(Sheet sheet, List<ExcelColumn> columns, int firstRow, int lastRow)
    {
        if (_maxLineLengths == null)
        {
            _maxLineLengths = new int[columns.size()];
        }
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            for (int column = 0; row != null && column < _maxLineLengths.length; column++)
            {
                Cell cell = row.getCell(column);
                if (cell != null && cell.getCellType() == CellType.STRING)
                {
                    for (String line : cell.getStringCellValue().split("\n"))
                    {
                        _maxLineLengths[column] = Math.max(_maxLineLengths[column], line.length());
                    }
                }
            }
        }
    }

    /** POI's auto-sizing skips merged cells and measures with the server's fonts, which run narrower than Excel's */
    @Override
    public void adjustColumnWidths(RenderContext ctx, Sheet sheet, List<ExcelColumn> visibleColumns)
    {
        super.adjustColumnWidths(ctx, sheet, visibleColumns);
        for (int column = 0; _maxLineLengths != null && column < _maxLineLengths.length; column++)
        {
            // Pad for capitals, which Excel renders wider than its width unit, the digit '0'
            int width = Math.min(_maxLineLengths[column] + 2, 255) * 256;
            if (_maxLineLengths[column] > 0 && width > sheet.getColumnWidth(column))
            {
                sheet.setColumnWidth(column, width);
            }
        }
    }

    private void renderPeptideRows(Sheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws MaxRowsExceededException
    {
        boolean[] split = new boolean[columns.size()];
        int peptideCount = 1;
        for (int column = 0; column < columns.size(); column++)
        {
            if (columns.get(column).getDisplayColumn() instanceof PerPeptideColumn perPeptide && perPeptide.isSplit(ctx))
            {
                split[column] = true;
                peptideCount = Math.max(peptideCount, perPeptide.getPeptideCount(ctx));
            }
        }

        if (peptideCount <= 1)
        {
            super.renderGridRow(sheet, ctx, columns);
            return;
        }

        int firstRow = getCurrentRow();
        try
        {
            ctx.put(PEPTIDE_INDEX, 0);
            super.renderGridRow(sheet, ctx, columns);

            for (int peptideIndex = 1; peptideIndex < peptideCount; peptideIndex++)
            {
                ctx.put(PEPTIDE_INDEX, peptideIndex);
                for (int column = 0; column < columns.size(); column++)
                {
                    if (split[column])
                    {
                        columns.get(column).writeCell(sheet, column, getCurrentRow(), ctx);
                    }
                }
                incrementRow();
            }
        }
        finally
        {
            ctx.remove(PEPTIDE_INDEX);
            // Also merge the rows already written when MaxRowsExceededException cuts the cross-link short
            mergeSharedColumns(sheet, split, firstRow, getCurrentRow() - 1);
        }
    }

    private void mergeSharedColumns(Sheet sheet, boolean[] split, int firstRow, int lastRow)
    {
        if (lastRow <= firstRow)
        {
            return;
        }
        for (int column = 0; column < split.length; column++)
        {
            if (!split[column])
            {
                // Regions are disjoint by construction; the validating addMergedRegion is quadratic in the region count
                sheet.addMergedRegionUnsafe(new CellRangeAddress(firstRow, lastRow, column, column));
                CellUtil.setVerticalAlignment(sheet.getRow(firstRow).getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), VerticalAlignment.CENTER);
            }
        }
    }
}
