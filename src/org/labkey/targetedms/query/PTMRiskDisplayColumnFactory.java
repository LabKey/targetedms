/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
                Number percentModified = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PercentModified"), Number.class);
                String replicateName = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "ReplicateName"), String.class);
                Long runId = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "RunId"), Long.class);
                String modification = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Modification"), String.class);
                String modifiedSequence = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Sequence"), String.class);
                if (percentModified == null || replicateName == null || runId == null)
                {
                    return null;
                }

                if (_stressedSamples == null)
                {
                    _stressedSamples = PTMPercentsGroupedCustomizer.getSampleMetadata(getBoundColumn().getParentTable().getUserSchema().getContainer());
                }

                boolean cdr = CDRConditionalFormattingDisplayColumnFactory.isInCDR(getBoundColumn().getFieldKey().getParent(), ctx, _proteinGetter);
                Pair<Boolean, String> metadata = _stressedSamples.get(Pair.of(replicateName, runId));
                boolean stressed = metadata != null && metadata.first.booleanValue();

                result = CDRConditionalFormattingDisplayColumnFactory.getRiskLevel(percentModified, cdr, stressed, modification, modifiedSequence);
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
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "ReplicateName"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Location"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "RunId"));
        }
    }

}
