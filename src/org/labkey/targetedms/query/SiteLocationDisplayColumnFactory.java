package org.labkey.targetedms.query;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;

import java.util.Map;
import java.util.Set;

/** For Excel export, output a chain abbreviation, a three-letter abbreviation for the amino acid, and the index as a superscript */
public class SiteLocationDisplayColumnFactory implements DisplayColumnFactory
{
    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new SiteLocationDisplayColumn(colInfo);
    }

    private static class ExcelColumnImpl extends ExcelColumn
    {
        private final SiteLocationDisplayColumn _col;
        private XSSFFont _superscriptFont;

        public ExcelColumnImpl(SiteLocationDisplayColumn col, Map<ExcelFormatDescriptor, CellStyle> formatters, Workbook workbook)
        {
            super(col, formatters, workbook);
            _col = col;
        }

        @Override
        public void writeCell(Sheet sheet, int column, int row, RenderContext ctx)
        {
            Object o = _col.getExcelCompatibleValue(ctx);

            if (o instanceof String s && s.length() >= 2 &&
                    (s.charAt(0) >= 'A' && s.charAt(0) <= 'Z') &&
                    (s.charAt(1) >= '0' && s.charAt(1) <= '9'))
            {
                String aa = s.substring(0, 1);
                aa = switch (aa)
                {
                    case "G" ->	"Gly";
                    case "A" ->	"Ala";
                    case "V" ->	"Val";
                    case "L" ->	"Leu";
                    case "I" ->	"Ile";
                    case "T" ->	"Thr";
                    case "S" ->	"Ser";
                    case "M" ->	"Met";
                    case "C" ->	"Cys";
                    case "P" ->	"Pro";
                    case "F" ->	"Phe";
                    case "Y" ->	"Tyr";
                    case "W" ->	"Trp";
                    case "H" ->	"His";
                    case "K" ->	"Lys";
                    case "R" ->	"Arg";
                    case "D" ->	"Asp";
                    case "E" ->	"Glu";
                    case "N" ->	"Asn";
                    case "Q" ->	"Gln";
                    default -> aa;
                };

                String chain = _col.getChainLabel(ctx);
                String chainPrefix = CrossLinkedPeptideDisplayColumn.getChainPrefix(chain);
                if (!chainPrefix.isEmpty())
                {
                    chainPrefix = chainPrefix + " ";
                }

                String offset = s.substring(1);
                String value = chainPrefix + aa + offset;

                Row rowObject = getRow(sheet, row);
                Cell cell = rowObject.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(value);

                if (sheet instanceof SXSSFSheet)
                {
                    RichTextString richText = cell.getRichStringCellValue();
                    if (_superscriptFont == null)
                    {
                        _superscriptFont = (XSSFFont) _workbook.createFont();
                        _superscriptFont.setTypeOffset(Font.SS_SUPER);
                    }
                    richText.applyFont(chainPrefix.length() + aa.length(), value.length(), _superscriptFont);
                    cell.setCellValue(richText);
                }
            }
            else
            {
                super.writeCell(sheet, column, row, ctx);
            }
        }
    }

    private static class SiteLocationDisplayColumn extends DataColumn
    {
        public SiteLocationDisplayColumn(ColumnInfo colInfo)
        {
            super(colInfo);
        }

        @Override
        public ExcelColumn createExcelColumn(Map<ExcelColumn.ExcelFormatDescriptor, CellStyle> formatters, Workbook workbook)
        {
            return new ExcelColumnImpl(this, formatters, workbook);
        }

        private String getChainLabel(RenderContext ctx)
        {
            return ctx.get(getChainLabelFieldKey(), String.class);
        }

        private FieldKey getChainLabelFieldKey()
        {
            return new FieldKey(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId").append("Label");
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(getChainLabelFieldKey());
        }
    }
}
