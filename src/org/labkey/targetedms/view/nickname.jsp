<%
/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.model.InstrumentNickname" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<InstrumentNickname> currentView = HttpView.currentView();
    InstrumentNickname name = currentView.getModelBean();
    Map<String, String> targetContainers = new LinkedHashMap<>();
    Container shared = ContainerManager.getSharedContainer();
    if (shared.hasPermission(getUser(), UpdatePermission.class))
    {
        targetContainers.put(shared.getId(), shared.getPath());
    }
    Container project = getContainer().getProject();
    if (project != null && project.hasPermission(getUser(), UpdatePermission.class))
    {
        targetContainers.put(project.getId(), project.getPath());
    }
    if (getContainer().hasPermission(getUser(), UpdatePermission.class))
    {
        targetContainers.put(getContainer().getId(), getContainer().getPath());
    }
    %>

    <labkey:form action="<%=urlFor(TargetedMSController.SaveInstrumentNameAction.class)%>" method="post" layout="horizontal">
        <labkey:input type="hidden" name="id" value="<%=name.getId()%>"/>
        <labkey:input type="hidden" name="model" value="<%=name.getModel()%>"/>
        <labkey:input type="hidden" name="serialNumber" value="<%=name.getSerialNumber()%>"/>
        <table>
            <tr class="form-group">
                <td class="lk-form-label">Model</td>
                <td><%= h(name.getModel()) %></td>
            </tr>
            <tr class="form-group">
                <td class="lk-form-label">Serial Number</td>
                <td><%= h(name.getSerialNumber()) %></td>
            </tr>
            <tr class="form-group">
                <td class="lk-form-label"><label for="nickname<%= name.getId()%>">Nickname</label></td>
                <td><% if (targetContainers.isEmpty()) { %><%= h(name.getNickname()) %><% } else { %><labkey:input type="text" id="nickname<%= name.getId()%>" name="name" value="<%= name.getNickname() %>" size="40" /><% } %></td>
            </tr>
            <% if (!targetContainers.isEmpty()) { %>
            <tr class="form-group">
                <td class="lk-form-label"><label for="targetContainerId<%= name.getId()%>">Save in</label></td>
                <td>
                    <select name="targetContainerId" id="targetContainerId<%= name.getId()%>">
                        <labkey:options value="<%= name.getContainer().getId() %>" map="<%= targetContainers %>"/>
                    </select>
                    <%= button("Save").submit(true) %>
                </td>
            </tr>
            <% } %>
        </table>
    </labkey:form>

