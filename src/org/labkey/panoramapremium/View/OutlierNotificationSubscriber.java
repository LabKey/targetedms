package org.labkey.panoramapremium.View;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NavTreeCustomizer;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.List;

public class OutlierNotificationSubscriber implements NavTreeCustomizer
{

    private static final OutlierNotificationSubscriber _instance = new OutlierNotificationSubscriber();

    public static OutlierNotificationSubscriber get()
    {
        return _instance;
    }

    @NotNull
    @Override
    public List<NavTree> getNavTrees(ViewContext viewContext)
    {
        if(viewContext.getContainer().hasPermission(viewContext.getUser(), ReadPermission.class) && !viewContext.getUser().isGuest())
        {
            List<NavTree> navTrees = new ArrayList<>();
            NavTree link = new NavTree("OutlierNotificationSubscription");
            ActionURL url = new ActionURL("panoramapremium", "subscribeOutlierNotifications", viewContext.getContainer()).addReturnURL(viewContext.getActionURL());
            link.addChild("Subscribe Outlier Notifications", url);
            navTrees.add(link);

            return navTrees;
        }
        else
        {
            return new ArrayList<>();
        }
    }
}
