/*
 * Copyright (c) 2014-2018 LabKey Corporation
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

package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Pair;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.targetedms.parser.Protein;
import org.labkey.targetedms.view.IconFactory;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: vsharma
 */
public abstract class ModifiedSequenceDisplayColumn extends IconColumn
{
    private static final Logger LOG = LogHelper.getLogger(ModifiedSequenceDisplayColumn.class, "Wires up formatting for peptide sequences");
    private final ModifiedPeptideHtmlMaker _htmlMaker;
    String _iconPath;
    HtmlString _cellData;

    boolean _exportAsStrippedHtml = false;

    public static final String PEPTIDE_COLUMN_NAME = "ModifiedPeptideDisplayColumn";
    public static final String PRECURSOR_COLUMN_NAME = "ModifiedPrecursorDisplayColumn";

    public ModifiedSequenceDisplayColumn(ColumnInfo colInfo)
    {
        super(colInfo);

        _htmlMaker = new ModifiedPeptideHtmlMaker();
    }

    ModifiedPeptideHtmlMaker getHtmlMaker()
    {
        return _htmlMaker;
    }

    public abstract void initialize(RenderContext ctx);

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        initialize(ctx);
        super.renderGridCellContents(ctx, out);
    }

    @Override
    public String getTsvFormattedValue(RenderContext ctx)
    {
        if (_exportAsStrippedHtml)
        {
            initialize(ctx);
            return extractTextFromHtml(_cellData);
        }
        return super.getTsvFormattedValue(ctx);
    }

    @Override
    public Object getExcelCompatibleValue(RenderContext ctx)
    {
        if (_exportAsStrippedHtml)
        {
            initialize(ctx);
            return extractTextFromHtml(_cellData);
        }
        return super.getExcelCompatibleValue(ctx);
    }

    @Override
    public Object getJsonValue(RenderContext ctx)
    {
        if (_exportAsStrippedHtml)
        {
            initialize(ctx);
            return extractTextFromHtml(_cellData);
        }
        return super.getJsonValue(ctx);
    }

    private String extractTextFromHtml(HtmlString html)
    {
        // This isn't a particularly robust way to extract text from HTML but given our simple HTML (no script, etc), it's enough
        return html.toString().replaceAll("<[^>]*>", "");
    }

    @Override
    String getIconPath()
    {
        return _iconPath;
    }

    @Override
    HtmlString getCellDataHtml(RenderContext ctx)
    {
        return _cellData;
    }

    /**
     * Supports a number of properties that can be configured via .query.xml metadata:
     * showNextAndPrevious: true/false whether to show the adjacent amino acids as well as the peptide sequence itself
     * useParens: true/false whether to surround the adjacent amino acids in parentheses (if not, separate by dots)
     * exportFormatted: true/false whether to export HTML or plaintext for the sequence
     * _modificationSite: name of column to use for the amino acid and index within the protein for the sole site to format as modified
     */
    public static class PeptideDisplayColumnFactory implements DisplayColumnFactory
    {
        private boolean _exportStrippedHtml = false;
        private boolean _showNextAndPrevious = false;
        private boolean _useParens = false;
        /** Optionally, the name of the column that identifies the animo acid and index within the protein to highlight as modified */
        private String _modificationSite;

        public PeptideDisplayColumnFactory()
        {
        }

        public PeptideDisplayColumnFactory(MultiValuedMap<String, String> map)
        {
            _showNextAndPrevious = getBooleanProperty(map, "showNextAndPrevious", _showNextAndPrevious);
            _useParens = getBooleanProperty(map, "useParens", _useParens);
            _exportStrippedHtml = getBooleanProperty(map, "exportFormatted", _exportStrippedHtml);
            _modificationSite = map == null || map.get("modificationSite").isEmpty() ? null : map.get("modificationSite").iterator().next();
        }

        private boolean getBooleanProperty(MultiValuedMap<String, String> map, String propertyName, boolean defaultValue)
        {
            Collection<String> values = map == null ? Collections.emptyList() : map.get(propertyName);
            if (!values.isEmpty())
            {
                return Boolean.valueOf(values.iterator().next());
            }
            return defaultValue;
        }

        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new ModifiedSequenceDisplayColumn.PeptideCol(colInfo, _showNextAndPrevious, _useParens, _exportStrippedHtml, _modificationSite);
        }
    }

    public static class PeptideCol extends ModifiedSequenceDisplayColumn
    {
        private final boolean _showNextAndPrevious;
        private final boolean _useParens;
        private final String _modificationSite;
        private final FieldKey _previousAAFieldKey;
        private final FieldKey _nextAAFieldKey;

        public PeptideCol(ColumnInfo colInfo)
        {
            this(colInfo, false, false, false, null);
        }

        public PeptideCol(ColumnInfo colInfo, boolean showNextAndPrevious, boolean useParens, boolean exportStrippedHtml, String modificationSite)
        {
            super(colInfo);
            _showNextAndPrevious = showNextAndPrevious;
            _useParens = useParens;
            _modificationSite = modificationSite;
            _exportAsStrippedHtml = exportStrippedHtml;

            _previousAAFieldKey = FieldKey.fromString(getParentFieldKey(), "PreviousAa");
            _nextAAFieldKey = FieldKey.fromString(getParentFieldKey(), "NextAa");
        }

        @Override
        String getLinkTitle()
        {
            return "Peptide Details";
        }

        @Override
        public Object getDisplayValue(RenderContext ctx)
        {
            // Render for TSV and Excel
            Object result = super.getDisplayValue(ctx);
            if (_showNextAndPrevious)
            {
                String previous = ctx.get(_previousAAFieldKey, String.class);
                String next = ctx.get(_nextAAFieldKey, String.class);
                if (previous != null && next != null)
                {
                    if (_useParens)
                    {
                        return "(" + previous + ")" + result + "(" + next + ")";
                    }
                    return previous + "." + result + "." + next;
                }
            }
            return result;
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Sequence"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Decoy"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "StandardType"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId"));
            if (_modificationSite != null)
            {
                keys.add(FieldKey.fromString(super.getParentFieldKey(), _modificationSite));
            }
            if (_showNextAndPrevious)
            {
                keys.add(_previousAAFieldKey);
                keys.add(_nextAAFieldKey);
            }
        }

        @Override
        public void initialize(RenderContext ctx)
        {
            Long peptideId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"), Long.class);

            String sequence = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Sequence"), String.class);

            Long runId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"), Long.class);
            Long peptideGroupId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId"), Long.class);

            Boolean decoy = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Decoy"), Boolean.class);
            if(decoy == null)  decoy = Boolean.FALSE;

            String standardType = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "StandardType"), String.class);

            String previousAA = _showNextAndPrevious ? ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PreviousAa"), String.class) : null;
            String nextAA = _showNextAndPrevious ? ctx.get(FieldKey.fromString(super.getParentFieldKey(), "NextAa"), String.class) : null;

            String peptideModifiedSequence = (String)getValue(ctx);

            if (peptideId == null || sequence == null || runId == null)
            {
                _cellData = HtmlString.of(peptideModifiedSequence);
            }
            else
            {
                Set<Pair<Integer, Integer>> strModIndices = null;
                if (_modificationSite != null)
                {
                    String modificationSite = ctx.get(FieldKey.fromString(super.getParentFieldKey(), _modificationSite), String.class);
                    if (modificationSite != null && modificationSite.length() >= 2)
                    {
                        char aa = modificationSite.charAt(0);
                        String indexString = modificationSite.substring(1);
                        try
                        {
                            int index = Integer.parseInt(indexString);
                            Protein p = getHtmlMaker().getProtein(peptideGroupId, runId);
                            if (p != null && p.getSequence() != null)
                            {
                                int startIndex = p.getSequence().indexOf(sequence);
                                int aaIndex = index - startIndex - 1;

                                if (sequence.length() > aaIndex && sequence.charAt(aaIndex) == aa)
                                {
                                    peptideModifiedSequence = sequence;
                                    strModIndices = new HashSet<>();
                                    strModIndices.add(Pair.of(0, aaIndex));
                                }
                                else
                                {
                                    LOG.debug("Modified residue didn't match for " + modificationSite + " at calculated index " + aaIndex + " on peptide " + sequence + " in document " + runId);
                                }
                            }
                        }
                        catch (NumberFormatException ignored)
                        {
                            LOG.debug("Bad modificationSite value: " + modificationSite + " in document " + runId);
                        }
                    }
                }

                _cellData = getHtmlMaker().getPeptideHtml(peptideId, peptideGroupId, sequence, peptideModifiedSequence, runId, previousAA, nextAA, _useParens, strModIndices);
                _iconPath = IconFactory.getPeptideIconPath(peptideId, runId, decoy, standardType);
            }
        }
    }

    public static class PrecursorCol extends ModifiedSequenceDisplayColumn
    {
        public PrecursorCol(ColumnInfo colInfo)
        {
            super(colInfo);
        }

        @Override
        String getLinkTitle()
        {
            return "Precursor Details";
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Decoy"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId"));
        }

        @Override
        public void initialize(RenderContext ctx)
        {
            Long precursorId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"), Long.class);
            Long peptideId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"), Long.class);
            String sequence = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"), String.class);
            Long isotopeLabelId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"), Long.class);
            Long runId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"), Long.class);
            Long peptideGroupId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId"), Long.class);
            Boolean decoy = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Decoy"), Boolean.class);
            if(decoy == null) decoy = Boolean.FALSE;

            String precursorModifiedSequence = (String)getValue(ctx);

            if(precursorId == null || peptideId == null || isotopeLabelId == null || precursorModifiedSequence == null || sequence == null || runId == null)
            {
                _cellData = HtmlString.of(precursorModifiedSequence);
            }
            else
            {
                _cellData = getHtmlMaker().getPrecursorHtml(peptideId,
                        peptideGroupId,
                        isotopeLabelId,
                        precursorModifiedSequence,
                        runId);
                _iconPath = IconFactory.getPrecursorIconPath(precursorId, runId, decoy);
            }
        }
    }
}
