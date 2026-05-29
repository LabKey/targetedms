/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
