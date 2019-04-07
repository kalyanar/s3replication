<%@page session="false"%><%--
  Copyright 1997-2009 Day Management AG
  Barfuesserplatz 6, 4001 Basel, Switzerland
  All Rights Reserved.

  This software is the confidential and proprietary information of
  Day Management AG, ("Confidential Information"). You shall not
  disclose such Confidential Information and shall use it only in
  accordance with the terms of the license agreement you entered into
  with Day.

  ==============================================================================

  Agent component

  Displays information about a replication agent.

--%><%@page contentType="text/html"
            pageEncoding="utf-8"
            import="com.day.cq.replication.Agent,
                    com.day.cq.replication.AgentConfig,
                    com.day.cq.replication.AgentManager,
                    com.adobe.granite.ui.clientlibs.HtmlLibraryManager,
                    com.day.cq.replication.ReplicationQueue,
                    com.day.cq.i18n.I18n" %><%
%><%@include file="/libs/foundation/global.jsp"%><%
    I18n i18n = new I18n(slingRequest);
    String id = currentPage.getName();
    String title = properties.get("jcr:title", id);

    AgentManager agentMgr = sling.getService(AgentManager.class);
    Agent agent = agentMgr.getAgents().get(id);
    AgentConfig cfg = agent == null ? null : agent.getConfiguration();
    ReplicationQueue queue = agent == null ? null : agent.getQueue();

    if (cfg == null || !cfg.getConfigPath().equals(currentNode.getPath())) {
        // agent not active
        agent = null;
    }

String uri = xssAPI.encodeForHTML(properties.get("transportUri", i18n.get("(not configured)")));
    String queueStr = i18n.get("Queue is <strong>not active</strong>");
    String queueCls = "cq-agent-queue";
    if (queue != null) {
        int num = queue.entries().size();
        if (queue.isBlocked()) {
            queueStr = i18n.get("Queue is <strong>blocked - {0} pending</strong>", "{0} is the number of pending items", num);
            queueCls += "-blocked";
        } else {
            if (num == 0) {
                queueStr = i18n.get("Queue is <strong>idle</strong>");
                queueCls += "-idle";
            } else  {
                queueStr = i18n.get("Queue is <strong>active - {0} pending</strong>", "{0} is the number of pending items", num);
                queueCls += "-active";
            }
        }
    } else {
        queueCls += "-inactive";
    }


    // get status
    String status;
    String message = i18n.get("Replicating to <strong>{0}</strong>", "{0} is an URL", uri);
    String globalIcnCls = "cq-agent-header";
    String statusIcnCls = "cq-agent-status";
    if (agent == null) {
        status = "not active";
        statusIcnCls += "-inactive";
        globalIcnCls += "-off";
    } else {
        try {
            agent.checkValid();
            if (agent.isEnabled()) {
                status = i18n.get("Agent is <strong>enabled.</strong>");
                globalIcnCls += "-on";
                statusIcnCls += "-ok";
            } else {
                status = i18n.get("Agent is <strong>disabled.</strong>");
                globalIcnCls += "-off";
                statusIcnCls += "-disabled";
            }
        } catch (IllegalArgumentException e) {
            message = xssAPI.encodeForHTML(e.getMessage());
            status = i18n.get("Agent is <strong>not valid.</strong>");
            globalIcnCls += "-off";
            statusIcnCls += "-invalid";
        }
    }

%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN">
<html>
<head>
    <title><%= i18n.get("AEM Replication") %> | <%= xssAPI.encodeForHTML(title) %></title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <%
    HtmlLibraryManager htmlMgr = sling.getService(HtmlLibraryManager.class);
    if (htmlMgr != null) {
        htmlMgr.writeIncludes(slingRequest, out, "cq.wcm.edit", "cq.replication");
    }

    %>
    <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>
</head>
<body>
    <h2 class="<%= globalIcnCls %>"><%= xssAPI.encodeForHTML(title) %> (<%= xssAPI.encodeForHTML(id) %>)</h2>
    <%
        String description = properties.get("jcr:description", "");
            %><p><%= xssAPI.encodeForHTML(description) %></p><%
    %><ul>
        <li><div class="li-bullet <%=statusIcnCls%>"><%= status %> <%= message %></div></li>
        <li><div class="li-bullet <%=queueCls%>"><%= queueStr %></div></li>
        <%
            if (cfg != null && cfg.isSpecific()) {
                %><li><%= i18n.get("Agent is ignored on normal replication") %></li><%
            }
            if (cfg != null && cfg.isTriggeredOnModification()) {
                %><li><%= i18n.get("Agent is triggered on modification") %></li><%
            }
            if (cfg != null && cfg.isTriggeredOnOffTime()) {
                %><li><%= i18n.get("Agent is triggered when on-/offtime reached") %></li><%
            }
            if (cfg != null && cfg.isTriggeredOnReceive()) {
                %><li><%= i18n.get("Agent is triggered when receiving replication events") %></li><%
            }
            if (cfg != null && cfg.usedForReverseReplication()) {
                %><li><%= i18n.get("Agent is used for reverse replication") %></li><%
            }
        %>
        <li><a href="<%= xssAPI.getValidHref(currentPage.getPath()) %>.log.html#end"><%= i18n.get("View log") %></a></li>
        <li><a href="javascript:test()"><%= i18n.get("Test Connection") %></a></li>
    </ul>
    <div>
    <br>
    <%
        // draw the 'edit' bar explicitly. since we want to be able to edit the
        // settings on publish too. we are too late here for setting the WCMMode.
        /*
        out.flush();
        if (editContext != null) {
            editContext.getEditConfig().getToolbar().add(0, new Toolbar.Label("Settings"));
            editContext.includeEpilog(slingRequest, slingResponse, WCMMode.EDIT);
        }
        */

    %>
        <script type="text/javascript">
        CQ.WCM.edit({
            "path":"<%= xssAPI.encodeForHTML(resource.getPath()) %>",
            "dialog":"/apps/ams-commons/components/awsagent/dialog",
            "type":"/apps/ams-commons/components/awsagent",
            "editConfig":{
                "xtype":"editbar",
                "listeners":{
                    "afteredit":"REFRESH_PAGE"
                },
                "inlineEditing":CQ.wcm.EditBase.INLINE_MODE_NEVER,
                "disableTargeting": true,
                "actions":[
                    {
                        "xtype":"tbtext",
                        "text":"Settings"
                    },
                    CQ.wcm.EditBase.EDIT
                ]
            }
        });
        </script>
    </div>

    <%
        if (agent != null) {
    %>
    <div id="CQ">
        <div id="cq-queue">
        </div>
    </div>

    <script type="text/javascript">
        CQ.Ext.onReady(function(){
            var queue = new CQ.wcm.ReplicationQueue({
                url: "<%= xssAPI.getValidHref(currentPage.getPath()) %>/jcr:content.queue.json",
                applyTo: "cq-queue",
                height: 400
            });
            queue.loadAgent("<%= xssAPI.encodeForJSString(id) %>");
        });

        function test() {
            window.open(CQ.HTTP.externalize('<%= xssAPI.getValidHref(currentPage.getPath()) %>.test.html'));
        }
    </script>
    <%
        } // if (agent != null)
    %>

</body>
</html>
