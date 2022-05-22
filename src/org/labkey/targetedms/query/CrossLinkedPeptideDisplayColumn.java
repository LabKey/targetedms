package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.view.CrossLinkedPeptideInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Renders the cross-linked peptide info in a few different variants. For unlinked peptides, passes through the primary
 * value from the underlying SQL query. Calculates cross-linking info by parsing a sibling column, ModifiedSequence
 */
public class CrossLinkedPeptideDisplayColumn extends DataColumn
{
    // Cache the proteins for a given run to avoid many redundant queries
    private final Map<Long, List<PeptideGroup>> _proteins = new HashMap<>();
    private final BiFunction<PeptideGroup, CrossLinkedPeptideInfo.PeptideSequence, String> _formatter;

    public CrossLinkedPeptideDisplayColumn(ColumnInfo col, BiFunction<PeptideGroup, CrossLinkedPeptideInfo.PeptideSequence, String> formatter)
    {
        super(col);
        setNoWrap(true);
        _formatter = formatter;
    }

    @Override
    public Object getDisplayValue(RenderContext ctx)
    {
        return getValue(ctx);
    }

    @Override
    public Object getValue(RenderContext ctx)
    {
        Object defaultValue = super.getValue(ctx);

        String modifiedSequence = ctx.get(getModifiedSequenceFieldKey(), String.class);
        if (modifiedSequence != null)
        {
            CrossLinkedPeptideInfo i = new CrossLinkedPeptideInfo(modifiedSequence);
            if (i.isCrosslinked())
            {
                List<PeptideGroup> proteins = getProteinsFromRun(ctx);

                StringBuilder result = new StringBuilder();
                String separator = "";

                for (CrossLinkedPeptideInfo.PeptideSequence sequence : i.getAllSequences())
                {
                    PeptideGroup protein = sequence.findMatch(proteins);
                    if (protein != null)
                    {
                        result.append(separator);
                        separator = "\n";
                        result.append(_formatter.apply(protein, sequence));
                    }
                }
                return result.toString();
            }
        }
        return defaultValue;
    }

    private List<PeptideGroup> getProteinsFromRun(RenderContext ctx)
    {
        Long runId = ctx.get(getRunIdFieldKey(), Long.class);
        if (runId != null)
        {
            return _proteins.computeIfAbsent(runId, x -> PeptideGroupManager.getPeptideGroupsForRun(runId));
        }
        return Collections.emptyList();
    }

    private FieldKey getModifiedSequenceFieldKey()
    {
        return FieldKey.fromString(getColumnInfo().getFieldKey().getParent(), "PeptideModifiedSequence");
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
        keys.add(getRunIdFieldKey());
    }

    public static class ChainFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new CrossLinkedPeptideDisplayColumn(colInfo, (protein, sequence) -> protein.getLabel() == null ? protein.getName() : protein.getLabel());
        }
    }

    public static class PeptideLocationFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new CrossLinkedPeptideDisplayColumn(colInfo, (protein, sequence) ->
            {
                int startIndex = protein.getSequence().indexOf(sequence.getUnmodified());
                return (startIndex + 1) + "-" + (startIndex + sequence.getUnmodified().length());
            });
        }
    }

    public static class BondLocationFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new CrossLinkedPeptideDisplayColumn(colInfo, (protein, sequence) ->
            {
                StringBuilder result = new StringBuilder();
                String separator = "";
                for (int linkIndex : sequence.getLinkIndices())
                {
                    result.append(separator);
                    separator = ", ";
                    int startIndex = protein.getSequence().indexOf(sequence.getUnmodified());
                    result.append(sequence.getUnmodified().charAt(linkIndex));
                    result.append(startIndex + linkIndex + 1);
                }
                return result.toString();
            });
        }
    }

}
