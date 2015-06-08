<%@ include file="adminHeaders.jsp" %>
<%

/* $Id: editgroup.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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

    // The contract of this edit page is as follows.  It is either called directly, in which case it is expected to be creating
    // a connection or beginning the process of editing an existing connection, or it is called via redirection from execute.jsp, in which case
    // the connection object being edited will be placed in the thread context under the name "GroupObject".
    try
    {
	// Get the group manager
	IAuthorityGroupManager authGroupManager = AuthorityGroupManagerFactory.make(threadContext);
	
	// Figure out what the current tab name is.
	String tabName = variableContext.getParameter("tabname");
	if (tabName == null || tabName.length() == 0)
		tabName = Messages.getString(pageContext.getRequest().getLocale(),"editgroup.Name");

	String groupName = null;
	IAuthorityGroup group = (IAuthorityGroup)threadContext.get("GroupObject");
	if (group == null)
	{
		// We did not go through execute.jsp
		// We might have received an argument specifying the connection name.
		groupName = variableContext.getParameter("groupname");
		// If the groupname is not null, load the connection description and prepopulate everything with what comes from it.
		if (groupName != null && groupName.length() > 0)
		{
			group = authGroupManager.load(groupName);
		}
	}

	// Setup default fields
	boolean isNew = true;
	String description = "";
	
	if (group != null)
	{
		// Set up values
		isNew = group.getIsNew();
		groupName = group.getName();
		description = group.getDescription();
	}
	else
		groupName = null;

	if (groupName == null)
		groupName = "";

	// Initialize tabs array
	ArrayList tabsArray = new ArrayList();

	// Set up the predefined tabs
	tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editgroup.Name"));
%>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<link rel="StyleSheet" href="style.css" type="text/css" media="screen"/>
	<title>
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editgroup.ApacheManifoldCFEditAuthorityGroup")%>
	</title>

	<script type="text/javascript">
	<!--
	// Use this method to repost the form and pick a new tab
	function SelectTab(newtab)
	{
		if (checkForm())
		{
			document.editgroup.tabname.value = newtab;
			document.editgroup.submit();
		}
	}

	// Use this method to repost the form,
	// and set the anchor request.
	function postFormSetAnchor(anchorValue)
	{
		if (checkForm())
		{
			if (anchorValue != "")
				document.group.action = document.editgroup.action + "#" + anchorValue;
			document.editgroup.submit();
		}
	}

	// Use this method to repost the form
	function postForm()
	{
		if (checkForm())
		{
			document.editgroup.submit();
		}
	}

	function Save()
	{
		if (checkForm())
		{
			// Can't submit until all required fields have been set.
			// Some of these don't live on the current tab, so don't set
			// focus.

			// Check our part of the form, for save
			if (editgroup.groupname.value == "")
			{
				alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editgroup.AuthorityGroupMustHaveAName")%>");
				SelectTab("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editgroup.Name")%>");
				document.editgroup.groupname.focus();
				return;
			}
			if (window.checkConfigForSave)
			{
				if (!checkConfigForSave())
					return;
			}
			document.editgroup.op.value="Save";
			document.editgroup.submit();
		}
	}

	function Continue()
	{
		document.editgroup.op.value="Continue";
		postForm();
	}

	function Cancel()
	{
		document.editgroup.op.value="Cancel";
		document.editgroup.submit();
	}

	function checkForm()
	{
		return true;
	}

	//-->
	</script>
</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="darkwindow">


	<form class="standardform" name="editgroup" action="execute.jsp" method="POST" enctype="multipart/form-data">
	  <input type="hidden" name="op" value="Continue"/>
	  <input type="hidden" name="type" value="group"/>
	  <input type="hidden" name="tabname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(tabName)%>'/>
	  <input type="hidden" name="isnewconnection" value='<%=(isNew?"true":"false")%>'/>
	    <table class="tabtable">
	      <tr class="tabspacerrow">
		<td class="spacertab" colspan="<%=tabsArray.size()%>"></td>
		<td class="remaindertab" rowspan="3">
<%
	if (description.length() > 0)
	{
%>
			<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editgroup.EditGroup")%> '<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%>'</nobr>
<%
	}
	else
	{
%>
		          <nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editgroup.EditAGroup")%></nobr>
<%
	}
%>
		</td>
	      </tr>
	      <tr class="tabsequencerow">
		<td class="blanksequencetab" colspan="<%=tabsArray.size()%>"></td>
	      </tr>
	      <tr class="tabrow">
<%
	int tabNum = 0;
	while (tabNum < tabsArray.size())
	{
		String tab = (String)tabsArray.get(tabNum++);
		if (tab.equals(tabName))
		{
%>
		      <td class="activetab"><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(tab)%></nobr></td>
<%
		}
		else
		{
%>
		      <td class="passivetab"><nobr><a href="javascript:void(0);" alt='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(tab)+" "+Messages.getAttributeString(pageContext.getRequest().getLocale(),"editgroup.tab")%>' onclick='<%="javascript:SelectTab(\""+tab+"\");return false;"%>'><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(tab)%></a></nobr></td>
<%
		}
	}
%>
	      </tr>
	      <tr class="tabbodyrow">
		<td class="tabbody" colspan='<%=Integer.toString(tabsArray.size()+1)%>'>

<%

	// Name tab
	if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editgroup.Name")))
	{
%>
		    <table class="displaytable">
			<tr><td class="separator" colspan="5"><hr/></td></tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editgroup.NameColon")%></nobr></td>
				<td class="value" colspan="4">
<%
	    // If the group doesn't exist yet, we are allowed to change the name.
	    if (isNew)
	    {
%>
					<input type="text" size="32" name="groupname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(groupName)%>'/>
<%
	    }
	    else
	    {
%>
					<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(groupName)%>
					<input type="hidden" name="groupname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(groupName)%>'/>
<%
	    }
%>
				</td>
			</tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editgroup.DescriptionColon")%></nobr></td>
				<td class="value" colspan="4">
					<input type="text" size="50" name="description" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(description)%>'/>
				</td>
			</tr>
		    </table>
<%
	}
	else
	{
		// Hiddens for the Name tab
%>
		    <input type="hidden" name="groupname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(groupName)%>'/>
		    <input type="hidden" name="description" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(description)%>'/>
<%
	}
%>
		    <table class="displaytable">
			<tr><td class="separator" colspan="4"><hr/></td></tr>
			<tr><td class="message" colspan="4"><nobr>
			    <input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editgroup.Save")%>" onClick="javascript:Save()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editgroup.SaveThisAuthorityGroup")%>"/>
			    &nbsp;<input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editgroup.Cancel")%>" onClick="javascript:Cancel()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editgroup.CancelAuthorityGroupEditing")%>"/></nobr></td>
			</tr>
		    </table>
		</td>
	      </tr>
	    </table>
	</form>
       </td>
      </tr>
    </table>

</body>

</html>

<%
    }
    catch (ManifoldCFException e)
    {
	e.printStackTrace();
	variableContext.setParameter("text",e.getMessage());
	variableContext.setParameter("target","listauthorities.jsp");
%>
	<jsp:forward page="error.jsp"/>
<%
    }
%>

