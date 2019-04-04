<%@page session="false"
        contentType="text/html"
        pageEncoding="utf-8"
        import="com.day.cq.replication.Agent,
            com.day.cq.replication.AgentManager" %><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %><%
%><cq:defineObjects />
<%@include file="/libs/cq/replication/components/dateutils/dateutils.jsp" %>
<%

    String agentName = currentPage.getName();
    AgentManager agentMgr = sling.getService(AgentManager.class);
    Agent agent = agentName == null ? null : agentMgr.getAgents().get(agentName);
    String title = agent == null ? null : agent.getConfiguration().getName();
    if (title == null) {
        title = agentName;
    }
%><html><head>
    <style type="text/css">
        code {
            font-family:lucida console, courier new, monospace;
            font-size:12px;
            white-space:nowrap;
        }
    </style>
    <title>AEM Replication | Log for <%= xssAPI.encodeForHTML(title) %></title>
</head>
<body bgcolor="white"><code><%
    if (agent == null) {
        %>no such agent: <%= xssAPI.encodeForHTML(agentName) %><br><%
    } else {
        for (String line: agent.getLog().getLines()) {
            // convert time 
            String date = "";
            int idx = line.indexOf(' ');
            if (idx > 0) {
                try {
                    long time = Long.parseLong(line.substring(0, idx));
                    line = line.substring(idx);
                    date = REPLICATION_DATE_DEFAULT.format(time);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            %><%= date %><%= xssAPI.encodeForHTML(line) %><br>
<%
        }
    }
%></code>
<a name="end"></a>
</body></html>
