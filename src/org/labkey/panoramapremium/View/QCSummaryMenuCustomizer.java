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
    private final String controller = "panoramapremium";
    private @NotNull String actionName;
    private @NotNull String menuLabel;

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
            ActionURL url = new ActionURL(controller, actionName, viewContext.getContainer()).addReturnURL(viewContext.getActionURL());
            navTrees.add(new NavTree(menuLabel, url));

            return navTrees;
        }
        else
        {
            return Collections.emptyList();
        }
    }
}
