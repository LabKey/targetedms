package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ConditionalFormat;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Protein;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Customizes the set of columns available on a pivot query that operates on samples, hiding all of the pivot values
 * that aren't part of the run that's being filtered on. This lets you view a single document's worth of data
 * without seeing empty columns for all the other samples in the same container.
 */
public class PTMPercentsGroupedCustomizer extends PTMPercentsCustomizer
{
    /** Referenced from query XML metadata */
    @SuppressWarnings("unused")
    public PTMPercentsGroupedCustomizer(MultiValuedMap<String, String> props)
    {
        super(props);
    }

    private FieldKey getCountFieldKey(ColumnInfo columnInfo)
    {
        return FieldKey.fromString(columnInfo.getFieldKey().getParent(), "ModificationCount");
    }

    /** These are the sorts we need to span rows, because we know that the related rows will be adjacent to each other */
    public static final List<FieldKey> EXPECTED_SORTS = Collections.unmodifiableList(Arrays.asList(
            FieldKey.fromParts("PeptideGroupId"),
            FieldKey.fromParts("Location"),
            FieldKey.fromParts("Sequence")
    ));

    /** Sorts that are safe because they overlap with the expected sorts in terms of grouping rows together */
    public static final List<FieldKey> ALLOWABLE_SORTS = Collections.unmodifiableList(Arrays.asList(
            FieldKey.fromParts("PeptideGroupId", "Label"),
            FieldKey.fromParts("SiteLocation"),
            FieldKey.fromParts("AminoAcid")
    ));

    private int getRowSpan(ColumnInfo columnInfo, RenderContext ctx)
    {
        List<Sort.SortField> sortFields = ctx.getBaseSort().getSortList();

        // Only span rows if we're sorted as expected. Related to ticket 48873
        List<FieldKey> neededSorts = new ArrayList<>(EXPECTED_SORTS);
        for (Sort.SortField sortField : sortFields)
        {
            neededSorts.remove(sortField.getFieldKey());
            if (neededSorts.isEmpty())
            {
                break;
            }
            if (!EXPECTED_SORTS.contains(sortField.getFieldKey()) && !ALLOWABLE_SORTS.contains(sortField.getFieldKey()))
            {
                return 1;
            }
        }

        Integer targetCount = ctx.get(getCountFieldKey(columnInfo), Integer.class);
        if (targetCount != null && targetCount > 1)
        {
            return targetCount;
        }
        return 1;
    }

    @Override
    public void customize(TableInfo tableInfo)
    {
        Map<String, Pair<Boolean, String>> stressedSamples = getSampleMetadata(tableInfo);

        List<Protein> proteins = getProteins(tableInfo);

        for (ColumnInfo c : tableInfo.getColumns())
        {
            MutableColumnInfo col = (MutableColumnInfo) c;

            if (col.getName().endsWith("::TotalPercentModified") || col.getName().endsWith("::PercentModified"))
            {
                col.setFormat("0.0%");
                col.setDisplayColumnFactory((boundCol) -> new CDRConditionalFormatDisplayColumn(boundCol, proteins, stressedSamples));
            }

            if (col.getName().endsWith("::TotalPercentModified") ||
                    col.getName().equalsIgnoreCase("PeptideGroupId") ||
                    col.getName().equalsIgnoreCase("PeptideModifiedSequence") ||
                    col.getName().equalsIgnoreCase("MaxPercentModified") ||
                    col.getName().equalsIgnoreCase("AminoAcid") ||
                    col.getName().equalsIgnoreCase("Location") ||
                    col.getName().equalsIgnoreCase("SiteLocation"))
            {

                DisplayColumnFactory originalFactory = col.getDisplayColumnFactory();
                DisplayColumnFactory factory = colInfo -> {
                    DisplayColumn displayColumn = originalFactory.createRenderer(colInfo);
                    displayColumn.setRowSpanner(new DisplayColumn.RowSpanner()
                    {
                        private int _count;
                        @Override
                        public int getRowSpan(RenderContext ctx)
                        {
                            return PTMPercentsGroupedCustomizer.this.getRowSpan(col, ctx);
                        }

                        @Override
                        public void addQueryColumns(Set<FieldKey> fieldKeys)
                        {
                            fieldKeys.add(getCountFieldKey(col));
                        }

                        @Override
                        public boolean shouldRenderInCurrentRow(RenderContext ctx)
                        {
                            int targetCount = PTMPercentsGroupedCustomizer.this.getRowSpan(col, ctx);
                            if (targetCount > 1)
                            {
                                _count++;
                                if (_count == 1)
                                {
                                    return true;
                                }
                                if (_count == targetCount)
                                {
                                    _count = 0;
                                }
                                return false;
                            }
                            return true;
                        }
                    });
                    return displayColumn;
                };
                col.setDisplayColumnFactory(factory);
            }
        }

        super.customize(tableInfo);
    }

    @NotNull
    private static List<Protein> getProteins(TableInfo tableInfo)
    {
        List<Protein> proteins = Collections.emptyList();

        // Table name should be PTMPercentsGrouped_X, where X is the runId
        String tableName = tableInfo.getName();
        if (tableName.toLowerCase().startsWith(TargetedMSSchema.QUERY_PTM_PERCENTS_GROUPED_PREFIX.toLowerCase()))
        {
            try
            {
                long runId = Long.parseLong(tableName.substring(TargetedMSSchema.QUERY_PTM_PERCENTS_GROUPED_PREFIX.length()));
                proteins = PeptideGroupManager.getProteinsForRun(runId);
            }
            catch (NumberFormatException ignored) {}
        }
        return proteins;
    }

    /** Key is the sample name, value is a pair of boolean (stressed or not) and description, both annotation-based */
    public static Map<String, Pair<Boolean, String>> getSampleMetadata(TableInfo tableInfo)
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT sf.SampleName, raStressed.Value AS Stressed, COALESCE(raDescription.Value, sf.SampleName) AS Description FROM ");
        sql.append(TargetedMSManager.getTableInfoSampleFile(), "sf");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicate(), "r");
        sql.append(" ON r.Id = sf.ReplicateId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" ON r.RunId = run.Id AND run.Container = ?");
        sql.add(tableInfo.getUserSchema().getContainer());
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "raStressed");
        sql.append(" ON r.Id = raStressed.ReplicateId AND raStressed.Name = 'Stressed or Non-stressed'");
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "raDescription");
        sql.append(" ON r.Id = raDescription.ReplicateId AND raDescription.Name = 'Sample Description'");
        CaseInsensitiveHashMap<Pair<Boolean, String>> result = new CaseInsensitiveHashMap<>();
        new SqlSelector(TargetedMSManager.getSchema(), sql).forEach(rs -> {
            String sampleName = rs.getString("SampleName");
            Boolean stressed = "Stressed".equalsIgnoreCase(rs.getString("Stressed"));
            String description = rs.getString("Description");
            result.put(sampleName, Pair.of(stressed, description));
        });
        return result;
    }

    /** Chooses the background color based the percentage compared to low/medium/high range definitions
     * based on whether sample described by this pivot column is "stressed" and whether the modification is within a
     * CDR (complementarity-determining region) */
    private static class CDRConditionalFormatDisplayColumn extends DataColumn
    {
        private final List<Protein> _proteins;
        private final Map<String, Pair<Boolean, String>> _stressedSamples;

        public CDRConditionalFormatDisplayColumn(ColumnInfo colInfo, List<Protein> proteins, Map<String, Pair<Boolean, String>> stressedSamples)
        {
            super(colInfo);
            _proteins = proteins;
            _stressedSamples = stressedSamples;
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"));
            keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "SiteLocation"));
        }

        @NotNull
        private ConditionalFormat createConditionalFormat(Number value, boolean inCDR, boolean stressed)
        {
            double highCutoff = inCDR ? 0.1 : 0.3;
            double mediumCutoff = inCDR ? 0.05 : 0.15;

            if (stressed)
            {
                highCutoff *= 2;
                mediumCutoff *= 2;
            }

            String backgroundColor = "89ca53"; // Green for low risk
            if (value.doubleValue() > highCutoff)
            {
                backgroundColor = "fa081a"; // Red
            }
            else if (value.doubleValue() > mediumCutoff)
            {
                backgroundColor = "feff3f"; // Yellow
            }
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
            result.setBackgroundColor(backgroundColor);
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

            boolean inCDR = false;
            Long peptideGroupId = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"), Long.class);
            String siteLocation = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "SiteLocation"), String.class);
            if (peptideGroupId != null && siteLocation != null)
            {
                // Strip off the leading letter, which is the amino acid, leaving us with
                // the one-based index within the full protein sequence
                int modificationIndex = Integer.parseInt(siteLocation.substring(1));
                Optional<Protein> match = _proteins.stream().filter(p -> p.getPeptideGroupId() == peptideGroupId.intValue()).findFirst();
                if (match.isPresent())
                {
                    for (Pair<Integer, Integer> cdrRange : match.get().getCdrRangesList())
                    {
                        if (cdrRange.first <= modificationIndex && cdrRange.second >= modificationIndex)
                        {
                            inCDR = true;
                            break;
                        }
                    }
                }
            }

            String sampleName = getBoundColumn().getName();
            if (sampleName.contains("::"))
            {
                // Strip off the suffix to get to the sample name
                sampleName = sampleName.substring(0, sampleName.indexOf("::"));
            }
            Pair<Boolean, String> metadata = _stressedSamples.get(sampleName);
            return createConditionalFormat(value, inCDR, metadata != null && metadata.first.booleanValue());
        }
    }
}
