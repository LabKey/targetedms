/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

package org.labkey.panoramapremium;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.Portal;
import org.labkey.api.view.WebPartFactory;
import org.labkey.panoramapremium.View.QCSummaryMenuCustomizer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;


public class PanoramaPremiumModule extends DefaultModule
{
    public static final String NAME = "PanoramaPremium";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 22.000;
    }

    @Override
    protected void init()
    {
        addController(PanoramaPremiumController.NAME, PanoramaPremiumController.class);
        PanoramaPremiumSchema.register(this);
        Portal.registerNavTreeCustomizer("Targeted MS QC Summary", new QCSummaryMenuCustomizer("configureQCMetric", "Configure QC Metrics"));
        Portal.registerNavTreeCustomizer("Targeted MS QC Plots", new QCSummaryMenuCustomizer("configureQCMetric", "Configure QC Metrics"));

        Portal.registerNavTreeCustomizer("Targeted MS QC Summary", new QCSummaryMenuCustomizer("subscribeOutlierNotifications", "Subscribe Outlier Notifications"));
        Portal.registerNavTreeCustomizer("Targeted MS QC Plots", new QCSummaryMenuCustomizer("subscribeOutlierNotifications", "Subscribe Outlier Notifications"));

        Portal.registerNavTreeCustomizer("Targeted MS QC Summary", new QCSummaryMenuCustomizer("configureQCGroups", "Include or Exclude Precursors"));
        TargetedMSService.get().registerSkylineDocumentImportListener(QCNotificationSender.get());
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(new PanoramaPremiumContainerListener());
    }

    @Override
    @NotNull
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(PanoramaPremiumManager.get().getSchemaName());
    }
}