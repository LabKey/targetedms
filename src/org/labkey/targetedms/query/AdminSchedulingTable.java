package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.targetedms.TargetedMSSchema;

/** Tables that only folder admins or higher should be able to modify */
public class AdminSchedulingTable extends SimpleTargetedMSTable
{
    public AdminSchedulingTable(String name, TargetedMSSchema schema, ContainerFilter cf)
    {
        super(name, schema, cf);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // Admins can do it all
        if (getUserSchema().getContainer().hasPermission(user, AdminPermission.class))
        {
            return true;
        }

        // Anyone else with folder permissions can read
        if (perm.equals(ReadPermission.class))
        {
            return super.hasPermission(user, perm);
        }

        // but nothing else
        return false;
    }
}
