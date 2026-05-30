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
package org.labkey.targetedms.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.targetedms.TargetedMSSchema;

public class QCTraceMetricValuesTable extends TargetedMSTable
{
    public QCTraceMetricValuesTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_QC_TRACE_METRIC_VALUES), schema, cf, TargetedMSSchema.ContainerJoinType.SampleFileFK);
        TargetedMSTable.fixupLookups(this);
    }
}
