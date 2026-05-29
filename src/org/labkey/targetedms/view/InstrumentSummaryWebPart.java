/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
            var runId = propertyValues.getPropertyValue("id");
            if (null != runId && runId.getValue() != null)
            {
                try
                {
                    instrumentSummaryQS.setBaseFilter(new SimpleFilter(FieldKey.fromString("runId"), Long.valueOf(runId.getValue().toString())));
                    // Avoid perf problems doing a cross-folder query when all data is scoped to this container
                    setAllowableContainerFilterTypes();
                    instrumentSummaryQS.setContainerFilterName(null);
                }
                catch (NumberFormatException ignored) {}
            }
        }
        setSettings(instrumentSummaryQS);
        setTitle("Instrument Summary");
        setShowDetailsColumn(false);

        setShowBorders(true);
        setShadeAlternatingRows(true);
        setContainerFilter(null);
    }

}
