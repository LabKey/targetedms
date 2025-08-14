package org.labkey.targetedms.query;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            return render(sequences, proteins, modification);
        }
        return defaultValue;
    }

    protected @NotNull String render(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification)
    {
        StringBuilder result = new StringBuilder();
        String outerSeparator = "";

        for (CrossLinkedPeptideInfo.PeptideSequence sequence : sequences)
        {
            List<CrossLinkedPeptideInfo.Match> matches = sequence.findMatches(proteins);
            if (!matches.isEmpty())
            {
                result.append(outerSeparator);
                outerSeparator = "\n";

                String innerSeparator = "";
                for (CrossLinkedPeptideInfo.Match match : matches)
                {
                    result.append(innerSeparator);
                    innerSeparator = "; ";
                    result.append(_formatter.format(match, sequence, modification));
                }
            }
        }
        return result.toString();
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

    private FieldKey getModifiedSequenceFieldKey()
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

    public static class ChainFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new CrossLinkedPeptideDisplayColumn(colInfo, (match, sequence, modification) -> match.protein().getLabel() == null ? match.protein().getName() : match.protein().getLabel());
        }
    }

    public static class PeptideLocationFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new CrossLinkedPeptideDisplayColumn(colInfo, (match, sequence, modification) ->
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
            return new CrossLinkedPeptideDisplayColumn(colInfo, (match, sequence, modification) ->
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
                protected @NotNull String render(List<CrossLinkedPeptideInfo.PeptideSequence> sequences, List<Protein> proteins, String modification)
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
