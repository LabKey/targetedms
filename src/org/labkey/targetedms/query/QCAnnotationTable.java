/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.gwt.client.AuditBehaviorType;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.labkey.targetedms.query.GuideSetTable.appendFormatLabel;

/**
* Created by: jeckels
* Date: 12/7/14
*/
public class QCAnnotationTable extends SimpleUserSchema.SimpleTable<TargetedMSSchema>
{
    public QCAnnotationTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(schema, TargetedMSManager.getTableInfoQCAnnotation(), cf);

        wrapAllColumns(true);
        TargetedMSTable.fixupLookups(this);
        getMutableColumn("QCAnnotationTypeId").setFk(QueryForeignKey
                .from(schema, ContainerFilter.Type.CurrentPlusProjectAndShared.create(schema))
                .to(TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, "Id", "Name"));

        appendFormatLabel(getMutableColumn("Date"));
        appendFormatLabel(getMutableColumn("EndDate"));
        setAuditBehavior(AuditBehaviorType.DETAILED);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        TableInfo table = getRealTable();
        if (table != null)
        {
            return new DefaultQueryUpdateService(this, getRealTable())
            {
                @Override
                public List<Map<String, Object>> insertRows(User user, Container container, List<Map<String, Object>> rows, BatchValidationException errors, @Nullable Map<Enum, Object> configParameters, @Nullable Map<String, Object> extraScriptContext) throws SQLException, QueryUpdateServiceException, DuplicateKeyException
                {
                    List<Map<String, Object>> resultRows = new java.util.ArrayList<>();
                    for (Map<String, Object> row : rows)
                    {
                        // Check if the QCAnnotationType is shareable
                        int qcAnnotationTypeId = (Integer) row.get("QCAnnotationTypeId");
                        boolean isShareable = TargetedMSManager.isQCAnnotationTypeShareable(qcAnnotationTypeId);

                        if (isShareable)
                        {
                            List<TargetedMSManager.InstrumentDetails> instruments = TargetedMSManager.getInstrumentDetails(getContainer());
                            if (instruments.isEmpty())
                            {
                                resultRows.add(row);
                            }
                            else
                            {
                                for (TargetedMSManager.InstrumentDetails instrument : instruments)
                                {
                                    Map<String, Object> newRow = new java.util.HashMap<>(row);
                                    newRow.put("instrumentModel", instrument.getModel());
                                    newRow.put("instrumentSerialNumber", instrument.getInstrumentSerialNumber());
                                    newRow.put("Container", getContainer().getId());
                                    resultRows.add(newRow);
                                }
                            }
                        }
                        else
                        {
                            resultRows.add(row);
                        }
                    }

                    return super.insertRows(user, container, resultRows, errors, configParameters, extraScriptContext);
                }
            };
        }
        return null;
    }
}
