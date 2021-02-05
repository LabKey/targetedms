package org.labkey.targetedms.view;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;

public class InstrumentSummaryWebPart extends QueryView
{
    public InstrumentSummaryWebPart (ViewContext viewContext)
    {
        super(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()));
        QuerySettings instrumentSummaryQS = new QuerySettings(getViewContext(), "InstrumentSummary", "QCInstrumentSummary");
        var propertyValues = getBindPropertyValues();
        if (null != propertyValues)
        {
            var runId = propertyValues.getPropertyValue("id").getValue().toString();
            instrumentSummaryQS.setBaseFilter(new SimpleFilter(FieldKey.fromString("runId"), Long.valueOf(runId)));
        }
        setSettings(instrumentSummaryQS);
        setTitle("Instruments Summary");
        setShowDetailsColumn(false);

        setShowBorders(true);
        setShadeAlternatingRows(true);
        setContainerFilter(null);
    }

}
