package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSSchema;

public class SimpleTargetedMSTable extends SimpleUserSchema.SimpleTable<TargetedMSSchema>
{
    public SimpleTargetedMSTable(String name, TargetedMSSchema schema, ContainerFilter cf)
    {
        super(schema, TargetedMSSchema.getSchema().getTable(name), cf);
        wrapAllColumns(true);
        TargetedMSTable.fixupLookups(this);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return getContainer().hasPermission(user, perm);
    }

    @Override
    @NotNull
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
