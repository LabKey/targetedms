package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSSchema;

public class SimpleTargetedMSTable extends FilteredTable<TargetedMSSchema>
{
    public SimpleTargetedMSTable(String name, TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSSchema.getSchema().getTable(name), schema, cf);
        wrapAllColumns(true);
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
