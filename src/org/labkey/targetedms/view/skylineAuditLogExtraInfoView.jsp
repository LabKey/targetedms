<%
/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.parser.skyaudit.AuditLogEntry" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<AuditLogEntry> me = HttpView.currentView();
    AuditLogEntry bean = me.getModelBean();
%>
    <div id="targetedmsAuditLogExtraInfo" >
    <% if (bean == null) { %>
        Unable to find requested audit log entry
    <% } else { %>
    <table>
        <tr><td><strong>User Name:</strong> <%= h(bean.getUserName()) %></td><td><span class="fa fa-times">&nbsp;</span></td></tr>
        <tr><td colspan="2"><strong>Entry Timestamp:</strong> <%=formatDateTime(bean.getCreateTimestamp())%></td></tr>
    </table><br/>
    <pre style="overflow: scroll; max-height: 400px"><%=h(bean.getExtraInfo()) %></pre>
    <% } %>
</div>
