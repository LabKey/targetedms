package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ConditionalFormat;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Protein;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Customizes the set of columns available on a pivot query that operates on samples, hiding all of the pivot values
 * that aren't part of the run that's being filtered on. This lets you view a single document's worth of data
 * without seeing empty columns for all of the other samples in the same container.
 */
public class PTMPercentsGroupedCustomizer implements TableCustomizer
{

    private static final String QUERY_NAME_PREFIX = "PTMPercentsGrouped";

    /** Referenced from query XML metadata */
    @SuppressWarnings("unused")
    public PTMPercentsGroupedCustomizer(MultiValuedMap<String, String> props)
    {

    }

    private FieldKey getCountFieldKey(ColumnInfo columnInfo)
    {
        return FieldKey.fromString(columnInfo.getFieldKey().getParent(), "ModificationCount");
    }

    private int getRowSpan(ColumnInfo columnInfo, RenderContext ctx)
    {
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
        Set<String> stressedSamples = getStressedSamples(tableInfo);

        List<Protein> proteins = getProteins(tableInfo);

        for (ColumnInfo c : tableInfo.getColumns())
        {
            MutableColumnInfo col = (MutableColumnInfo) c;

            if (col.getName().endsWith("::TotalPercentModified") || col.getName().endsWith("::PercentModified"))
            {
                col.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    @Override
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        return new DataColumn(colInfo)
                        {

                            @Override
                            public void addQueryFieldKeys(Set<FieldKey> keys)
                            {
                                super.addQueryFieldKeys(keys);
                                keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"));
                                keys.add(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "SiteLocation"));
                            }

                            @Override
                            protected @Nullable ConditionalFormat findApplicableFormat(RenderContext ctx)
                            {
                                Number value = ctx.get(colInfo.getFieldKey(), Number.class);
                                if (value == null)
                                {
                                    return null;
                                }

                                boolean inCDR = false;
                                Long peptideGroupId = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "PeptideGroupId"), Long.class);
                                String siteLocation = ctx.get(FieldKey.fromString(getBoundColumn().getFieldKey().getParent(), "SiteLocation"), String.class);
                                if (peptideGroupId != null && siteLocation != null)
                                {
                                    // Strip off the leading letter, which is the amino acid, to get the one-based index with the full protein sequence
                                    int modificationIndex = Integer.parseInt(siteLocation.substring(1));
                                    Optional<Protein> match = proteins.stream().filter(p -> p.getPeptideGroupId() == peptideGroupId.intValue()).findFirst();
                                    if (match.isPresent())
                                    {
                                        for (Pair<Integer, Integer> cdrRange : match.get().getCdrRanges())
                                        {
                                            if (cdrRange.first <= modificationIndex && cdrRange.second >= modificationIndex)
                                            {
                                                inCDR = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                String sampleName = colInfo.getName();
                                if (sampleName.contains("::"))
                                {
                                    sampleName = sampleName.substring(0, sampleName.indexOf("::"));
                                }
                                return createConditionalFormat(value, inCDR, stressedSamples.contains(sampleName));
                            }
                        };
                    }
                });
            }

            if (col.getName().endsWith("::TotalPercentModified") ||
                    col.getName().equalsIgnoreCase("PeptideGroupId") ||
                    col.getName().equalsIgnoreCase("PeptideModifiedSequence") ||
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
                        public boolean shouldRender(RenderContext ctx)
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
    }

    @NotNull
    private static ConditionalFormat createConditionalFormat(Number value, boolean inCDR, boolean stressed)
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
                result.addClause(new SimpleFilter.FilterClause()
                {
                    @Override
                    public List<FieldKey> getFieldKeys()
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
                    {
                        sb.append("the peptide is ");
                        sb.append(inCDR ? "" : "not ");
                        sb.append("part of CDR and observed in a ");
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

    @NotNull
    private static List<Protein> getProteins(TableInfo tableInfo)
    {
        List<Protein> proteins = Collections.emptyList();

        // Table name should be PTMPercentsGroupedX, where X is the runId
        String tableName = tableInfo.getName();
        if (tableName.toLowerCase().startsWith(QUERY_NAME_PREFIX.toLowerCase()))
        {
            try
            {
                int runId = Integer.parseInt(tableName.substring(QUERY_NAME_PREFIX.length()));
                proteins = PeptideGroupManager.getProteinsForRun(runId);
            }
            catch (NumberFormatException ignored) {}
        }
        return proteins;
    }

    private Set<String> getStressedSamples(TableInfo tableInfo)
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT sf.SampleName FROM ");
        sql.append(TargetedMSManager.getTableInfoSampleFile(), "sf");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicate(), "r");
        sql.append(" ON r.Id = sf.ReplicateId INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "ra");
        sql.append(" ON r.Id = ra.ReplicateId AND ra.Name = 'Stressed or Non-stressed' AND ra.Value = 'Stressed'");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" ON r.RunId = run.Id AND run.Container = ?");
        sql.add(tableInfo.getUserSchema().getContainer());
        return new CaseInsensitiveHashSet(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(String.class));
    }
}
