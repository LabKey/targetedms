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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.targetedms.TargetedMSService" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.chromlib.ChromatogramLibraryUtils" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.util.Date" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    Container c = getContainer();
    TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(c);

    boolean isLibrary = ( folderType == TargetedMSService.FolderType.Library ||
                          folderType == TargetedMSService.FolderType.LibraryProtein );

    int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(c, getUser());
%>

<table class="table-condensed labkey-data-region table-bordered" style="width: auto">
<tr class="labkey-col-header-row">
    <th>Revision #</th>
    <th>File name</th>
    <th>Size</th>
    <th>Date created</th>
</tr>
        <%
    for (int i=1; i <= currentRevision; i++)
    {
        ActionURL u = new ActionURL(TargetedMSController.DownloadChromLibraryAction.class, c);
        u.addParameter("revision", i);
        Path archiveFile = ChromatogramLibraryUtils.getChromLibFile(c, i);
        if (Files.exists(archiveFile) && !Files.isDirectory(archiveFile)) {
%>
    <tr class="<%= h(i % 2 == 0 ? "labkey-row" : "labkey-alternate-row")%>">
        <td align="right"><%= i %></td>
        <td><%= link(ChromatogramLibraryUtils.getDownloadFileName(c, i), u) %></td>
        <td align="right"><%= h(FileUtils.byteCountToDisplaySize(Files.size(archiveFile))) %></td>
        <td><%= formatDateTime(new Date(Files.getLastModifiedTime(archiveFile).toMillis()))%></td>
    </tr>
<%
        }
        else
        {
%><tr>
    <td align="right"><%= i %></td>
    <td><%= h(ChromatogramLibraryUtils.getDownloadFileName(c, i)) %></td>
    <td>unavailable</td>
    <td></td>
</tr><%
        }
    }
%>
</table>