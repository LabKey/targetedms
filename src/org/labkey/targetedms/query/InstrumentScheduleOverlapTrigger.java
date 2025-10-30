package org.labkey.targetedms.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Prevents overlapping instrument schedule entries for the same instrument.
 */
public class InstrumentScheduleOverlapTrigger implements Trigger
{
    @Override
    public void beforeInsert(TableInfo table, Container c, User user, Map<String, Object> newRow, ValidationException errors, Map<String, Object> extraContext)
    {
        validateNoOverlap(newRow, null, errors);
    }

    @Override
    public void beforeUpdate(TableInfo table, Container c, User user, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow, ValidationException errors, Map<String, Object> extraContext)
    {
        // Use the Id from either newRow or oldRow to exclude the current record
        Integer id = getId(newRow);
        if (id == null)
            id = getId(oldRow);
        validateNoOverlap(newRow, id, errors);
    }

    private void validateNoOverlap(Map<String, Object> row, @Nullable Integer excludeId, ValidationException errors)
    {
        Number instrument = (Number) row.get("Instrument");
        Date start = (Date) row.get("StartTime");
        Date end = (Date) row.get("EndTime");

        if (instrument == null)
        {
            errors.addGlobalError("Instrument is required");
            return;
        }
        if (start == null || end == null)
        {
            errors.addGlobalError("StartTime and EndTime are required");
            return;
        }
        if (!start.before(end))
        {
            errors.addGlobalError("StartTime must be before EndTime");
            return;
        }

        // Overlap condition: existing.start < new.end AND existing.end > new.start
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT s.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoInstrumentSchedule(), "s");
        sql.append(" WHERE s.Instrument = ? AND s.StartTime < ? AND s.EndTime > ?");
        sql.add(instrument.intValue());
        sql.add(new Timestamp(end.getTime()));
        sql.add(new Timestamp(start.getTime()));

        if (excludeId != null)
        {
            sql.append(" AND s.Id != ?");
            sql.add(excludeId);
        }

        boolean overlapExists = new SqlSelector(TargetedMSManager.getSchema(), sql).exists();
        if (overlapExists)
        {
            errors.addGlobalError("Instrument schedule overlaps with an existing reservation for this instrument");
        }
    }

    @Nullable
    private Integer getId(@Nullable Map<String, Object> row)
    {
        if (row == null)
            return null;
        Object o = row.get("Id");
        if (o instanceof Number n)
            return n.intValue();
        return null;
    }
}
