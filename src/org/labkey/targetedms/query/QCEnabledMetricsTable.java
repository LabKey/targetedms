/*
 * Copyright (c) 2019 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.model.QCMetricStatus;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class QCEnabledMetricsTable extends SimpleUserSchema.SimpleTable<TargetedMSSchema>
{
    public QCEnabledMetricsTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(schema, TargetedMSManager.getTableInfoQCEnabledMetrics(), cf);
        wrapAllColumns(true);
        TargetedMSTable.fixupLookups(this);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        if (filter.getType() == ContainerFilter.Type.Current)
            filter = getDefaultMetricContainerFilter(getUserSchema().getContainer(), getUserSchema().getUser());

        super.applyContainerFilter(filter);
    }

    public static ContainerFilter getDefaultMetricContainerFilter(Container c, User user)
    {
        // the base set of configuration live at the root container
        return new ContainerFilter.CurrentPlusExtras(c, user, ContainerManager.getRoot());
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        Class<? extends Permission> permissionToCheck = perm == ReadPermission.class ? ReadPermission.class : AdminPermission.class;
        return getContainer().hasPermission(user, permissionToCheck);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            protected Map<String, Object> _insert(User user, Container c, Map<String, Object> row) throws SQLException, ValidationException
            {
                TargetedMSManager.get().clearCachedEnabledQCMetrics(c);
                validateBounds(row);
                return super._insert(user, c, row);
            }

            private static void validateBounds(Map<String, Object> row) throws ValidationException
            {
                if (!QCMetricStatus.ValueCutoff.toString().equalsIgnoreCase(Objects.toString(row.get("Status"))))
                {
                    row.put("upperBound", null);
                    row.put("lowerBound", null);
                }
                else
                {
                    Object upperObject = row.get("upperBound");
                    Object lowerObject = row.get("lowerBound");
                    if (upperObject == null && lowerObject == null)
                    {
                        throw new ValidationException("For value cutoffs, please provide a value for the upper and/or lower bound");
                    }
                    if (upperObject != null && lowerObject != null)
                    {
                        double upperBound = ConvertHelper.convert(row.get("upperBound"), Double.class);
                        double lowerBound = ConvertHelper.convert(row.get("lowerBound"), Double.class);
                        if (upperBound < lowerBound)
                        {
                            throw new ValidationException("Upper bound must be greater than lower bound");
                        }
                    }
                }
            }

            @Override
            protected Map<String, Object> _update(User user, Container c, Map<String, Object> row, Map<String, Object> oldRow, Object[] keys) throws SQLException, ValidationException
            {
                TargetedMSManager.get().clearCachedEnabledQCMetrics(c);
                validateBounds(row);
                return super._update(user, c, row, oldRow, keys);
            }

            @Override
            protected void _delete(Container c, Map<String, Object> row) throws InvalidKeyException
            {
                TargetedMSManager.get().clearCachedEnabledQCMetrics(c);
                super._delete(c, row);
            }
        };
    }
}