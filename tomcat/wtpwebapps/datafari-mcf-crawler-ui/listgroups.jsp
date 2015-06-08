<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: listgroups.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.ApacheManifoldCFListAuthorityGroups")%>
	</title>

	<script type="text/javascript">
	<!--

	function Delete(groupName)
	{
		if (confirm("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"listgroups.DeleteAuthorityGroup")%> '"+groupName+"'?"))
		{
			document.listgroups.op.value="Delete";
			document.listgroups.groupname.value=groupName;
			document.listgroups.submit();
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
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.ListOfAuthorityGroups")%></p>
	<form class="standardform" name="listgroups" action="execute.jsp" method="POST">
		<input type="hidden" name="op" value="Continue"/>
		<input type="hidden" name="type" value="group"/>
		<input type="hidden" name="groupname" value=""/>

<%
    try
    {
	// Get the authority group manager handle
	IAuthorityGroupManager manager = AuthorityGroupManagerFactory.make(threadContext);
	IAuthorityGroup[] groups = manager.getAllGroups();
%>
		<table class="datatable">
			<tr>
				<td class="separator" colspan="5"><hr/></td>
			</tr>
			<tr class="headerrow">
				<td class="columnheader"></td>
				<td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.Name")%></td>
				<td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.Description")%></td>
			</tr>
<%
	int i = 0;
	while (i < groups.length)
	{
		IAuthorityGroup group = groups[i++];

		String name = group.getName();
		String description = group.getDescription();
		if (description == null)
			description = "";

%>
		<tr <%="class=\""+((i%2==0)?"evendatarow":"odddatarow")+"\""%>>
			<td class="columncell">
				<nobr>
					<a href='<%="viewgroup.jsp?groupname="+org.apache.manifoldcf.core.util.URLEncoder.encode(name)%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listgroups.View")+" "+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(name)%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.View")%></a>
					<a href='<%="editgroup.jsp?groupname="+org.apache.manifoldcf.core.util.URLEncoder.encode(name)%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listgroups.Edit")+" "+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(name)%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.Edit")%></a>
					<a href="javascript:void()" onclick='<%="javascript:Delete(\""+org.apache.manifoldcf.ui.util.Encoder.attributeJavascriptEscape(name)+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listgroups.Delete")+" "+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(name)%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.Delete")%></a>
				</nobr>
			</td>
			<td class="columncell"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(name)%></td>
			<td class="columncell"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%></td>
		</tr>
<%
	}
%>
			<tr>
				<td class="separator" colspan="5"><hr/></td>
			</tr>
			<tr><td class="message" colspan="5"><a href="editgroup.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listgroups.AddNewGroup")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listgroups.AddaNewGroup")%></a></td></tr>
		</table>

<%
    }
    catch (ManifoldCFException e)
    {
	e.printStackTrace();
	variableContext.setParameter("text",e.getMessage());
	variableContext.setParameter("target","index.jsp");
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
