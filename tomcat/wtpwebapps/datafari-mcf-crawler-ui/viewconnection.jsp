<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: viewconnection.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
%>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<link rel="StyleSheet" href="style.css" type="text/css" media="screen"/>
	<title>
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.ApacheManifoldCFViewRepositoryConnectionStatus")%>
	</title>

	<script type="text/javascript">
	<!--

	function Delete(connectionName)
	{
		if (confirm("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"viewconnection.DeleteConnection")%> '"+connectionName+"'<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"viewconnection.qmark")%>"))
		{
			document.viewconnection.op.value="Delete";
			document.viewconnection.connname.value=connectionName;
			document.viewconnection.submit();
		}
	}

	function ClearHistory(connectionName)
	{
		if (confirm("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"viewconnection.Thiscommandwillclearallhistoryrelatedto")%> '"+connectionName+"' <%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"viewconnection.period")%>"))
		{
			document.viewconnection.op.value="ClearHistory";
			document.viewconnection.connname.value=connectionName;
			document.viewconnection.submit();
		}
	}

	//-->
	</script>

</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="window">
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.ViewRepositoryConnectionStatus")%></p>
	<form class="standardform" name="viewconnection" action="execute.jsp" method="POST">
		<input type="hidden" name="op" value="Continue"/>
		<input type="hidden" name="type" value="connection"/>
		<input type="hidden" name="connname" value=""/>

<%
    try
    {
	IConnectorManager connectorManager = ConnectorManagerFactory.make(threadContext);
	// Get the connection manager handle
	IRepositoryConnectionManager connManager = RepositoryConnectionManagerFactory.make(threadContext);
	IRepositoryConnectorPool repositoryConnectorPool = RepositoryConnectorPoolFactory.make(threadContext);
	String connectionName = variableContext.getParameter("connname");
	IRepositoryConnection connection = connManager.load(connectionName);
	if (connection == null)
	{
		throw new ManifoldCFException("No such connection: '"+connectionName+"'");
	}
	else
	{
		String description = connection.getDescription();
		if (description == null)
			description = "";
		String className = connection.getClassName();
		String connectorName = connectorManager.getDescription(className);
		if (connectorName == null)
			connectorName = className + Messages.getString(pageContext.getRequest().getLocale(),"viewconnection.uninstalled");
		String authorityName = connection.getACLAuthority();
		if (authorityName == null)
			authorityName = Messages.getString(pageContext.getRequest().getLocale(),"viewconnection.NoneGlobalAuthority");
		int maxCount = connection.getMaxConnections();
		String[] throttles = connection.getThrottles();
		ConfigParams parameters = connection.getConfigParams();

		// Do stuff so we can call out to display the parameters
		//String JSPFolder = RepositoryConnectorFactory.getJSPFolder(threadContext,className);
		//threadContext.save("Parameters",parameters);

		// Now, test the connection.
		String connectionStatus;
		try
		{
			IRepositoryConnector c = repositoryConnectorPool.grab(connection);
			if (c == null)
				connectionStatus = Messages.getString(pageContext.getRequest().getLocale(),"viewconnection.Connectorisnotinstalled");
			else
			{
				try
				{
					connectionStatus = c.check();
				}
				finally
				{
					repositoryConnectorPool.release(connection,c);
				}
			}
		}
		catch (ManifoldCFException e)
		{
			connectionStatus = Messages.getString(pageContext.getRequest().getLocale(),"viewconnection.Threwexception")+" '"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(e.getMessage())+"'";
		}
%>
		<table class="displaytable">
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.NameColon")%></nobr></td><td class="value" colspan="1"><%="<!--connection="+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionName)+"-->"%><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionName)%></nobr></td>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.DescriptionColon")%></nobr></td><td class="value" colspan="1"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.ConnectionTypeColon")%></nobr></td><td class="value" colspan="1"><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectorName)%></nobr></td>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.MaxConnectionsColon")%></nobr></td><td class="value" colspan="1"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Integer.toString(maxCount))%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.AuthorityGroupColon")%></nobr></td><td class="value" colspan="3"><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(authorityName)%></nobr></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.ThrottlingColon")%></nobr></td>
				<td class="boxcell" colspan="3">
					<table class="formtable">
						<tr class="formheaderrow">
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.Binregularexpression")%></nobr></td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.Description")%></nobr></td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.Maxavgfetches")%></nobr></td>
						</tr>
<%
		int j = 0;
		while (j < throttles.length)
		{
%>
						<tr class='<%=((j % 2)==0)?"evenformrow":"oddformrow"%>'>
							<td class="formcolumncell">
								<nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(throttles[j])%></nobr>
							</td>
							<td class="formcolumncell">
<%
			String tdescription = connection.getThrottleDescription(throttles[j]);
			if (tdescription != null && tdescription.length() > 0)
			{
%>
								<nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(tdescription)%></nobr>
<%
			}
%>
							</td>
							<td class="formcolumncell">
								<%=new Long((long)((double)connection.getThrottleValue(throttles[j])*(double)60000.0+0.5)).toString()%>
							</td>
						</tr>
<%
			j++;
		}
		if (j == 0)
		{
%>
						<tr class="formrow"><td colspan="3" class="formmessage"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.NoThrottles")%></nobr></td></tr>
<%
		}
%>
					</table>
				</td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td colspan="4">
<%
		RepositoryConnectorFactory.viewConfiguration(threadContext,className,new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),parameters);
%>
				</td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.ConnectionStatusColon")%></nobr></td><td class="value" colspan="3"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionStatus)%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="message" colspan="4">
					<nobr><a href='<%="viewconnection.jsp?connname="+org.apache.manifoldcf.core.util.URLEncoder.encode(connectionName)%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewconnection.Refresh")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.Refresh")%></a></nobr>
					<nobr><a href='<%="editconnection.jsp?connname="+org.apache.manifoldcf.core.util.URLEncoder.encode(connectionName)%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewconnection.EditThisConnection")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.Edit")%></a></nobr>
					<nobr><a href="javascript:void()" onclick='<%="javascript:Delete(\""+org.apache.manifoldcf.ui.util.Encoder.attributeJavascriptEscape(connectionName)+"\")"%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewconnection.Deletethisconnection")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.Delete")%></a></nobr>
					<nobr><a href="javascript:void()" onclick='<%="javascript:ClearHistory(\""+org.apache.manifoldcf.ui.util.Encoder.attributeJavascriptEscape(connectionName)+"\")"%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewconnection.ClearHistoryAssociatedWithThisConnection")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewconnection.ClearAllRelatedHistory")%></a></nobr>
				</td>
			</tr>
		</table>

<%
	}
    }
    catch (ManifoldCFException e)
    {
	e.printStackTrace();
	variableContext.setParameter("text",e.getMessage());
	variableContext.setParameter("target","listconnections.jsp");
%>
	<jsp:forward page="error.jsp"/>
<%
    }
%>
	    </form>
       </td>
      </tr>
    </table>

</body>

</html>
