<%
    /*
     * Copyright (c) 2026 LabKey Corporation
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
<%@ page import="org.labkey.targetedms.TargetedMSController.InstrumentUtilizationBean" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<InstrumentUtilizationBean> me = HttpView.currentView();
    InstrumentUtilizationBean bean = me.getModelBean();
%>
<div class="btn-group" role="group" style="margin-bottom: 10px;">
    <button type="button" id="utilizationToggleDay" class="btn btn-primary labkey-button">By Day</button>
    <button type="button" id="utilizationToggleMonth" class="btn btn-default labkey-button">By Month</button>
</div>

<div id="utilizationByDayGrid" style="max-width: 100%; overflow-x: auto;">
    <% me.include(bean.getByDayView(), out); %>
</div>
<div id="utilizationByMonthGrid" style="max-width: 100%; overflow-x: auto; display: none;">
    <% me.include(bean.getByMonthView(), out); %>
</div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
(function() {
    const dayBtn = document.getElementById('utilizationToggleDay');
    const monthBtn = document.getElementById('utilizationToggleMonth');
    const dayGrid = document.getElementById('utilizationByDayGrid');
    const monthGrid = document.getElementById('utilizationByMonthGrid');

    function show(byMonth) {
        dayGrid.style.display = byMonth ? 'none' : '';
        monthGrid.style.display = byMonth ? '' : 'none';
        dayBtn.className = byMonth ? 'btn btn-default labkey-button' : 'btn btn-primary labkey-button';
        monthBtn.className = byMonth ? 'btn btn-primary labkey-button' : 'btn btn-default labkey-button';
    }

    dayBtn.addEventListener('click', function() { show(false); });
    monthBtn.addEventListener('click', function() { show(true); });
})();
</script>
