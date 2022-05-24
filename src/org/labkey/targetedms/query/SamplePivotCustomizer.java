package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Customizes the set of columns available on a pivot query that operates on samples, hiding all of the pivot values
 * that aren't part of the run that's being filtered on. This lets you view a single document's worth of data
 * without seeing empty columns for all of the other samples in the same container.
 */
public class SamplePivotCustomizer implements TableCustomizer
{
    private final FieldKey _runIdField;
    private final String _urlParameter;
    private final String _replicateNameQuery;
    private final String _replicateField;
    private final String _replicateRunIdField;

    /** Referenced from query XML metadata */
    @SuppressWarnings("unused")
    public SamplePivotCustomizer(MultiValuedMap<String, String> props)
    {
        _runIdField = FieldKey.fromString(getParameter(props, "runIdField", true));
        _urlParameter = getParameter(props, "urlParameter", false);
        _replicateNameQuery = getParameter(props, "replicateNameQuery", false);
        _replicateField = getParameter(props, "replicateField", false);
        _replicateRunIdField = getParameter(props, "replicateRunIdField", false);
    }

    private static String getParameter(MultiValuedMap<String, String> props, String propertyName, boolean required)
    {
        Collection<String> values = props.get(propertyName);
        if (values != null && values.size() == 1)
        {
            return values.iterator().next();
        }

        if (required)
        {
            throw new IllegalArgumentException("Must have exactly one property named '" + propertyName + "'");
        }
        return null;
    }

    private Long getRunId()
    {
        ViewContext context = HttpView.currentContext();

        ActionURL filterURL = context.getActionURL();

        boolean getQueryDetails = filterURL.getAction().equalsIgnoreCase("getQueryDetails");

        if (getQueryDetails)
        {
            // When getting the query metadata to customize the view, the Run ID parameter doesn't get propagated to
            // the current URL. Look for it from the referrer URL
            String referer = context.getRequest() != null ? context.getRequest().getHeader("Referer") : null;
            filterURL = referer == null ? context.getActionURL() : new ActionURL(referer);
        }

        if (_urlParameter != null)
        {
            String paramValue = filterURL.getParameter(_urlParameter);
            try
            {
                return Long.parseLong(paramValue);
            }
            catch (NumberFormatException ignored) {}
        }

        if (!getQueryDetails)
        {
            // Grab any filters that have been applied. Use QuerySettings, which will find the right values whether it's
            // a GET or POST
            QuerySettings settings = new QuerySettings(context, "query");
            filterURL = settings.getSortFilterURL();
        }

        SimpleFilter filter = new SimpleFilter(filterURL, "query");
        for (SimpleFilter.FilterClause clause : filter.getClauses())
        {
            // Look for an equals filter on the RunId column that has a value specified
            if (clause.getFieldKeys().contains(_runIdField) &&
                    clause instanceof CompareType.CompareClause &&
                    ((CompareType.CompareClause) clause).getCompareType() == CompareType.EQUAL &&
                    clause.getParamVals().length == 1 &&
                    clause.getParamVals()[0] != null)
            {
                try
                {
                    return Long.parseLong(clause.getParamVals()[0].toString());
                }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    @Override
    public void customize(TableInfo tableInfo)
    {
        if (HttpView.hasCurrentView())
        {
            Long runId = getRunId();
            if (runId != null)
            {
                ViewContext context = HttpView.currentContext();

                Set<String> sampleNames = new CaseInsensitiveHashSet();
                TargetedMSSchema schema = new TargetedMSSchema(context.getUser(), context.getContainer());
                TableInfo sampleFileTable = schema.getTable(_replicateNameQuery == null ? TargetedMSSchema.TABLE_SAMPLE_FILE : _replicateNameQuery);
                if (sampleFileTable != null)
                {
                    // Fetch the sample and/or replicate names associated with the selected run
                    Set<FieldKey> fieldKeys = _replicateField == null ?
                            Set.of(FieldKey.fromParts("SampleName"), FieldKey.fromParts("ReplicateId", "Name")) :
                            Set.of(FieldKey.fromString(_replicateField));

                    for (Map<String, Object> sampleInfo : new TableSelector(sampleFileTable,
                            QueryService.get().getColumns(sampleFileTable, fieldKeys).values(),
                            new SimpleFilter(_replicateRunIdField == null ? FieldKey.fromParts("ReplicateId", "RunId") : FieldKey.fromString(_replicateRunIdField), runId), null).getMapCollection())
                    {
                        for (Object sampleName : sampleInfo.values())
                        {
                            if (sampleName instanceof String)
                            {
                                sampleNames.add(sampleName.toString());
                            }
                        }
                    }

                    // Match the samples from the desired run against all the pivoted columns
                    List<FieldKey> defaultColumns = new ArrayList<>(tableInfo.getDefaultVisibleColumns());
                    Iterator<FieldKey> iter = defaultColumns.iterator();
                    while (iter.hasNext())
                    {
                        FieldKey column = iter.next();
                        if (column.getName().contains("::"))
                        {
                            // Pivot columns get names like MySample::PercentModified
                            String columnSampleName = column.getName().split("::")[0];

                            // We might have an exact match, or the column name might have a prefix like 'Timepoint '
                            Set<String> possibleMatches = new CaseInsensitiveHashSet();
                            possibleMatches.add(columnSampleName);
                            possibleMatches.addAll(Arrays.asList(columnSampleName.split(" ")));

                            // Kick out columns that don't match our run's set of samples
                            boolean match = false;
                            for (String possibleMatch : possibleMatches)
                            {
                                if (sampleNames.contains(possibleMatch))
                                {
                                    match = true;
                                    break;
                                }
                            }
                            if (!match)
                            {
                                iter.remove();
                            }
                        }
                    }
                    tableInfo.setDefaultVisibleColumns(defaultColumns);
                }
            }
        }
    }
}
