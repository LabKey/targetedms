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
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSController.GuestAccessActionState" %>
<%@ page import="org.labkey.targetedms.TargetedMSController.GuestAccessSettingsBean" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    GuestAccessSettingsBean bean = (GuestAccessSettingsBean) HttpView.currentView().getModelBean();
%>
<div style="max-width: 720px;">
    <p>
        Use this to require a login on some Panorama pages when a lot of bot traffic is hitting public
        data. When the master switch below is <b>on</b>, each checked page requires a logged-in user, and a
        guest is sent to the login page. When the master switch is <b>off</b> (the default), all pages
        work normally and stay open to guests on public data. Leave this off during normal operation.
    </p>
    <p>
        This does not change the pages that always require a login for guests.
    </p>

    <labkey:form action="<%=urlFor(TargetedMSController.GuestAccessSettingsAction.class)%>" method="post">
        <div class="form-group">
            <label>
                <input type="checkbox" id="tms-require-login-master" name="masterEnabled" value="true"
                       <%=unsafe(bean.isMasterEnabled() ? "checked" : "")%> />
                <b>Require login for the selected pages (master switch)</b>
            </label>
        </div>

        <table class="lk-fields-table" style="margin-left: 24px;">
            <% for (GuestAccessActionState state : bean.getActions()) { %>
            <tr>
                <td>
                    <label>
                        <input type="checkbox" class="tms-require-login-action" name="restrictedActions"
                               value="<%=h(state.getName())%>"
                               <%=unsafe(state.isChecked() ? "checked" : "")%>
                               <%=unsafe(bean.isMasterEnabled() ? "" : "disabled")%> />
                        <%=h(state.getLabel())%>
                    </label>
                </td>
            </tr>
            <% } %>
        </table>

        <br/>
        <%=button("Save").submit(true)%>
    </labkey:form>
</div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
(function() {
    var master = document.getElementById('tms-require-login-master');
    var actionBoxes = document.querySelectorAll('.tms-require-login-action');

    // Per-action checkboxes are only meaningful when the master switch is on, so disable them otherwise.
    function syncEnabled() {
        for (var i = 0; i < actionBoxes.length; i++) {
            actionBoxes[i].disabled = !master.checked;
        }
    }

    master.addEventListener('change', syncEnabled);
    syncEnabled();
})();
</script>
