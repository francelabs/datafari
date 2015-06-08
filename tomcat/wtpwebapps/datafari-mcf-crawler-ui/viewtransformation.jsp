<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: viewtransformation.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.ApacheManifoldCFViewTransformationConnectionStatus")%>
	</title>

	<script type="text/javascript">
	<!--

	function Delete(connectionName)
	{
		if (confirm("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"viewtransformation.Deletetransformationconnection")%> '"+connectionName+"'<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"viewtransformation.qmark")%>"))
		{
			document.viewconnection.op.value="Delete";
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
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.ViewTransformationConnectionStatus")%></p>
	<form class="standardform" name="viewconnection" action="execute.jsp" method="POST">
		<input type="hidden" name="op" value="Continue"/>
		<input type="hidden" name="type" value="transformation"/>
		<input type="hidden" name="connname" value=""/>

<%
    try
    {
	ITransformationConnectorManager connectorManager = TransformationConnectorManagerFactory.make(threadContext);
	// Get the connection manager handle
	ITransformationConnectionManager connManager = TransformationConnectionManagerFactory.make(threadContext);
	ITransformationConnectorPool transformationConnectorPool = TransformationConnectorPoolFactory.make(threadContext);
	String connectionName = variableContext.getParameter("connname");
	ITransformationConnection connection = connManager.load(connectionName);
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
			connectorName = className + Messages.getString(pageContext.getRequest().getLocale(),"viewtransformation.uninstalled");
		int maxCount = connection.getMaxConnections();
		ConfigParams parameters = connection.getConfigParams();

		// Do stuff so we can call out to display the parameters
		//String JSPFolder = TransformationConnectorFactory.getJSPFolder(threadContext,className);
		//threadContext.save("Parameters",parameters);

		// Now, test the connection.
		String connectionStatus;
		try
		{
			ITransformationConnector c = transformationConnectorPool.grab(connection);
			if (c == null)
				connectionStatus = Messages.getString(pageContext.getRequest().getLocale(),"viewtransformation.Connectorisnotinstalled");
			else
			{
				try
				{
					connectionStatus = c.check();
				}
				finally
				{
					transformationConnectorPool.release(connection,c);
				}
			}
		}
		catch (ManifoldCFException e)
		{
			e.printStackTrace();
			connectionStatus = Messages.getString(pageContext.getRequest().getLocale(),"viewtransformation.Threwexception")+" '"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(e.getMessage())+"'";
		}
%>
		<table class="displaytable">
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.NameColon")%></nobr></td><td class="value" colspan="1"><%="<!--connection="+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionName)+"-->"%><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionName)%></nobr></td>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.DescriptionColon")%></nobr></td><td class="value" colspan="1"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.ConnectionTypeColon")%></nobr></td><td class="value" colspan="1"><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectorName)%></nobr></td>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.MaxConnectionsColon")%></nobr></td><td class="value" colspan="1"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(Integer.toString(maxCount))%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td colspan="4">
<%
		TransformationConnectorFactory.viewConfiguration(threadContext,className,new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),parameters);
%>
				</td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.ConnectionStatusColon")%></nobr></td><td class="value" colspan="3"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionStatus)%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="message" colspan="4">
					<nobr><a href='<%="viewtransformation.jsp?connname="+org.apache.manifoldcf.core.util.URLEncoder.encode(connectionName)%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewtransformation.Refresh")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.Refresh")%></a></nobr>
					<nobr><a href='<%="edittransformation.jsp?connname="+org.apache.manifoldcf.core.util.URLEncoder.encode(connectionName)%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewtransformation.EditThisTransformationConnection")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.Edit")%></a></nobr>
					<nobr><a href="javascript:void()" onclick='<%="javascript:Delete(\""+org.apache.manifoldcf.ui.util.Encoder.attributeJavascriptEscape(connectionName)+"\")"%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewtransformation.DeleteThisTransformationConnection")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewtransformation.Delete")%></a></nobr>
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
	variableContext.setParameter("target","listtransformations.jsp");
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
