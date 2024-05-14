package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Customizes the set of columns available on a pivot query that operates on samples, hiding all the pivot values
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
        var stressedSamples = getSampleMetadata(tableInfo.getUserSchema().getContainer());
        long runId = getRunId(tableInfo);

        List<Protein> proteins = PeptideGroupManager.getProteinsForRun(runId);
        Function<Long, Protein> proteinGetter = (peptideGroupId) -> {
            Optional<Protein> opt = proteins.stream().filter(p -> p.getPeptideGroupId() == peptideGroupId.intValue()).findFirst();
            return opt.orElse(null);
        };

        for (ColumnInfo c : tableInfo.getColumns())
        {
            MutableColumnInfo col = (MutableColumnInfo) c;

            if (col.getName().endsWith("::TotalPercentModified") || col.getName().endsWith("::PercentModified"))
            {
                col.setFormat("0.0%");
                col.setDisplayColumnFactory(new CDRConditionalFormattingDisplayColumnFactory(proteinGetter, stressedSamples, runId));
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

    public static long getRunId(TableInfo tableInfo)
    {
        // Table name should be PTMPercentsGrouped_X, where X is the runId
        String tableName = tableInfo.getName();
        if (tableName.toLowerCase().startsWith(TargetedMSSchema.QUERY_PTM_PERCENTS_GROUPED_PREFIX.toLowerCase()))
        {
            try
            {
                return Long.parseLong(tableName.substring(TargetedMSSchema.QUERY_PTM_PERCENTS_GROUPED_PREFIX.length()));
            }
            catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /** Key is the sample name and run ID, value is a pair of boolean (stressed or not) and description, both annotation-based */
    public static Map<Pair<String, Long>, Pair<Boolean, String>> getSampleMetadata(Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT sf.SampleName, r.RunId, raStressed.Value AS Stressed, COALESCE(raDescription.Value, sf.SampleName) AS Description FROM ");
        sql.append(TargetedMSManager.getTableInfoSampleFile(), "sf");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicate(), "r");
        sql.append(" ON r.Id = sf.ReplicateId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" ON r.RunId = run.Id AND run.Container = ?");
        sql.add(container);
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "raStressed");
        sql.append(" ON r.Id = raStressed.ReplicateId AND raStressed.Name = 'Stressed or Non-stressed'");
        sql.append(" LEFT OUTER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "raDescription");
        sql.append(" ON r.Id = raDescription.ReplicateId AND raDescription.Name = 'Sample Description'");
        Map<Pair<String, Long>, Pair<Boolean, String>> result = new HashMap<>();
        new SqlSelector(TargetedMSManager.getSchema(), sql).forEach(rs -> {
            String sampleName = rs.getString("SampleName");
            Long runId = rs.getLong("RunId");
            Boolean stressed = "Stressed".equalsIgnoreCase(rs.getString("Stressed"));
            String description = rs.getString("Description");
            result.put(Pair.of(sampleName, runId), Pair.of(stressed, description));
        });
        return result;
    }

    /** Look up proteins on demand by peptideGroupId, caching them for reuse */
    public static class PeptideGroupIdProteinGetter implements Function<Long, Protein>
    {
        private final Map<Long, Protein> _proteins = new HashMap<>();
        @Override
        public Protein apply(Long peptideGroupId)
        {
            return _proteins.computeIfAbsent(peptideGroupId, PeptideGroupManager::getProteinForPeptideGroupId);
        }
    }
}
