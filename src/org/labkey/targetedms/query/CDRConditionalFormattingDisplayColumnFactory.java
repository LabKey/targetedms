package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ConditionalFormat;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.parser.Protein;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class CDRConditionalFormattingDisplayColumnFactory implements DisplayColumnFactory
{
    private final Function<Long, Protein> _proteinGetter;
    private final Map<Pair<String, Long>, Pair<Boolean, String>> _stressedSamples;
    private final long _runId;

    public CDRConditionalFormattingDisplayColumnFactory(Function<Long, Protein> proteinGetter, Map<Pair<String, Long>, Pair<Boolean, String>> stressedSamples, long runId)
    {
        _proteinGetter = proteinGetter;
        _stressedSamples = stressedSamples;
        _runId = runId;
    }

    public static RiskLevel getRiskLevel(Number value, boolean inCDR, boolean stressed)
    {
        if (value == null)
        {
            return null;
        }

        double highCutoff = inCDR ? 0.1 : 0.3;
        double mediumCutoff = inCDR ? 0.05 : 0.15;

        if (stressed)
        {
            highCutoff *= 2;
            mediumCutoff *= 2;
        }

        if (value.doubleValue() > highCutoff)
        {
            return RiskLevel.High;
        }
        else if (value.doubleValue() > mediumCutoff)
        {
            return RiskLevel.Medium;
        }
        return RiskLevel.Low;

    }

    public static boolean isInCDR(FieldKey parentFieldKey, RenderContext ctx, Function<Long, Protein> proteinGetter)
    {
        boolean inCDR = false;
        Long peptideGroupId = ctx.get(FieldKey.fromString(parentFieldKey, "PeptideGroupId"), Long.class);
        Integer siteLocation = ctx.get(FieldKey.fromString(parentFieldKey, "Location"), Integer.class);
        if (peptideGroupId != null && siteLocation != null)
        {
            int modificationIndex = siteLocation.intValue();
            Protein protein = proteinGetter.apply(peptideGroupId);
            if (protein != null)
            {
                for (Pair<Integer, Integer> cdrRange : protein.getCdrRangesList())
                {
                    if (cdrRange.first <= modificationIndex && cdrRange.second >= modificationIndex)
                    {
                        inCDR = true;
                        break;
                    }
                }
            }
        }
        return inCDR;
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo boundCol)
    {
        return new Col(boundCol, _proteinGetter, _stressedSamples, _runId);
    }

    public enum RiskLevel
    {
        Low("89ca53"), // Green for low risk
        Medium("feff3f"), // Yellow
        High("fa081a"); // Red

        private final String _color;

        RiskLevel(String color)
        {
            _color = color;
        }

        public String getColor()
        {
            return _color;
        }
    }

    /** Chooses the background color based the percentage compared to low/medium/high range definitions
     * based on whether sample described by this pivot column is "stressed" and whether the modification is within a
     * CDR (complementarity-determining region) */
    private static class Col extends DataColumn
    {
        private final Function<Long, Protein> _proteinGetter;
        private final Map<Pair<String, Long>, Pair<Boolean, String>> _stressedSamples;
        private final long _runId;

        public Col(ColumnInfo colInfo, Function<Long, Protein> proteinGetter, Map<Pair<String, Long>, Pair<Boolean, String>> stressedSamples, long runId)
        {
            super(colInfo);
            _proteinGetter = proteinGetter;
            _stressedSamples = stressedSamples;
            _runId = runId;
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Location"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "Modification"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "RunId"));
        }

        @NotNull
        private ConditionalFormat createConditionalFormat(Number value, boolean inCDR, boolean stressed)
        {
            RiskLevel level = getRiskLevel(value, inCDR, stressed);

            ConditionalFormat result = new ConditionalFormat()
            {
                @Override
                public SimpleFilter getSimpleFilter()
                {
                    SimpleFilter result = new SimpleFilter();
                    result.addClause(new CompareType.CompareClause(getBoundColumn().getFieldKey(), CompareType.GT, value)
                    {
                        @Override
                        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
                        {
                            sb.append("the peptide is ");
                            sb.append(inCDR ? "" : "not ");
                            sb.append("part of a CDR sequence and observed in a ");
                            sb.append(stressed ? "" : "non-");
                            sb.append("stressed sample");
                        }
                    });
                    return result;
                }
            };
            result.setBackgroundColor(level.getColor());
            return result;
        }

        @Override
        protected @Nullable ConditionalFormat findApplicableFormat(RenderContext ctx)
        {
            Number value = ctx.get(getBoundColumn().getFieldKey(), Number.class);
            if (value == null)
            {
                return null;
            }

            String modification = ctx.get(new FieldKey(getBoundColumn().getFieldKey().getParent(), "Modification"), String.class);
            if ("Gln->pyro-Glu (N-term Q)".equalsIgnoreCase(modification))
            {
                return null;
            }

            boolean inCDR = isInCDR(getBoundColumn().getFieldKey().getParent(), ctx, _proteinGetter);

            String sampleName = getBoundColumn().getName();
            if (sampleName.contains("::"))
            {
                // Strip off the suffix to get to the sample name
                sampleName = sampleName.substring(0, sampleName.indexOf("::"));
            }
            Pair<Boolean, String> metadata = _stressedSamples.get(Pair.of(sampleName, _runId));
            return createConditionalFormat(value, inCDR, metadata != null && metadata.first.booleanValue());
        }
    }

}
