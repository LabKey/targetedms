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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: vsharma
 * Date: 4/29/12
 * Time: 7:33 PM
 */
public class ModifiedPeptideHtmlMaker
{
    // RunId -> IsotopeLabelId (The database ID of the first isotope label type for the run).
    // Used to get the display color for label types.
    private final Map<Long, Long> _firstIsotopeLabelIdInDocMap;

    /** RunId -> all proteins in that run */
    private final Map<Long, List<Protein>> _proteins = new HashMap<>();

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
        _firstIsotopeLabelIdInDocMap = new HashMap<>();
    }

    public HtmlString getPrecursorHtml(Precursor precursor, Long runId, TargetedMSSchema schema)
    {
        Peptide peptide = PeptideManager.getPeptide(schema.getContainer(), precursor.getGeneralMoleculeId());
        return getPrecursorHtml(peptide, precursor, runId);
    }

    public HtmlString getPrecursorHtml(Peptide peptide, Precursor precursor, Long runId)
    {
        return getPrecursorHtml(peptide.getId(), peptide.getPeptideGroupId(), precursor.getIsotopeLabelId(), precursor.getModifiedSequence(), runId);
    }

    public HtmlString getPrecursorHtml(long peptideId, Long peptideGroupId, long isotopeLabelId, String precursorModifiedSequence, Long runId)
    {
        return getHtml(peptideId, peptideGroupId, isotopeLabelId, precursorModifiedSequence, runId, null, null, false, null);
    }

    public HtmlString getPeptideHtml(Peptide peptide, Long runId)
    {
        return getPeptideHtml(peptide.getId(), peptide.getPeptideGroupId(), peptide.getSequence(), peptide.getPeptideModifiedSequence(), runId, null, null, false, null);
    }

    public HtmlString getPeptideHtml(long peptideId, Long peptideGroupId, String sequence, String peptideModifiedSequence, Long runId, @Nullable String previousAA, @Nullable String nextAA, boolean useParens, @Nullable Set<Pair<Integer, Integer>> strModIndices)
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
    private HtmlString getHtml(long peptideId, @Nullable Long peptideGroupId, @Nullable Long isotopeLabelId, String altSequence, Long runId, @Nullable String previousAA, @Nullable String nextAA, boolean useParens, @Nullable Set<Pair<Integer, Integer>> strModIndices)
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
            strModIndices = ModificationManager.getStructuralModIndexes(peptideId, runId);
        }
        Set<Integer> isotopeModIndices = null;
        if(isotopeLabelId != null)
        {
            isotopeModIndices = ModificationManager.getIsotopeModIndexes(peptideId, isotopeLabelId, runId);
        }

        StringBuilder result = new StringBuilder();
        result.append("<div style=\"display: inline-block;\" title='").append(PageFlowUtil.filter(altSequence)).append("'>");
        String labelModColor = "black";
        StringBuilder error = new StringBuilder();
        if(isotopeLabelId != null)
        {
            if(isotopeLabelId >= firstIsotopeLabelIdInDoc)
            {
                labelModColor = toHex(ChartColors.getIsotopeColor(isotopeLabelId - firstIsotopeLabelIdInDoc).getRGB());
            }
            else
            {
                error.append("Error getting color for isotope label.");
            }
        }

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

        renderSequence(crossLink.getBaseSequence(), filterModIndices(strModIndices, 0), isotopeModIndices, result, labelModColor, useParens, previousAA, nextAA, cdrIndices);

        // If we have cross-linking info, show those peptides too
        for (CrossLinkedPeptideInfo.PeptideSequence extraSequence : crossLink.getExtraSequences())
        {
            previousAA = null;
            nextAA = null;
            result.append("<br />\n");
            if (runId != null)
            {
                Protein matchingProtein = extraSequence.findMatch(getProteins(runId));
                if (matchingProtein != null)
                {
                    String proteinSequence = matchingProtein.getSequence();
                    int startIndex = proteinSequence.indexOf(extraSequence.getUnmodified());
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
            renderSequence(extraSequence, filterModIndices(strModIndices, extraSequence.getPeptideIndex()), isotopeModIndices, result, labelModColor, useParens, previousAA, nextAA, Collections.emptySet());
        }

        result.append("</div>");

        if (!error.isEmpty())
        {
            result.append("<div style='color:red;'>").append(PageFlowUtil.filter(error.toString())).append("</div>");
        }
        return HtmlString.unsafe(result.toString());
    }

    private Set<Integer> filterModIndices(Set<Pair<Integer, Integer>> strModIndices, int peptideIndex)
    {
        return strModIndices.stream().filter(x -> x.first == peptideIndex).map(x -> x.second).collect(Collectors.toSet());
    }

    private void renderSequence(CrossLinkedPeptideInfo.PeptideSequence sequenceInfo, Set<Integer> strModIndices, Set<Integer> isotopeModIndices, StringBuilder result, String labelModColor, boolean useParens, @Nullable String previousAA, @Nullable String nextAA, Set<Integer> cdrIndices)
    {
        if (previousAA != null)
        {
            if (useParens)
            {
                result.append("(");
            }
            result.append(PageFlowUtil.filter(previousAA));
            result.append(useParens ? ")" : ".");
        }

        String sequence = sequenceInfo.getUnmodified();

        for(int i = 0; i < sequence.length(); i++)
        {
            boolean isStrModified = strModIndices != null && strModIndices.contains(i);
            boolean isIsotopeModified = isotopeModIndices != null && isotopeModIndices.contains(i);
            boolean isCrossLinked = sequenceInfo.isCrossLinked(i);
            boolean isCDR = cdrIndices.contains(i);

            if (isIsotopeModified || isStrModified || isCrossLinked || isCDR)
            {
                StringBuilder style = new StringBuilder("style='");
                if (isIsotopeModified || isStrModified || isCrossLinked)
                {
                    style.append("font-weight:bold;");
                }
                if (isIsotopeModified)
                {
                    style.append("color:").append(labelModColor).append(";");
                }
                else if (isCrossLinked)
                {
                    style.append("color:").append("green").append(";");
                }
                if (isStrModified)
                {
                    style.append("text-decoration:underline;");
                }
                if (isCDR)
                {
                    style.append("background-color:lightgrey;");
                }
                style.append("'");
                result.append("<span ").append(style).append(">").append(sequence.charAt(i)).append("</span>");
            }
            else
            {
                result.append(PageFlowUtil.filter(sequence.charAt(i)));
            }
        }

        if (nextAA != null)
        {
            result.append(useParens ? "(" : ".");
            result.append(PageFlowUtil.filter(nextAA));
            if (useParens)
            {
                result.append(")");
            }
        }
    }

    public String toHex(int rgb)
    {
        String hex = Integer.toHexString(rgb & 0x00ffffff);
        return "#"+ HEX_PADDING[6 - hex.length()] + hex;
    }
}
