package org.labkey.panoramapremium.View;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NavTreeCustomizer;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.List;

public class ConfigureQCMetricsCustomizer implements NavTreeCustomizer
{

    private static final ConfigureQCMetricsCustomizer _instance = new ConfigureQCMetricsCustomizer();

    public static ConfigureQCMetricsCustomizer get()
    {
        return _instance;
    }

    @NotNull
    @Override
    public List<NavTree> getNavTrees(ViewContext viewContext)
    {
        if(viewContext.getContainer().hasPermission(viewContext.getUser(), AdminPermission.class))
        {
            List<NavTree> navTrees = new ArrayList<>();
            NavTree link = new NavTree("QCMetricConfiguration");
            ActionURL url = new ActionURL("panoramapremium", "configureQCMetric", viewContext.getContainer()).addReturnURL(viewContext.getActionURL());
            link.addChild("Configure QC Metrics", url);
            navTrees.add(link);

            return navTrees;
        }
        else
        {
            return new ArrayList<>();
        }
    }
}
