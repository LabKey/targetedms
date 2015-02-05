<%@ page import="org.labkey.targetedms.SkylineDocImporter" %>
<%
/*
 * Copyright (c) 2014 LabKey Corporation
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

<div id="docSummary"></div>
<div id="precursorSummary"></div>

<script type="text/javascript">
    function init()
    {
        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: 'SELECT '
                + '(SELECT COUNT(DISTINCT ModifiedSequence) FROM targetedms.Precursor) as precursorCount '
                + ',(SELECT COUNT(Id) FROM targetedms.Runs WHERE StatusId = ' + <%=SkylineDocImporter.STATUS_SUCCESS%> + ') as docCount '
                + ',(SELECT COUNT(Id) FROM targetedms.SampleFile) as fileCount',
            success: function (data)
            {
                var docCount = data.rows[0].docCount;
                var fileCount = data.rows[0].fileCount;
                var precursorCount = data.rows[0].precursorCount;
                var docSummaryLine = docCount + " Skyline document" + (docCount == 1 ? "" : "s") + " uploaded containing " + fileCount + " sample file";
                if (fileCount != 1)
                    docSummaryLine += "s";
                Ext4.get('docSummary').update(docSummaryLine);
                Ext4.get('precursorSummary').update(precursorCount + " precursor" + (precursorCount == 1 ? "" : "s") + " tracked");
            },
            failure: function (response)
            {
                Ext4.get('docSummary').update("Error: " + response.exception);
            }
        });
    }
    Ext4.onReady(init);
</script>