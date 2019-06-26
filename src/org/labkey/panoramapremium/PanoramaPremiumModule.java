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

package org.labkey.panoramapremium;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.Portal;
import org.labkey.api.view.WebPartFactory;
import org.labkey.panoramapremium.View.ConfigureQCMetricsCustomizer;
import org.labkey.panoramapremium.View.OutlierNotificationSubscriber;

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
    public double getVersion()
    {
        return 19.11;
    }

    @Override
    protected void init()
    {
        addController(PanoramaPremiumController.NAME, PanoramaPremiumController.class);
        PanoramaPremiumSchema.register(this);
        Portal.registerNavTreeCustomizer("Targeted MS QC Plots", ConfigureQCMetricsCustomizer.get());
        Portal.registerNavTreeCustomizer("Targeted MS QC Summary", OutlierNotificationSubscriber.get());
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