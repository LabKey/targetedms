package org.labkey.targetedms.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProjectAddUserTrigger implements Trigger
{

    @Override
    public void afterInsert(TableInfo table, Container c,
                             User user, @Nullable Map<String, Object> newRow,
                             ValidationException errors, Map<String, Object> extraContext, @Nullable Map<String, Object> existingRecord) throws ValidationException
    {
        // Add the creating user to the membership list
        Map<String, Object> membership = new HashMap<>();
        membership.put("Project", newRow.get("Id"));
        membership.put("Researcher", user.getUserId());
        membership.put("Container", c.getId());

        Table.insert(user, TargetedMSManager.getTableInfoProjectResearcher(), membership);
    }
}
