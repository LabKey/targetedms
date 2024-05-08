package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.parser.Protein;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class PTMRiskDisplayColumnFactory implements DisplayColumnFactory
{
    @Override
    public org.labkey.api.data.DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new Col(colInfo);
    }

    private static class Col extends DataColumn
    {
        private final Function<Long, Protein> _proteinGetter = new PTMPercentsGroupedCustomizer.PeptideGroupIdProteinGetter();
        private Map<Pair<String, Long>, Pair<Boolean, String>> _stressedSamples;

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
                Number modified = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PercentModified"), Number.class);
                String sampleName = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "SampleName"), String.class);
                Long runId = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "RunId"), Long.class);
                if (modified == null || sampleName == null || runId == null)
                {
                    return null;
                }

                if (_stressedSamples == null)
                {
                    _stressedSamples = PTMPercentsGroupedCustomizer.getSampleMetadata(getBoundColumn().getParentTable().getUserSchema().getContainer());
                }

                boolean cdr = CDRConditionalFormattingDisplayColumnFactory.isInCDR(getBoundColumn().getFieldKey().getParent(), ctx, _proteinGetter);
                Pair<Boolean, String> metadata = _stressedSamples.get(Pair.of(sampleName, runId));
                boolean stressed = metadata != null && metadata.first.booleanValue();

                result = CDRConditionalFormattingDisplayColumnFactory.getRiskLevel(modified, cdr, stressed);
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
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PercentModified"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "SampleName"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Location"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "RunId"));
        }
    }

}
