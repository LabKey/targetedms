/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;

public class FontAwesomeLinkColumn extends DataColumn
{
    private final String _icon;
    private final String _tooltip;

    public FontAwesomeLinkColumn(ColumnInfo col, String icon, String tooltip)
    {
        super(col);
        _icon = icon;
        _tooltip = tooltip;
    }

    @Override
    public @NotNull HtmlString getFormattedHtml(RenderContext ctx)
    {
        return HtmlString.unsafe("<i class=\"fa " + _icon + "\" title=\"" + PageFlowUtil.filter(_tooltip) + "\"></i>");
    }

    @Override
    public @Nullable HtmlString getTitle(RenderContext ctx)
    {
        return null;
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
}
