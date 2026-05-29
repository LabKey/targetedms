/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.panoramapremium.View;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NavTreeCustomizer;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QCSummaryMenuCustomizer implements NavTreeCustomizer
{
    private final @NotNull String actionName;
    private final @NotNull String menuLabel;

    public QCSummaryMenuCustomizer(@NotNull String actionName, @NotNull String menuLabel)
    {
        this.actionName = actionName;
        this.menuLabel = menuLabel;
    }

    @NotNull
    @Override
    public List<NavTree> getNavTrees(ViewContext viewContext)
    {
        if(viewContext.getContainer().hasPermission(viewContext.getUser(), AdminPermission.class))
        {
            List<NavTree> navTrees = new ArrayList<>();
            ActionURL url = new ActionURL("targetedms", actionName, viewContext.getContainer()).addReturnUrl(viewContext.getActionURL());
            navTrees.add(new NavTree(menuLabel, url));

            return navTrees;
        }
        else
        {
            return Collections.emptyList();
        }
    }
}
