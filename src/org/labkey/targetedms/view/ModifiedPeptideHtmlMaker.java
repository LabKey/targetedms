/*
 * Copyright (c) 2012-2026 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.LongHashMap;
import org.labkey.api.util.DOM;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.ChartColors;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.Protein;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.labkey.api.util.DOM.at;

/**
 * User: vsharma
 */
public class ModifiedPeptideHtmlMaker
{
    // RunId -> IsotopeLabelId (The database ID of the first isotope label type for the run).
    // Used to get the display color for label types.
    private final Map<Long, Long> _firstIsotopeLabelIdInDocMap;

    /** RunId -> all proteins in that run */
    private final Map<Long, List<Protein>> _proteins = new LongHashMap<>();

    /** Whether to include fixed modifications in the formatting */
    private final boolean _highlightFixedMods;

    private final static String[] HEX_PADDING = new String[] {
                                                        "",
                                                        "0",
                                                        "00",
                                                        "000",
                                                        "0000",
                                                        "00000",
                                                        "000000"
    };

    public ModifiedPeptideHtmlMaker()
    {
        this(true);
    }

    /**
     * @param highlightFixedMods Whether to include fixed modifications in the formatting
     */
    public ModifiedPeptideHtmlMaker(boolean highlightFixedMods)
    {
        _firstIsotopeLabelIdInDocMap = new HashMap<>();
        _highlightFixedMods = highlightFixedMods;
    }

    public Pair<HtmlString, List<List<SequencePart>>> getPrecursorHtml(Precursor precursor, Long runId, TargetedMSSchema schema)
    {
        Peptide peptide = PeptideManager.getPeptide(schema.getContainer(), precursor.getGeneralMoleculeId());
        return getPrecursorHtml(peptide, precursor, runId);
    }

    public Pair<HtmlString, List<List<SequencePart>>> getPrecursorHtml(Peptide peptide, Precursor precursor, Long runId)
    {
        return getPrecursorHtml(peptide.getId(), peptide.getPeptideGroupId(), precursor.getIsotopeLabelId(), precursor.getModifiedSequence(), runId);
    }

    public Pair<HtmlString, List<List<SequencePart>>> getPrecursorHtml(long peptideId, Long peptideGroupId, long isotopeLabelId, String precursorModifiedSequence, Long runId)
    {
        return getHtml(peptideId, peptideGroupId, isotopeLabelId, precursorModifiedSequence, runId, null, null, false, null);
    }

    public HtmlString getPeptideHtml(Peptide peptide, Long runId)
    {
        return getPeptideHtml(peptide.getId(), peptide.getPeptideGroupId(), peptide.getSequence(), peptide.getPeptideModifiedSequence(), runId, null, null, false, null).first;
    }

    public Pair<HtmlString, List<List<SequencePart>>> getPeptideHtml(long peptideId, Long peptideGroupId, String sequence, String peptideModifiedSequence, Long runId, @Nullable String previousAA, @Nullable String nextAA, boolean useParens, @Nullable Set<Pair<Integer, Integer>> strModIndices)
    {
        String altSequence = peptideModifiedSequence;
        if (StringUtils.isBlank(altSequence))
        {
            altSequence = sequence;
        }

        return getHtml(peptideId, peptideGroupId, null, altSequence, runId, previousAA, nextAA, useParens, strModIndices);
    }

    public List<Protein> getProteins(Long runId)
    {
        return runId == null ? Collections.emptyList() : _proteins.computeIfAbsent(runId, id -> PeptideGroupManager.getProteinsForRun(runId));
    }

    public Protein getProtein(Long peptideGroupId, Long runId)
    {
        List<Protein> proteins = getProteins(runId);

        if (peptideGroupId != null)
        {
            Optional<Protein> match = proteins.stream().filter(p -> p.getPeptideGroupId() == peptideGroupId.intValue()).findFirst();
            if (match.isPresent())
            {
                return match.get();
            }
        }
        return null;
    }

    /**
     * @param strModIndices optionally, the 0-based index of the amino acid that should be formatted as modified. First
     *                      value of the pair is the index of the cross-linked peptide (0 for non-cross linked
     *                      peptides), and the second value is the index of the AA
     */
    private Pair<HtmlString, List<List<SequencePart>>> getHtml(long peptideId, @Nullable Long peptideGroupId, @Nullable Long isotopeLabelId, String altSequence, Long runId, @Nullable String previousAA, @Nullable String nextAA, boolean useParens, @Nullable Set<Pair<Integer, Integer>> strModIndices)
    {
        Long firstIsotopeLabelIdInDoc = null;
        if(runId != null)
        {
            firstIsotopeLabelIdInDoc = _firstIsotopeLabelIdInDocMap.get(runId);
        }
        if (firstIsotopeLabelIdInDoc == null)
        {
            firstIsotopeLabelIdInDoc = IsotopeLabelManager.getLightIsotopeLabelId(peptideId);
            if(runId != null)
            {
                _firstIsotopeLabelIdInDocMap.put(runId, firstIsotopeLabelIdInDoc);
            }
        }

        boolean showPreviousNext = previousAA != null || nextAA != null;

        if (strModIndices == null)
        {
            strModIndices = ModificationManager.getStructuralModIndexes(peptideId, runId, _highlightFixedMods);
        }
        Set<Integer> isotopeModIndices = null;
        if(isotopeLabelId != null)
        {
            isotopeModIndices = ModificationManager.getIsotopeModIndexes(peptideId, isotopeLabelId, runId);
        }

        String labelModColor;
        StringBuilder error = new StringBuilder();
        if (isotopeLabelId != null)
        {
            if (isotopeLabelId >= firstIsotopeLabelIdInDoc)
            {
                labelModColor = toHex(ChartColors.getIsotopeColor(isotopeLabelId - firstIsotopeLabelIdInDoc).getRGB());
            }
            else
            {
                error.append("Error getting color for isotope label.");
                labelModColor = "black";
            }
        }
        else
        {
            labelModColor = "black";
        }

        List<List<SequencePart>> allParts = getCrossLinkedSequenceParts(altSequence, peptideGroupId, runId, strModIndices, isotopeModIndices, useParens, previousAA, nextAA, showPreviousNext);

        AtomicBoolean separator = new AtomicBoolean(false);

        return Pair.of(DOM.createHtmlFragment(
                DOM.DIV(at(DOM.Attribute.style, "display:inline-block;", DOM.Attribute.title, altSequence),
                        allParts.stream().map(parts ->
                                DOM.createHtmlFragment(
                                        separator.getAndSet(true) ? DOM.BR() : null,
                                        renderSequenceHtml(parts, labelModColor))),
                        error.isEmpty() ? DOM.DIV(at(DOM.Attribute.style, "color:red;"), error.toString()) : null)),
            allParts);
    }

    private List<List<SequencePart>> getCrossLinkedSequenceParts(String altSequence, Long peptideGroupId, Long runId, Set<Pair<Integer, Integer>> strModIndices, Set<Integer> isotopeModIndices, boolean useParens, String previousAA, String nextAA, boolean showPreviousNext)
    {
        List<List<SequencePart>> result = new ArrayList<>();

        CrossLinkedPeptideInfo crossLink = new CrossLinkedPeptideInfo(altSequence);

        Set<Integer> cdrIndices = new HashSet<>();
        Protein protein = getProtein(peptideGroupId, runId);
        if (protein != null)
        {
            String sequence = protein.getSequence();
            if (sequence != null && sequence.contains(crossLink.getBaseSequence().getUnmodified()))
            {
                // CDR ranges are one-based to make comparisons easy by doing the same for the peptide start/end indices
                int peptideStartIndex = sequence.indexOf(crossLink.getBaseSequence().getUnmodified()) + 1;
                int peptideEndIndex = peptideStartIndex + crossLink.getBaseSequence().getUnmodified().length();
                for (Pair<Integer, Integer> cdrRange : protein.getCdrRangesList())
                {
                    for (int i = Math.max(cdrRange.first, peptideStartIndex); i <= Math.min(cdrRange.second, peptideEndIndex); i++)
                    {
                        cdrIndices.add(i - peptideStartIndex);
                    }
                }
            }
        }

        result.add(renderSequence(crossLink.getBaseSequence(), filterModIndices(strModIndices, 0), isotopeModIndices, useParens, previousAA, nextAA, cdrIndices));

        // If we have cross-linking info, show those peptides too
        for (CrossLinkedPeptideInfo.PeptideSequence extraSequence : crossLink.getExtraSequences())
        {
            previousAA = null;
            nextAA = null;
            if (runId != null)
            {
                CrossLinkedPeptideInfo.Match matchingProtein = extraSequence.findMatch(getProteins(runId));
                if (matchingProtein != null)
                {
                    String proteinSequence = matchingProtein.protein().getSequence();
                    int startIndex = matchingProtein.index();
                    int endIndex = startIndex + extraSequence.getUnmodified().length();

                    // Stay consistent with primary sequence for showing or hiding previous and next amino acids
                    if (showPreviousNext)
                    {
                        if (startIndex > 0)
                        {
                            previousAA = Character.toString(proteinSequence.charAt(startIndex - 1));
                        }
                        else
                        {
                            previousAA = "-";
                        }
                        if (endIndex < proteinSequence.length())
                        {
                            nextAA = Character.toString(proteinSequence.charAt(endIndex));
                        }
                        else
                        {
                            nextAA = "-";
                        }
                    }
                }
            }
            result.add(renderSequence(extraSequence, filterModIndices(strModIndices, extraSequence.getPeptideIndex()), isotopeModIndices, useParens, previousAA, nextAA, Collections.emptySet()));
        }
        return result;
    }

    private Set<Integer> filterModIndices(Set<Pair<Integer, Integer>> strModIndices, int peptideIndex)
    {
        return strModIndices.stream().filter(x -> x.first == peptideIndex).map(x -> x.second).collect(Collectors.toSet());
    }

    public record SequencePart(String sequence, boolean modified, boolean isotopeModified, boolean crossLinked, boolean cdr) {}

    private HtmlString renderSequenceHtml(List<SequencePart> parts, String labelModColor)
    {
        return DOM.createHtmlFragment(
            parts.stream().map(part -> {
                if (part.modified || part.isotopeModified || part.crossLinked || part.cdr)
                {
                    StringBuilder style = new StringBuilder();
                    if (part.modified || part.isotopeModified || part.crossLinked)
                    {
                        style.append("font-weight:bold;");
                    }
                    if (part.isotopeModified)
                    {
                        style.append("color:").append(labelModColor).append(";");
                    }
                    else if (part.crossLinked)
                    {
                        style.append("color:").append("green").append(";");
                    }
                    if (part.modified)
                    {
                        style.append("text-decoration:underline;");
                    }
                    if (part.cdr)
                    {
                        style.append("background-color:lightgrey;");
                    }
                    return DOM.SPAN(at(DOM.Attribute.style, style), part.sequence);
                }
                return part.sequence;
            }));
    }

    private List<SequencePart> renderSequence(CrossLinkedPeptideInfo.PeptideSequence sequenceInfo, Set<Integer> strModIndices, Set<Integer> isotopeModIndices, boolean useParens, @Nullable String previousAA, @Nullable String nextAA, Set<Integer> cdrIndices)
    {
        List<SequencePart> parts = new ArrayList<>();

        if (previousAA != null)
        {
            parts.add(new SequencePart(useParens ? "(" + previousAA + ")" : previousAA, false, false, false, false));
        }

        String sequence = sequenceInfo.getUnmodified();
        for(int i = 0; i < sequence.length(); i++)
        {
            boolean isStrModified = strModIndices != null && strModIndices.contains(i);
            boolean isIsotopeModified = isotopeModIndices != null && isotopeModIndices.contains(i);
            boolean isCrossLinked = sequenceInfo.isCrossLinked(i);
            boolean isCDR = cdrIndices.contains(i);

            parts.add(new SequencePart(String.valueOf(sequence.charAt(i)), isStrModified, isIsotopeModified, isCrossLinked, isCDR));
        }

        if (nextAA != null)
        {
            parts.add(new SequencePart(useParens ? "(" + nextAA + ")" : nextAA, false, false, false, false));
        }

        return parts;
    }

    public String toHex(int rgb)
    {
        String hex = Integer.toHexString(rgb & 0x00ffffff);
        return "#"+ HEX_PADDING[6 - hex.length()] + hex;
    }
}
