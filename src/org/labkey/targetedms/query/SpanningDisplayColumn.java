package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

public class SpanningDisplayColumn extends DataColumn
{
    private int _count = 0;

    public SpanningDisplayColumn(ColumnInfo col)
    {
        super(col);
    }

    @Override
    public void renderGridDataCell(RenderContext ctx, Writer out) throws IOException
    {
        int targetCount = getRowSpan(ctx);
        if (targetCount > 1)
        {
            if (_count == 0)
            {
                super.renderGridDataCell(ctx, out);
            }
            _count++;
            if (_count == targetCount)
            {
                _count = 0;
            }
        }
        else
        {
            super.renderGridDataCell(ctx, out);
        }
    }



    public static class Factory implements DisplayColumnFactory
    {
        public Factory()
        {
            super();
        }

        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new SpanningDisplayColumn(colInfo);
        }
    }
}
