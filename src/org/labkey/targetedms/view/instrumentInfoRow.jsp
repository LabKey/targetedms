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
<%@ page import="org.labkey.targetedms.TargetedMSController.InstrumentInfoRowBean" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<InstrumentInfoRowBean> me = HttpView.currentView();
    InstrumentInfoRowBean bean = me.getModelBean();
%>
<div class="row" id="lk-instrument-info-row">
    <div class="col-md-5">
        <% me.include(bean.getInfoView(), out); %>
    </div>
    <div class="col-md-7" style="max-width: 100%; overflow-x: auto;">
        <% me.include(bean.getSummaryView(), out); %>
    </div>
</div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
(function() {
    // Match the height of the two side-by-side web part panels
    function equalizeHeights() {
        var cols = document.querySelectorAll('#lk-instrument-info-row > [class*="col-"]');
        var panels = [];
        for (var i = 0; i < cols.length; i++) {
            var panel = cols[i].querySelector('.panel');
            if (panel) {
                panels.push(panel);
            }
        }
        if (panels.length < 2) {
            return;
        }

        // Reset before measuring so re-runs (e.g. on resize) don't accumulate.
        panels.forEach(function(p) { p.style.minHeight = ''; });

        // If the columns have wrapped onto separate rows, leave their natural heights alone.
        var firstTop = panels[0].getBoundingClientRect().top;
        var sameRow = panels.every(function(p) { return Math.abs(p.getBoundingClientRect().top - firstTop) < 1; });
        if (!sameRow) {
            return;
        }

        var tallest = 0;
        panels.forEach(function(p) { tallest = Math.max(tallest, p.offsetHeight); });
        panels.forEach(function(p) { p.style.minHeight = tallest + 'px'; });
    }

    if (document.readyState !== 'loading') {
        equalizeHeights();
    }
    else {
        document.addEventListener('DOMContentLoaded', equalizeHeights);
    }
    window.addEventListener('resize', equalizeHeights);
})();
</script>
