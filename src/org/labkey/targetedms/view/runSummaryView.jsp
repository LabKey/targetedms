<%
/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
%>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.SkylineFileUtils" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page import="java.io.File" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("vis/vis");
        dependencies.add("targetedms/js/DocumentSummary.js");
    }
%>

<%
    JspView<TargetedMSController.RunDetailsBean> me = (JspView<TargetedMSController.RunDetailsBean>) HttpView.currentView();
    TargetedMSController.RunDetailsBean bean = me.getModelBean();
    TargetedMSRun run = bean.getRun();
    File skyDocFile = SkylineFileUtils.getSkylineFile(run.getExperimentRunLSID());

    ActionURL downloadAction = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getContainer());
    downloadAction.addParameter("runId", run.getId());
    Container c = getContainer();

    ActionURL versionsAction = new ActionURL(TargetedMSController.ShowVersionsAction.class, getContainer());
    versionsAction.addParameter("id", run.getId());

    ActionURL precursorListAction = new ActionURL(TargetedMSController.ShowPrecursorListAction.class, getContainer());
    precursorListAction.addParameter("id", run.getId());

    ActionURL transitionListAction = new ActionURL(TargetedMSController.ShowTransitionListAction.class, getContainer());
    transitionListAction.addParameter("id", run.getId());

    ActionURL calibrationCurveListAction = new ActionURL(TargetedMSController.ShowCalibrationCurvesAction.class, getContainer());
    calibrationCurveListAction.addParameter("id", run.getId());

    String renameAction = null;
    if(c.hasPermission(getUser(), UpdatePermission.class))
        renameAction = TargetedMSController.getRenameRunURL(c, run, getActionURL()).getLocalURIString();
%>

<%
    String elementId = "targetedmsDocumentSummary";
%>
<div id=<%=q(elementId)%>> </div>

<script type="text/javascript">

    Ext4.onReady(function () {

        Ext4.create('LABKEY.targetedms.DocumentSummary', {
            renderTo: <%=q(elementId)%>,
            peptideCount: <%=run.getPeptideCount()%>,
            smallMoleculeCount: <%=run.getSmallMoleculeCount()%>,
            precursorCount: <%=run.getPrecursorCount()%>,
            transitionCount: <%=run.getTransitionCount()%>,
            peptideGroupCount: <%=run.getPeptideGroupCount()%>,
            calibrationCurveCount: <%=run.getCalibrationCurveCount()%>,
            versionCount: <%=bean.getVersionCount()%>,
            fileName: <%=q(skyDocFile != null ? skyDocFile.getName() : null)%>,
            downloadAction: <%=q(downloadAction.getLocalURIString())%>,
            renameAction: <%=q(renameAction)%>,
            versionsAction: <%=q(versionsAction.getLocalURIString())%>,
            precursorListAction: <%=q(precursorListAction.getLocalURIString())%>,
            transitionListAction: <%=q(transitionListAction.getLocalURIString())%>,
            calibrationCurveListAction: <%=q(calibrationCurveListAction.getLocalURIString())%>
        });
    });

</script>