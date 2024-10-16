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
package org.labkey.targetedms.view;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.targetedms.TargetedMSController;

public class ReplicateSummaryWebPart extends JspView<Integer>
{
    public ReplicateSummaryWebPart(ViewContext context, @Nullable Integer sampleLimit)
    {
        super("/org/labkey/targetedms/view/qcSummary.jsp", sampleLimit);
        setTitleHref(new ActionURL(TargetedMSController.QCSummaryHistoryAction.class, context.getContainer()));
        addClientDependency(ClientDependency.fromPath("Ext4"));
        setTitle("Replicate Summary");
        setFrame(WebPartView.FrameType.PORTAL);
    }
}
