/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.collections.LongHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.parser.Protein;
import org.labkey.targetedms.view.CrossLinkedPeptideInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders the cross-linked peptide info in a few different variants. For unlinked peptides, passes through the primary
 * value from the underlying SQL query. Calculates cross-linking info by parsing a sibling column, ModifiedSequence
 */
public class CrossLinkedPeptideDisplayColumn extends DataColumn
{
    // Cache the proteins for a given run to avoid many redundant queries
    private final Map<Long, List<Protein>> _proteins = new LongHashMap<>();
    private final Formatter _formatter;

    private CrossLinkedPeptideDisplayColumn(ColumnInfo col, Formatter formatter)
    {
        super(col);
        setNoWrap(true);
        _formatter = formatter;
    }

    interface Formatter
    {
        String format(CrossLinkedPeptideInfo.Match match, CrossLinkedPeptideInfo.PeptideSequence sequence, String modification);
    }

    @Override
    public Object getDisplayValue(RenderContext ctx)
    {
        return getValue(ctx);
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
        Object defaultValue = super.getValue(ctx);

        String modifiedSequence = ctx.get(getModifiedSequenceFieldKey(), String.class);
        String modification = ctx.get(getModificationFieldKey(), String.class);
        if (modifiedSequence != null)
        {
            CrossLinkedPeptideInfo i = new CrossLinkedPeptideInfo(modifiedSequence);
            List<Protein> proteins = getProteinsFromRun(ctx);
            List<CrossLinkedPeptideInfo.PeptideSequence> sequences = i.getAllSequences();
            Integer peptideIndex = ctx.get(CrossLinkedPeptideExcelWriter.PEPTIDE_INDEX) instanceof Integer index ? index : null;
            return render(sequences, proteins, modification, peptideIndex);
        }
        return defaultValue;
    }

    /** @param peptideIndex non-null when the Excel export is writing each cross-linked peptide on its own row */
    protected @NotNull String render(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification, @Nullable Integer peptideIndex)
    {
        return formatAll(sequences, proteins, modification).stream()
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /** @return the formatted value for each peptide, blank when it matches no protein */
    protected @NotNull List<String> formatAll(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification)
    {
        return sequences.stream().map(sequence -> formatSequence(sequence, proteins, modification)).toList();
    }

    protected @NotNull List<String> getPeptideValues(RenderContext ctx)
    {
        String modifiedSequence = ctx.get(getModifiedSequenceFieldKey(), String.class);
        if (modifiedSequence == null)
        {
            return List.of();
        }
        return formatAll(new CrossLinkedPeptideInfo(modifiedSequence).getAllSequences(), getProteinsFromRun(ctx), ctx.get(getModificationFieldKey(), String.class));
    }

    protected @NotNull String formatSequence(CrossLinkedPeptideInfo.PeptideSequence sequence, List<Protein> proteins, String modification)
    {
        return sequence.findMatches(proteins).stream()
                .map(match -> _formatter.format(match, sequence, modification))
                .collect(Collectors.joining("; "));
    }

    private List<Protein> getProteinsFromRun(RenderContext ctx)
    {
        Long runId = ctx.get(getRunIdFieldKey(), Long.class);
        if (runId != null)
        {
            return _proteins.computeIfAbsent(runId, x -> PeptideGroupManager.getProteinsForRun(runId));
        }
        return Collections.emptyList();
    }

    protected FieldKey getModifiedSequenceFieldKey()
    {
        return FieldKey.fromString(getColumnInfo().getFieldKey().getParent(), "PeptideModifiedSequence");
    }

    private FieldKey getModificationFieldKey()
    {
        return FieldKey.fromString(getColumnInfo().getFieldKey().getParent(), "Modification");
    }

    private FieldKey getRunIdFieldKey()
    {
        return FieldKey.fromString(getColumnInfo().getFieldKey().getParent(), "RunId");
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(getModifiedSequenceFieldKey());
        keys.add(getModificationFieldKey());
        keys.add(getRunIdFieldKey());
    }

    /** Renders one value per cross-linked peptide, so the Excel export puts each on its own row */
    private static class PerPeptideColumn extends CrossLinkedPeptideDisplayColumn implements CrossLinkedPeptideExcelWriter.PerPeptideColumn
    {
        private PerPeptideColumn(ColumnInfo col, Formatter formatter)
        {
            super(col, formatter);
        }

        @Override
        public int getPeptideCount(RenderContext ctx)
        {
            String modifiedSequence = ctx.get(getModifiedSequenceFieldKey(), String.class);
            return modifiedSequence == null ? 1 : new CrossLinkedPeptideInfo(modifiedSequence).getAllSequences().size();
        }

        @Override
        protected @NotNull String render(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification, @Nullable Integer peptideIndex)
        {
            if (peptideIndex == null)
            {
                return super.render(sequences, proteins, modification, null);
            }
            return peptideIndex < sequences.size() ? formatSequence(sequences.get(peptideIndex), proteins, modification) : "";
        }
    }

    /** Splits across the Excel rows only when the cross-linked peptides are on different chains */
    private static class ChainColumn extends PerPeptideColumn
    {
        private ChainColumn(ColumnInfo col)
        {
            super(col, (match, _, _) -> match.protein().getLabel() == null ? match.protein().getName() : match.protein().getLabel());
        }

        @Override
        public boolean isSplit(RenderContext ctx)
        {
            return getDistinctChains(getPeptideValues(ctx)).size() > 1;
        }

        @Override
        protected @NotNull String render(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification, @Nullable Integer peptideIndex)
        {
            List<String> chains = getDistinctChains(formatAll(sequences, proteins, modification));
            if (peptideIndex == null || chains.size() > 1)
            {
                return super.render(sequences, proteins, modification, peptideIndex);
            }
            return String.join("\n", chains);
        }

        private static List<String> getDistinctChains(List<String> values)
        {
            return values.stream().filter(value -> !value.isEmpty()).distinct().toList();
        }
    }

    public static class ChainFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new ChainColumn(colInfo);
        }
    }

    public static class PeptideLocationFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new PerPeptideColumn(colInfo, (match, sequence, modification) ->
                (match.index() + 1) + "-" + (match.index() + getPeptideLength(sequence.getUnmodified(), modification)));
        }
    }

    private static int getPeptideLength(String unmodifiedSequence, String modification)
    {
        // If this is a clipped peptide, it's actual length is one shorter than its amino acid sequence
        if (modification != null && modification.toLowerCase().contains("c-term lys clipping"))
        {
            return unmodifiedSequence.length() - 1;
        }
        return unmodifiedSequence.length();
    }

    public static class PeptideIdentityFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new PerPeptideColumn(colInfo, (match, sequence, modification) ->
            {
                String label = match.protein().getLabel();
                String prefix = getChainPrefix(label);
                return prefix + (match.index() + 1) + "-" + (match.index() + getPeptideLength(sequence.getUnmodified(), modification));
            });
        }
    }

    public static @NotNull String getChainPrefix(String label)
    {
        String result = "";
        if (label != null)
        {
            label = label.toLowerCase();
            if (label.endsWith("_hc"))
            {
                result = "H";
            }
            if (label.endsWith("_hcstar"))
            {
                result = "H*";
            }
            if (label.endsWith("_lc"))
            {
                result = "L";
            }
        }
        return result;
    }

    public static class BondLocationFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new CrossLinkedPeptideDisplayColumn(colInfo, null)
            {
                @Override
                protected @NotNull String render(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification, @Nullable Integer peptideIndex)
                {
                    List<List<String>> allBonds = new ArrayList<>();

                    for (CrossLinkedPeptideInfo.PeptideSequence sequence : sequences)
                    {
                        List<CrossLinkedPeptideInfo.Match> matches = sequence.findMatches(proteins);
                        List<String> bonds = new ArrayList<>();
                        for (CrossLinkedPeptideInfo.Match match : matches)
                        {
                            for (int linkIndex : sequence.getLinkIndices())
                            {
                                bonds.add(Character.toString(sequence.getUnmodified().charAt(linkIndex)) + (match.index() + linkIndex + 1) + getChainPrefix(match.protein().getLabel()));
                            }
                        }
                        allBonds.add(bonds);
                    }

                    List<String> combinations = generateCombinations(allBonds);

                    return StringUtils.join(combinations, "/\n");
                }
            };
        }
    }

    private static void generateCombinationsRecursive(List<List<String>> inputLists, List<String> results, int depth, StringBuilder currentCombination)
    {
        // Base case: if we have reached the end of the input lists, add the combination to results
        if (depth == inputLists.size())
        {
            results.add(currentCombination.toString());
            return;
        }

        // Iterate over the current list at this depth
        for (String value : inputLists.get(depth))
        {
            int oldLength = currentCombination.length();
            if (oldLength != 0)
            {
                currentCombination.append("-");
            }
            currentCombination.append(value); // Append the current value to the combination
            generateCombinationsRecursive(inputLists, results, depth + 1, currentCombination); // Recursively go to the next depth
            currentCombination.setLength(oldLength); // Undo the append operation for backtracking
        }
    }

    public static List<String> generateCombinations(List<List<String>> inputLists)
    {
        List<String> results = new ArrayList<>();
        if (inputLists == null || inputLists.isEmpty())
        {
            return results;
        }

        // Helper method to recursively generate combinations
        generateCombinationsRecursive(inputLists, results, 0, new StringBuilder());
        return results;
    }


    public static class TestCase
    {
        @Test
        public void testGenerateCombinationsThreeDoubles()
        {
            List<List<String>> inputLists = new ArrayList<>();
            inputLists.add(List.of("A", "B"));
            inputLists.add(List.of("1", "2"));
            inputLists.add(List.of("X", "Y"));

            List<String> expected = List.of(
                    "A-1-X", "A-1-Y",
                    "A-2-X", "A-2-Y",
                    "B-1-X", "B-1-Y",
                    "B-2-X", "B-2-Y"
            );

            List<String> actual = generateCombinations(inputLists);
            Assert.assertEquals(expected, actual);
        }

        @Test
        public void testGenerateCombinationsTwoDoubles()
        {
            List<List<String>> inputLists = new ArrayList<>();
            inputLists.add(List.of("A", "B"));
            inputLists.add(List.of("1", "2"));

            List<String> expected = List.of(
                    "A-1", "A-2",
                    "B-1", "B-2"
            );

            List<String> actual = generateCombinations(inputLists);
            Assert.assertEquals(expected, actual);
        }

        @Test
        public void testGenerateCombinationsMixed()
        {
            List<List<String>> inputLists = new ArrayList<>();
            inputLists.add(List.of("A", "B"));
            inputLists.add(List.of("1"));

            List<String> expected = List.of(
                    "A-1", "B-1"
            );

            List<String> actual = generateCombinations(inputLists);
            Assert.assertEquals(expected, actual);
        }

    }
}
