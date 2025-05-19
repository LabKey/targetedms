/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
import org.labkey.api.data.RenderContext;
import org.labkey.api.targetedms.RepresentativeDataState;
import org.labkey.api.writer.HtmlWriter;

import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.SPAN;
import static org.labkey.api.util.DOM.at;

public class RepresentativeStateDisplayColumn extends DataColumn
{
    public RepresentativeStateDisplayColumn (ColumnInfo columnInfo)
    {
        super(columnInfo);
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, HtmlWriter out)
    {
        Object representativeState = getValue(ctx);
        if (representativeState == null)
            return;

        if (RepresentativeDataState.Representative.getLabel().equals(representativeState.toString()))
        {
            SPAN(
                at(style, "color:green;"),
                representativeState.toString()
            ).appendTo(out);
        }
        else if (RepresentativeDataState.Conflicted.getLabel().equals(representativeState.toString()))
        {
            SPAN(
                at(style, "color:red;"),
                representativeState.toString()
            ).appendTo(out);
        }
        else
        {
            out.write(representativeState.toString());
        }
    }
}
