package org.labkey.targetedms.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.ApiUsageException;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ensures that all edits to instrument usage payments add up to 100%.
 */
public class InstrumentUsagePaymentTrigger implements Trigger
{
    private final Set<Integer> _schedulesToCheck = new HashSet<>();
    private final String _instrumentScheduleColumnName;

    public InstrumentUsagePaymentTrigger(String instrumentScheduleColumnName)
    {
        _instrumentScheduleColumnName = instrumentScheduleColumnName;
    }

    @Override
    public void afterUpdate(TableInfo table, Container c, User user, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
    {
        trackChange(newRow);
    }

    private void trackChange(Map<String, Object> row) throws ValidationException
    {
        Number number = (Number) row.get(_instrumentScheduleColumnName);
        if (number == null)
        {
            throw new ValidationException(_instrumentScheduleColumnName + " cannot be null");
        }
        _schedulesToCheck.add(number.intValue());
    }

    @Override
    public void afterInsert(TableInfo table, Container c, User user, @Nullable Map<String, Object> newRow, ValidationException errors, Map<String, Object> extraContext, @Nullable Map<String, Object> existingRecord) throws ValidationException
    {
        trackChange(newRow);
    }

    @Override
    public void beforeDelete(TableInfo table, Container c, User user, @Nullable Map<String, Object> oldRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
    {
        trackChange(oldRow);
    }

    @Override
    public void complete(TableInfo table, Container c, User user, TableInfo.TriggerType event, BatchValidationException errors, Map<String, Object> extraContext)
    {
        table.getSchema().getScope().addCommitTask(() ->
        {
            SQLFragment sql = new SQLFragment("SELECT * FROM (\n");
            sql.append("SELECT COUNT(*) AS C, s.Id, SUM(PercentPayment) AS PercentTotal FROM ");
            sql.append(TargetedMSManager.getTableInfoInstrumentSchedule(), "s");
            sql.append(" LEFT OUTER JOIN ");
            sql.append(TargetedMSManager.getTableInfoInstrumentUsagePayment(), "iup");
            sql.append(" ON s.Id = iup.InstrumentScheduleId\n");
            sql.append(" GROUP BY s.Id\n");
            sql.append(") x\n");
            sql.append(" WHERE (PercentTotal != 100 OR PercentTotal IS NULL) AND Id ");
            sql.appendInClause(_schedulesToCheck, table.getSchema().getSqlDialect());

            Collection<Map<String, Object>> rows = new SqlSelector(TargetedMSManager.getSchema(), sql).getMapCollection();

            if (!rows.isEmpty())
            {
                throw new ApiUsageException("Instrument usage payments do not add up to 100%");
            }
        }, DbScope.CommitTaskOption.PRECOMMIT);
    }
}
