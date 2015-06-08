<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: viewgroup.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewgroup.ApacheManifoldCFViewGroup")%>
	</title>

	<script type="text/javascript">
	<!--

	function Delete(groupName)
	{
		document.viewgroup.op.value="Delete";
		document.viewgroup.groupname.value=groupName;
		document.viewgroup.submit();
	}

	//-->
	</script>

</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="window">
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewgroup.ViewAuthorityGroup")%></p>

	<form class="standardform" name="viewgroup" action="execute.jsp" method="POST">
		<input type="hidden" name="op" value="Continue"/>
		<input type="hidden" name="type" value="group"/>
		<input type="hidden" name="groupname" value=""/>

<%
    try
    {
	// Get the job manager handle
	IAuthorityGroupManager manager = AuthorityGroupManagerFactory.make(threadContext);
	String groupName = variableContext.getParameter("groupname");
	IAuthorityGroup group = manager.load(groupName);
	if (group == null)
	{
		throw new ManifoldCFException("No such group: "+groupName);
	}
	else
	{
		String description = group.getDescription();
		if (description == null)
			description = "";
%>
		<table class="displaytable">
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewgroup.NameColon")%></nobr></td>
				<td class="value" colspan="1"><%="<!--group="+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(groupName)+"-->"%><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(groupName)%></nobr></td>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewgroup.DescriptionColon")%></nobr></td>
				<td class="value" colspan="1"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%></td>
			</tr>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="message" colspan="4">
					<a href='<%="editgroup.jsp?groupname="+org.apache.manifoldcf.core.util.URLEncoder.encode(groupName)%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewgroup.EditThisAuthorityGroup")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewgroup.Edit")%></a>&nbsp;<a href="javascript:void()" onclick='<%="javascript:Delete(\""+org.apache.manifoldcf.ui.util.Encoder.attributeJavascriptEscape(groupName)+"\")"%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"viewgroup.DeleteThisAuthorityGroup")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"viewgroup.Delete")%></a>
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
	variableContext.setParameter("target","listgroups.jsp");
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
