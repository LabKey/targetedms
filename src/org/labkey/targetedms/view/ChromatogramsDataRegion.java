/*
 * Copyright (c) 2012-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.view;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.collections.ResultSetRowMapFactory;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DetailsColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.Results;
import org.labkey.api.data.UpdateColumn;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.PageConfig;
import org.labkey.targetedms.query.ChromatogramGridQuerySettings;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * User: vsharma
 * Date: 4/28/12
 * Time: 5:40 PM
 */
public class ChromatogramsDataRegion extends DataRegion
{
    public static final String GROUP_CHROM_DATA_REGION = "GroupChromatograms";
    public static final String PRECURSOR_CHROM_DATA_REGION = "PrecursorChromatograms";
    public static final String PEPTIDE_CHROM_DATA_REGION = "PeptideChromatograms";
    public static final String PEPTIDE_PRECURSOR_CHROM_DATA_REGION = "PeptidePrecursorChromatograms";
    public static final String MOLECULE_PRECURSOR_CHROM_DATA_REGION = "MoleculePrecursorChromatograms";

    public static final String HIGHLIGHTED_CHROMATOGRAM_PARAMETER_NAME = "highlightChromInfoId";
    public static final String FRAGMENT_PREFIX = "ChromInfo";
    private final List<String> _listeningDataRegionNames = new ArrayList<>();
    private final JSONArray _svgs = new JSONArray();
    private final boolean _splitGraph;

    private String _legendElementId;
    private final boolean _canBeSplit;

    private final boolean _replicateChromatogramsGrouped;

    public ChromatogramsDataRegion(ViewContext context, FilteredTable<?> tableInfo, String name, boolean splitGraph, boolean canBeSplit)
    {
        this(context, tableInfo, name, splitGraph, canBeSplit, "Id", false);
    }

    public ChromatogramsDataRegion(ViewContext context, FilteredTable<?> tableInfo, String name, boolean splitGraph, boolean canBeSplit, String columns, boolean replicateChromatogramsGrouped)
    {
        setTable(tableInfo);
        addColumns(tableInfo, columns);
        _splitGraph = splitGraph;
        _canBeSplit = canBeSplit;
        _replicateChromatogramsGrouped = replicateChromatogramsGrouped;

        ChromatogramGridQuerySettings settings = new ChromatogramGridQuerySettings(context, name, _replicateChromatogramsGrouped);
        setSettings(settings);

        populateButtonBar();

        setShadeAlternatingRows(false);
    }

    protected void populateButtonBar()
    {
        ButtonBar bar = new ButtonBar();
        bar.add(createRowSizeMenuButton());
        if (_canBeSplit)
        {
            bar.add(createSplitGraphButton());
        }
        setButtonBar(bar);
    }

    private MenuButton createSplitGraphButton()
    {
        MenuButton graphMenu = new MenuButton("Graph Type", getName() + ".Menu.GraphType");

        URLHelper target = getSettings().getSortFilterURL();

        graphMenu.addMenuItem("Split", target.clone().replaceParameter("splitGraph", "true"), null, _splitGraph);
        graphMenu.addMenuItem("Combined", target.clone().replaceParameter("splitGraph", "false"), null, !_splitGraph);

        return graphMenu;
    }

    @Override
    protected void renderTable(RenderContext ctx, Writer out) throws SQLException, IOException
    {
        super.renderTable(ctx, out);

        out.write("\n<script type=\"text/javascript\" nonce=\"");
        out.write(PageFlowUtil.filter(PageConfig.getScriptNonceHeader(ctx.getRequest())));
        out.write("\">");
        out.write("LABKEY.DataRegions[" + PageFlowUtil.jsString(getName()) + "].refreshPlots = function() {\n");
        out.write("  const svgInfos = " + _svgs.toString() + ";\n");
        out.write("  for (let i = 0; i < svgInfos.length; i++) {\n");
        out.write("    let svgInfo = svgInfos[i];\n");
        out.write("    LABKEY.targetedms.SVGChart.requestAndRenderSVG(svgInfo.url, document.getElementById(svgInfo.mainId), ");
        out.write(_legendElementId == null ? "null" : ("document.getElementById(" + PageFlowUtil.jsString(_legendElementId) + ")"));
        out.write(", document.getElementById(svgInfo.labelId));\n");
        out.write("  }\n");
        out.write("};\n");
        out.write("LABKEY.DataRegions[" + PageFlowUtil.jsString(getName()) + "].refreshPlots();\n");

        for (String listeningDataRegionName : _listeningDataRegionNames)
        {
            out.write("LABKEY.DataRegions[");
            out.write(PageFlowUtil.jsString(listeningDataRegionName));
            out.write("].on('selectchange', LABKEY.DataRegions[" + PageFlowUtil.jsString(getName()) + "].refreshPlots);\n");
        }

        out.write("</script>\n");
    }

    @Override
    protected void renderGridHeaderColumns(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers)
    {
        // No need to render the headers for this specialized grid - they just take space
    }

    @Override
    protected int renderTableContents(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers) throws SQLException, IOException
    {
        int rowIndex = 0;
        int maxRowSize = getSettings().getMaxRowSize();
        int count = 0;

        Results results = ctx.getResults();

        // unwrap for efficient use of ResultSetRowMapFactory
        try (ResultSet rs = results.getResultSet())
        {
            assert rs != null;
            ResultSetRowMapFactory factory = ResultSetRowMapFactory.create(rs);

            while (rs.next())
            {
                if (count == 0)
                {
                    out.write("<tr");
                    String rowClass = getRowClass(ctx, rowIndex);
                    if (rowClass != null)
                        out.write(" class=\"" + rowClass + "\"");
                    out.write(">");
                }
                ctx.setRow(factory.getRowMap(rs));
                renderTableRow(ctx, out, showRecordSelectors, renderers, rowIndex++);
                count++;
                if (count == maxRowSize)
                {
                    out.write("</tr>\n");
                    count = 0;
                }
            }
        }

        if (count != 0)
        {
            while(count < maxRowSize)
            {
                out.write("<td style=\"border:0;\"></td>");
                count++;
            }
            out.write("</tr>\n");
        }

        return rowIndex;
    }

    @Override
    protected void renderTableRow(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex) throws IOException
    {
        DisplayColumn detailsColumn = getDetailsUpdateColumn(ctx, renderers, true);
        DisplayColumn updateColumn = getDetailsUpdateColumn(ctx, renderers, false);

        if (showRecordSelectors || (detailsColumn != null || updateColumn != null))
            renderActionColumn(ctx, out, rowIndex, showRecordSelectors, detailsColumn, updateColumn);

        for (DisplayColumn renderer : renderers)
            if (renderer.isVisible(ctx))
            {
                if (renderer instanceof DetailsColumn || renderer instanceof UpdateColumn)
                    continue;

                renderer.renderGridDataCell(ctx, out);
            }

    }

    @Override
    public ChromatogramGridQuerySettings getSettings()
    {
        return (ChromatogramGridQuerySettings) super.getSettings();
    }

    protected MenuButton createRowSizeMenuButton()
    {
        int maxRowSize = getSettings().getMaxRowSize();

        MenuButton pageSizeMenu = new MenuButton("Row Size", getName() + ".Menu.RowSize");

        // insert current maxRows into sorted list of possible sizes
        List<Integer> sizes = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 10));
        if (maxRowSize > 0)
        {
            int index = Collections.binarySearch(sizes, maxRowSize);
            if (index < 0)
            {
                sizes.add(-index-1, maxRowSize);
            }
        }

        URLHelper target = getSettings().getSortFilterURL();
        target.deleteParameter("maxRowSize");

        for (Integer rowSize : sizes)
        {
            URLHelper url = target.clone();
            url.replaceParameter("maxRowSize", rowSize);

            boolean checked = rowSize == maxRowSize;
            // When all the chromatograms (total precursor ion + fragment ion) from a replicate are displayed together, the row size is
            // the number of replicates for which chromatograms are being displayed in a row, rather than the number of plots in a row.
            // Clarify this in the caption.
            String menuItemCaption = (_replicateChromatogramsGrouped ? StringUtilsLabKey.pluralize(rowSize, "replicate") : rowSize.toString()) + " per row";
            NavTree item = pageSizeMenu.addMenuItem(menuItemCaption, url, null, checked);
            item.setId("Row Size:" + rowSize);
        }

        return pageSizeMenu;
    }

    public void addRefreshListener(String dataRegionName)
    {
        _listeningDataRegionNames.add(dataRegionName);
    }

    public void addSVG(String url, String mainId, String labelId)
    {
        JSONObject svgInfo = new JSONObject();
        svgInfo.put("url", url);
        svgInfo.put("mainId", mainId);
        svgInfo.put("labelId", labelId);
        _svgs.put(svgInfo);
    }

    public void setLegendElementId(String legendElementId)
    {
        _legendElementId = legendElementId;
    }
}
