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
