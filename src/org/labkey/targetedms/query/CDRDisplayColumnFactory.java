package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.parser.Protein;

import java.util.Set;
import java.util.function.Function;

public class CDRDisplayColumnFactory implements DisplayColumnFactory
{
    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new Col(colInfo);
    }

    private static class Col extends DataColumn
    {
        private final Function<Long, Protein> _proteinGetter = new PTMPercentsGroupedCustomizer.PeptideGroupIdProteinGetter();
        public Col(ColumnInfo col)
        {
            super(col);
        }

        @Override
        public boolean isSortable()
        {
            return false;
        }

        @Override
        public boolean isFilterable()
        {
            return false;
        }

        @Override
        public Object getValue(RenderContext ctx)
        {
            Object result = super.getValue(ctx);
            if (result == null)
            {
                result = CDRConditionalFormattingDisplayColumnFactory.isInCDR(getBoundColumn().getFieldKey().getParent(), ctx, _proteinGetter);
            }
            return result;
        }

        @Override
        public Object getDisplayValue(RenderContext ctx)
        {
            return getValue(ctx);
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Location"));
        }
    }


}
