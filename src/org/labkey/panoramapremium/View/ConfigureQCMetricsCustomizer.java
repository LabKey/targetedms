/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
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
            ActionURL url = new ActionURL("panoramapremium", "configureQCMetric", viewContext.getContainer()).addReturnURL(viewContext.getActionURL());
            navTrees.add(new NavTree("Configure QC Metrics", url));

            return navTrees;
        }
        else
        {
            return Collections.emptyList();
        }
    }
}
