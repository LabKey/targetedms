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
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.ChartColors;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final Map<Long, List<PeptideGroup>> _proteins = new HashMap<>();

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
        return getPrecursorHtml(peptide.getId(), precursor.getIsotopeLabelId(), precursor.getModifiedSequence(), runId);
    }

    public HtmlString getPrecursorHtml(long peptideId, long isotopeLabelId, String precursorModifiedSequence, Long runId)
    {
        return getHtml(peptideId, isotopeLabelId, precursorModifiedSequence, runId, null, null, false);
    }

    public HtmlString getPeptideHtml(Peptide peptide, Long runId)
    {
        return getPeptideHtml(peptide.getId(), peptide.getSequence(), peptide.getPeptideModifiedSequence(), runId, null, null, false);
    }

    public HtmlString getPeptideHtml(long peptideId, String sequence, String peptideModifiedSequence, Long runId, @Nullable String previousAA, @Nullable String nextAA, boolean useParens)
    {
        String altSequence = peptideModifiedSequence;
        if(StringUtils.isBlank(altSequence))
        {
            altSequence = sequence;
        }

        return getHtml(peptideId, null, altSequence, runId, previousAA, nextAA, useParens);
    }

    private HtmlString getHtml(long peptideId, @Nullable Long isotopeLabelId, String altSequence, Long runId, @Nullable String previousAA, @Nullable String nextAA, boolean useParens)
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

        Set<Integer> strModIndices = ModificationManager.getStructuralModIndexes(peptideId, runId);
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

        renderSequence(crossLink.getBaseSequence(), strModIndices, isotopeModIndices, result, labelModColor, useParens, previousAA, nextAA);

        // If we have cross-linking info, show those peptides too
        for (CrossLinkedPeptideInfo.PeptideSequence extraSequence : crossLink.getExtraSequences())
        {
            previousAA = null;
            nextAA = null;
            result.append("<br />\n");
            if (runId != null)
            {
                List<PeptideGroup> proteins = _proteins.computeIfAbsent(runId, id -> PeptideGroupManager.getPeptideGroupsForRun(runId));
                PeptideGroup matchingProtein = extraSequence.findMatch(proteins);
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
            renderSequence(extraSequence, strModIndices, isotopeModIndices, result, labelModColor, useParens, previousAA, nextAA);
        }

        result.append("</div>");

        if(error.length() > 0)
        {
            result.append("<div style='color:red;'>").append(PageFlowUtil.filter(error.toString())).append("</div>");
        }
        return HtmlString.unsafe(result.toString());
    }

    private void renderSequence(CrossLinkedPeptideInfo.PeptideSequence sequenceInfo, Set<Integer> strModIndices, Set<Integer> isotopeModIndices, StringBuilder result, String labelModColor, boolean useParens, @Nullable String previousAA, @Nullable String nextAA)
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

            if(isIsotopeModified || isStrModified || isCrossLinked)
            {
                StringBuilder style = new StringBuilder("style='font-weight:bold;");
                if(isIsotopeModified)
                {
                    style.append("color:").append(labelModColor).append(";");
                }
                else if (isCrossLinked)
                {
                    style.append("color:").append("green").append(";");
                }
                if(isStrModified)
                {
                    style.append("text-decoration:underline;");
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
