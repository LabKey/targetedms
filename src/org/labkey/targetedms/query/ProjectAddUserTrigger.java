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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;

import java.util.HashMap;
import java.util.Map;

public class ProjectAddUserTrigger implements Trigger
{

    @Override
    public void afterInsert(TableInfo table, Container c,
                             User user, @Nullable Map<String, Object> newRow,
                             ValidationException errors, Map<String, Object> extraContext, @Nullable Map<String, Object> existingRecord)
    {
        // Add the creating user to the membership list
        Map<String, Object> membership = new HashMap<>();
        membership.put("Project", newRow.get("Id"));
        membership.put("Researcher", user.getUserId());
        membership.put("Container", c.getId());

        Table.insert(user, TargetedMSManager.getTableInfoProjectResearcher(), membership);
    }
}
