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
 * Prevents overlapping instrument schedule entries for the same instrument and that the instrument is active.
 */
public class InstrumentScheduleOverlapTrigger implements Trigger
{
    @Override
    public void beforeInsert(TableInfo table, Container c, User user, Map<String, Object> newRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
    {
        validateNoOverlap(newRow, null);
        Number instrument = (Number) newRow.get("Instrument");
        SqlSelector selector = new SqlSelector(TargetedMSManager.getSchema(), "SELECT Id FROM " + TargetedMSManager.getTableInfoMSInstrument() + " WHERE Id = ? AND Active = ?", instrument, true);
        if (!selector.exists())
        {
            throw new ValidationException("Instrument does not exist or is not active");
        }
    }

    @Override
    public void beforeUpdate(TableInfo table, Container c, User user, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
    {
        // Use the Id from either newRow or oldRow to exclude the current record
        Integer id = getId(newRow);
        if (id == null)
            id = getId(oldRow);
        validateNoOverlap(newRow, id);
    }

    private void validateNoOverlap(Map<String, Object> row, @Nullable Integer excludeId) throws ValidationException
    {
        Number instrument = (Number) row.get("Instrument");
        Date start = (Date) row.get("StartTime");
        Date end = (Date) row.get("EndTime");

        if (instrument == null)
        {
            throw new ValidationException("Instrument is required");
        }
        if (start == null || end == null)
        {
            throw new ValidationException("StartTime and EndTime are required");
        }
        if (!start.before(end))
        {
            throw new ValidationException("StartTime must be before EndTime");
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
            throw new ValidationException("Instrument schedule overlaps with an existing reservation for this instrument");
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
