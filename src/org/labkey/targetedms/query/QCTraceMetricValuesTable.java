package org.labkey.targetedms.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.query.FilteredTable;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

public class QCTraceMetricValuesTable extends FilteredTable<TargetedMSSchema>
{
    public QCTraceMetricValuesTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSManager.getTableQCTraceMetricValues(), schema, cf);
        TargetedMSTable.fixupLookups(this);
        wrapAllColumns(true);
    }



}
