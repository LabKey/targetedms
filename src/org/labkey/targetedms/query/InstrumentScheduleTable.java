package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstrumentScheduleTable extends OwnProjectSchedulingTable
{
    public InstrumentScheduleTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSSchema.TABLE_INSTRUMENT_SCHEDULE, schema, cf, true);
        addTriggerFactory((c, table, extraContext) -> List.of(
            new InstrumentUsagePaymentTrigger("Id"),
            new InstrumentScheduleOverlapTrigger()
        ));
    }

    @Override
    public @NotNull QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            protected Map<String, Object> insertRow(User user, Container container, Map<String, Object> row) throws DuplicateKeyException, ValidationException, QueryUpdateServiceException, SQLException
            {
                Map<String, Object> result = super.insertRow(user, container, row);
                Object paymentInfo = row.get("UsagePayments");
                if (paymentInfo instanceof JSONArray a)
                {
                    List<Map<String, Object>> paymentRows = new ArrayList<>();
                    for (Object o : a)
                    {
                        if (o instanceof JSONObject jsonObject)
                        {
                            Map<String, Object> rowMap = jsonObject.toMap();
                            rowMap.put("InstrumentScheduleId", result.get("Id"));
                            paymentRows.add(rowMap);
                        }
                    }

                    TableInfo paymentTable = getUserSchema().getTableOrThrow(TargetedMSSchema.TABLE_INSTRUMENT_USAGE_PAYMENT);
                    BatchValidationException errors = new BatchValidationException();
                    try
                    {
                        paymentTable.getUpdateService().insertRows(getUserSchema().getUser(), getUserSchema().getContainer(), paymentRows, errors, null, null);
                    }
                    catch (BatchValidationException e)
                    {
                        throw e.getLastRowError();
                    }
                    if (errors.hasErrors())
                    {
                        throw errors.getLastRowError();
                    }
                }
                return result;
            }
        };
    }
}
