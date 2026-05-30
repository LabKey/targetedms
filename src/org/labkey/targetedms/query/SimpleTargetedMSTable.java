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
