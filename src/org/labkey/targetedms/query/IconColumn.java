/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.view.IconFactory;

import java.util.Set;

/**
 * Created by vsharma on 9/30/2016.
 */
public abstract class IconColumn extends DataColumn
{
     private final FieldKey _parentFieldKey;

    public IconColumn(@NotNull ColumnInfo colInfo)
    {
        super(colInfo);

        _parentFieldKey = colInfo.getFieldKey().getParent();
        setTextAlign("left");
    }

    FieldKey getParentFieldKey()
    {
        return _parentFieldKey;
    }

    abstract String getIconPath();

    abstract String getLinkTitle();

    abstract HtmlString getCellDataHtml(RenderContext ctx);

    boolean removeLinkDefaultColor()
    {
        return true;
    }

    private HtmlString getIconHtml(String iconPath)
    {
        if(StringUtils.isBlank(iconPath))
        {
            return HtmlString.EMPTY_STRING;
        }

        return HtmlString.unsafe("<img src=\"" + PageFlowUtil.filter(iconPath) +
                "\" title=\"" +
                PageFlowUtil.filter(getLinkTitle()) +
                "\" width=\"16\" height=\"16\" style=\"vertical-align: top; margin-right: 5px;\"/>");
    }

    @Override
    public @NotNull HtmlString getFormattedHtml(RenderContext ctx)
    {
        HtmlString iconHtml = getIconHtml(getIconPath());
        HtmlString cellDataHtml = getCellDataHtml(ctx);
        return
                HtmlStringBuilder.of(HtmlString.unsafe("<nobr>")).
                        append(iconHtml).
                        append(cellDataHtml == null ? HtmlString.EMPTY_STRING : cellDataHtml).
                        append(HtmlString.unsafe("</nobr>")).
                        getHtmlString();
    }

    @Override
    public @NotNull String getCssStyle(RenderContext ctx)
    {
        if(removeLinkDefaultColor())
        {
            return "color: #000000";
        }

        return super.getCssStyle(ctx);
    }

    public static class MoleculeDisplayCol extends IconColumn
    {
        public MoleculeDisplayCol(ColumnInfo colInfo)
        {
            super(colInfo);
        }

        @Override
        String getIconPath()
        {
            return IconFactory.getMoleculeIconPath();
        }

        @Override
        String getLinkTitle()
        {
            return "Molecule Details";
        }

        @Override
        HtmlString getCellDataHtml(RenderContext ctx)
        {
            return HtmlString.of(ctx.get(getColumnInfo().getFieldKey(), String.class));
        }

        @Override
        boolean removeLinkDefaultColor()
        {
            return false;
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
        }
    }
}
