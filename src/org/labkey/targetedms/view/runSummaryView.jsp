<%--
  ~ Copyright (c) 2012-2013 LabKey Corporation
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.RunDetailsBean> me = (JspView<TargetedMSController.RunDetailsBean>) HttpView.currentView();
    TargetedMSController.RunDetailsBean bean = me.getModelBean();
    ActionURL downloadAction = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getViewContext().getContainer());
    downloadAction.addParameter("runId", bean.getRun().getId());
%>

<table>
    <tr>
        <td class="labkey-form-label">File</td>
        <td>
            <%= h(bean.getRun().getFileName())%>
            <%= textLink("Download", downloadAction)%>
        </td>
    </tr>
    <tr>
        <td class="labkey-form-label">Peptide Group Count</td>
        <td><%= bean.getRun().getPeptideGroupCount()%></td>

        <td class="labkey-form-label">Peptide Count</td>
        <td><%= bean.getRun().getPeptideCount()%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Precursor Count</td>
        <td><%= bean.getRun().getPrecursorCount()%></td>

        <td class="labkey-form-label">Transition Count</td>
        <td><%= bean.getRun().getTransitionCount()%></td>
    </tr>
</table>


