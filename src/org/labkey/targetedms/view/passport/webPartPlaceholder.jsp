<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<String> me = HttpView.currentView();
    String divId = me.getModelBean();
%>

<div id="<%= h(divId )%>"></div>
