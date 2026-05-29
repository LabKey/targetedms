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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OwnProjectSchedulingTable extends SimpleTargetedMSTable
{
    private final boolean _allowCollaboratorsToInsert;

    public OwnProjectSchedulingTable(String name, TargetedMSSchema schema, ContainerFilter cf, boolean allowCollaboratorsToInsert)
    {
        super(name, schema, cf);
        _allowCollaboratorsToInsert = allowCollaboratorsToInsert;
        addTriggerFactory((c, table, extraContext) -> List.of(new OwnProjectTrigger()));
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        if (isAdmin())
        {
            return true;
        }

        // Collaborators shouldn't be able to insert new projects but should be able to edit ones they're attached to
        if (perm.equals(InsertPermission.class) && !_allowCollaboratorsToInsert)
        {
            return isLabMember();
        }

        // Let lab members and collaborators past the initial check for insert/update/delete permission and enforce
        // row-level permissions in the UpdateService
        return (perm.equals(InsertPermission.class) || perm.equals(UpdatePermission.class) ||
                perm.equals(DeletePermission.class) || perm.equals(ReadPermission.class))
                && (isLabMember() || isExternalCollaborator());
    }

    private boolean isAdmin()
    {
        return getUserSchema().getContainer().hasPermission(getUserSchema().getUser(), AdminPermission.class);
    }

    private boolean isLabMember()
    {
        // Check for key permissions for editors
        return getUserSchema().getContainer().hasPermission(getUserSchema().getUser(), InsertPermission.class) &&
                getUserSchema().getContainer().hasPermission(getUserSchema().getUser(), UpdatePermission.class) &&
                getUserSchema().getContainer().hasPermission(getUserSchema().getUser(), DeletePermission.class);
    }

    private boolean isExternalCollaborator()
    {
        // Check for key permissions submitters
        return getUserSchema().getContainer().hasPermission(getUserSchema().getUser(), InsertPermission.class);
    }

    private class OwnProjectTrigger implements Trigger
    {
        private Set<Integer> _projectIds;

        private void validateProject(Integer projectId) throws ValidationException
        {
            // Admins can make changes to any project. Others need to be members
            if (!getUserSchema().getContainer().hasPermission(getUserSchema().getUser(), AdminPermission.class))
            {
                if (_projectIds == null)
                {
                    _projectIds = new HashSet<>(new SqlSelector(getSchema(),
                            new SQLFragment("SELECT Project FROM ").
                                    append(TargetedMSManager.getTableInfoProjectResearcher(), "pr").
                                    append(" WHERE Researcher = ?").
                                    add(getUserSchema().getUser().getUserId())).getArrayList(Integer.class));
                }
                if (projectId == null || !_projectIds.contains(projectId))
                {
                    throw new ValidationException("User is not a member of the project");
                }
            }
        }

        @Nullable
        private Integer getInteger(@Nullable Map<String, Object> row, String key)
        {
            if (row == null)
                return null;
            Object o = row.get(key);
            if (o instanceof Number n)
                return n.intValue();
            return null;
        }


        @Override
        public void beforeInsert(TableInfo table, Container c, User user, @Nullable QueryUpdateService.InsertOption insertOption, @Nullable Map<String, Object> newRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
        {
            checkRowLevelPermission(newRow, true);
        }

        private void checkRowLevelPermission(@Nullable Map<String, Object> newRow, boolean insert) throws ValidationException
        {
            String tableName = OwnProjectSchedulingTable.this.getName();
            if (TargetedMSSchema.TABLE_MS_PROJECT.equalsIgnoreCase(tableName))
            {
                if (!insert)
                {
                    validateProject(getInteger(newRow, "id"));
                }
            }
            if (TargetedMSSchema.TABLE_INSTRUMENT_SCHEDULE.equalsIgnoreCase(tableName) ||
                    TargetedMSSchema.TABLE_PROJECT_RESEARCHER.equalsIgnoreCase(tableName))
            {
                validateProject(getInteger(newRow, "project"));

            }
            if (TargetedMSSchema.TABLE_INSTRUMENT_USAGE_PAYMENT.equalsIgnoreCase(tableName))
            {
                Integer scheduleId = getInteger(newRow, "InstrumentScheduleId");
                if (scheduleId != null)  // A null will violate the non-null constraint, so we don't need to report an error
                {
                    Integer project = new SqlSelector(getSchema(),
                            new SQLFragment("SELECT Project FROM ").
                                    append(TargetedMSManager.getTableInfoInstrumentSchedule()).
                                    append(" WHERE Id = ?").add(scheduleId)).getObject(Integer.class);
                    validateProject(project);
                }
            }
        }

        @Override
        public void beforeUpdate(TableInfo table, Container c, User user, @Nullable QueryUpdateService.InsertOption insertOption, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
        {
            checkRowLevelPermission(oldRow, false);
            checkRowLevelPermission(newRow, false);
        }

        @Override
        public void beforeDelete(TableInfo table, Container c, User user, @Nullable Map<String, Object> oldRow, ValidationException errors, Map<String, Object> extraContext) throws ValidationException
        {
            checkRowLevelPermission(oldRow, false);
        }
    }
}
