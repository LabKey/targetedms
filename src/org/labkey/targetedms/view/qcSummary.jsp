<%
/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("hopscotch/css/hopscotch.min.css");
        dependencies.add("hopscotch/js/hopscotch.min.js");
        dependencies.add("targetedms/js/BaseQCPlotPanel.js");
        dependencies.add("targetedms/css/QCSummary.css");
        dependencies.add("targetedms/js/QCSummaryPanel.js");
    }
%>
<%!
    int uid = UniqueID.getRequestScopedUID(HttpView.currentRequest());
    String qcSummaryId = "qcSummary-" + uid;
%>

<div id=<%=q(qcSummaryId)%>></div>

<script type="text/javascript">
    Ext4.onReady(function()
    {
        Ext4.create('LABKEY.targetedms.QCSummary', {
            renderTo: <%=q(qcSummaryId)%>
        });
    });
</script>