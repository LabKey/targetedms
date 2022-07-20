<%
/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
<%@ page import="org.labkey.targetedms.parser.PeptideGroup" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.targetedms.parser.Protein" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page import="java.util.Objects" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PeptideGroup> me = (JspView<PeptideGroup>) HttpView.currentView();
    PeptideGroup bean = me.getModelBean();
    TargetedMSRun run = TargetedMSManager.getRun(bean.getRunId());
%>

<table class="lk-fields-table">
    <tr>
        <td class="labkey-form-label">File</td>
        <td>
            <a href="<%= h(TargetedMSController.getShowRunURL(run.getContainer(), run.getRunId()))%>"><%= h(run.getDescription()) %></a> &nbsp;&nbsp;
            <% if (run.getFileName() != null) { TargetedMSController.createDownloadMenu(run).render(out); } %>
        </td>
    </tr>
    <tr>
        <td class="labkey-form-label">Description</td>
        <td><%= h(bean.getDescription()) %></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Decoy</td>
        <td><%= h(bean.isDecoy()) %></td>
    </tr>
    <%
        List<Protein> proteins = bean.getProteins();
        List<String> accessions = proteins.stream().map(Protein::getAccession).filter(Objects::nonNull).toList();
    %>
    <% if (proteins.size() > 1) { %>
    <tr>
        <td class="labkey-form-label">Proteins within group</td>
        <td><%= proteins.size() %></td>
    </tr>
    <% } %>
    <% if (!accessions.isEmpty()) { %>
        <tr>
            <td class="labkey-form-label"><%= h(StringUtilsLabKey.pluralize(accessions.size(), "Accession"))%></td>
            <td>
                <% for (String accession : accessions) { %>
                    <a href="https://www.uniprot.org/uniprot/<%=h(accession)%>"><%= h(accession)%></a>
                <% } %>
            </td>
        </tr>
    <% } %>
    <tr>
        <td class="labkey-form-label">Note</td>
        <td><%= h(bean.getNote()) %></td>
    </tr>
</table>


