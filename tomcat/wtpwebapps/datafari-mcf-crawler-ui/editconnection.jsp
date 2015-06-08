<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: editconnection.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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

<%
    // The contract of this edit page is as follows.  It is either called directly, in which case it is expected to be creating
    // a connection or beginning the process of editing an existing connection, or it is called via redirection from execute.jsp, in which case
    // the connection object being edited will be placed in the thread context under the name "ConnectionObject".
    try
    {
	// Get the connection manager handle
	IRepositoryConnectionManager connMgr = RepositoryConnectionManagerFactory.make(threadContext);
	// Also get the list of available connectors
	IConnectorManager connectorManager = ConnectorManagerFactory.make(threadContext);
	IAuthorityGroupManager authGroupManager = AuthorityGroupManagerFactory.make(threadContext);

	// Figure out what the current tab name is.
	String tabName = variableContext.getParameter("tabname");
	if (tabName == null || tabName.length() == 0)
		tabName = Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Name");

	String connectionName = null;
	IRepositoryConnection connection = (IRepositoryConnection)threadContext.get("ConnectionObject");
	if (connection == null)
	{
		// We did not go through execute.jsp
		// We might have received an argument specifying the connection name.
		connectionName = variableContext.getParameter("connname");
		// If the connectionname is not null, load the connection description and prepopulate everything with what comes from it.
		if (connectionName != null && connectionName.length() > 0)
		{
			connection = connMgr.load(connectionName);
		}
	}
	
	// Set up default fields.
	boolean isNew = true;
	String description = "";
	String className = "";
	String authorityName = null;
	int maxConnections = 10;
	// Fetches per minute
	ArrayList throttles = new ArrayList();
	ConfigParams parameters = new ConfigParams();

	// If there's a connection object, set up all our parameters from it.
	if (connection != null)
	{
		// Set up values
		isNew = connection.getIsNew();
		connectionName = connection.getName();
		description = connection.getDescription();
		className = connection.getClassName();
		parameters = connection.getConfigParams();
		authorityName = connection.getACLAuthority();
		maxConnections = connection.getMaxConnections();
		String[] throttlesX = connection.getThrottles();
		int j = 0;
		while (j < throttlesX.length)
		{
			String throttleRegexp = throttlesX[j++];
			Map map = new HashMap();
			map.put("regexp",throttleRegexp);
			map.put("description",connection.getThrottleDescription(throttleRegexp));
			map.put("value",new Long((long)(((double)connection.getThrottleValue(throttleRegexp) * (double)60000.0) + 0.5)));
			throttles.add(map);
		}
	}
	else
		connectionName = null;

	if (connectionName == null)
		connectionName = "";

	// Initialize tabs array.
	ArrayList tabsArray = new ArrayList();

	// Set up the predefined tabs
	tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Name"));
	tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Type"));
	if (className.length() > 0)
		tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Throttling"));

%>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<link rel="StyleSheet" href="style.css" type="text/css" media="screen"/>
	<title>
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.ApacheManifoldCFEditConnection")%>
	</title>

	<script type="text/javascript">
	<!--

	// Use this method to repost the form and pick a new tab
	function SelectTab(newtab)
	{
		if (checkForm())
		{
			document.editconnection.tabname.value = newtab;
			document.editconnection.submit();
		}
	}

	// Use this method to repost the form,
	// and set the anchor request.
	function postFormSetAnchor(anchorValue)
	{
		if (checkForm())
		{
			if (anchorValue != "")
				document.editconnection.action = document.editconnection.action + "#" + anchorValue;
			document.editconnection.submit();
		}
	}

	// Use this method to repost the form
	function postForm()
	{
		if (checkForm())
		{
			document.editconnection.submit();
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
			if (editconnection.connname.value == "")
			{
				alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editconnection.ConnectionMustHaveAName")%>");
				SelectTab("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editconnection.Name")%>");
				document.editconnection.connname.focus();
				return;
			}
			if (window.checkConfigForSave)
			{
				if (!checkConfigForSave())
					return;
			}
			document.editconnection.op.value="Save";
			document.editconnection.submit();
		}
	}

	function Continue()
	{
		document.editconnection.op.value="Continue";
		postForm();
	}

	function Cancel()
	{
		document.editconnection.op.value="Cancel";
		document.editconnection.submit();
	}

	function DeleteThrottle(i)
	{
		document.editconnection.throttleop.value="Delete";
		document.editconnection.throttlenumber.value=i;
		postForm();
	}

	function AddThrottle()
	{
		if (!isInteger(editconnection.throttlevalue.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editconnection.ThrottleRateMustBeAnInteger")%>");
			document.editconnection.throttlevalue.focus();
			return;
		}
		if (!isRegularExpression(editconnection.throttle.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editconnection.TheThrottleExpressionMustBeAValidRegularExpression")%>");
			editconnection.throttle.focus();
			return;
		}
		document.editconnection.throttleop.value="Add";
		postForm();
	}

	function checkForm()
	{
		if (!checkConnectionCount())
			return false;
		if (window.checkConfig)
			return checkConfig();
		return true;
	}

	function checkConnectionCount()
	{
		if (!isInteger(editconnection.maxconnections.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editconnection.TheMaximumNumberOfConnectionsMustBeAValidInteger")%>");
			editconnection.maxconnections.focus();
			return false;
		}
		return true;
	}

	function isRegularExpression(value)
	{
		try
		{
			var foo = "teststring";
                        foo.search(value.replace(/\(\?i\)/,""));
			return true;
		}
		catch (e)
		{
			return false;
		}

	}
	
	function isInteger(value)
	{
		var anum=/(^\d+$)/;
		return anum.test(value);
	}

	//-->
	</script>
<%
	RepositoryConnectorFactory.outputConfigurationHeader(threadContext,className,new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),parameters,tabsArray);
%>

</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="darkwindow">
<%
	// Get connector list; need this to decide what to do
	IResultSet set = connectorManager.getConnectors();
	if (set.getRowCount() == 0)
	{
%>
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.EditRepositoryConnection")%></p>
	<table class="displaytable"><tr><td class="message"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.NoRepositoryConnectorsRegistered")%></td></tr></table>
<%
	}
	else
	{
%>

	<form class="standardform" name="editconnection" action="execute.jsp" method="POST" enctype="multipart/form-data">
	    <input type="hidden" name="op" value="Continue"/>
	    <input type="hidden" name="type" value="connection"/>
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
			<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.EditConnection")%> '<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%>'</nobr>
<%
	  }
	  else
	  {
%>
		          <nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.EditAConnection")%></nobr>
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
		      <td class="passivetab"><nobr><a href="javascript:void(0);" alt='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(tab)+" "+Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.tab")%>' onclick='<%="javascript:SelectTab(\""+tab+"\");return false;"%>'><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(tab)%></a></nobr></td>
<%
		}
	  }
%>
	      </tr>
	      <tr class="tabbodyrow">
		<td class="tabbody" colspan='<%=Integer.toString(tabsArray.size()+1)%>'>
<%

	  // Name tab
	  if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Name")))
	  {
%>
		    <table class="displaytable">
			<tr><td class="separator" colspan="5"><hr/></td></tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.NameColon")%></nobr></td><td class="value" colspan="4">
<%
	    // If the connection doesn't exist yet, we are allowed to change the name.
	    if (isNew)
	    {
%>
					<input type="text" size="32" name="connname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionName)%>'/>
<%
	    }
	    else
	    {
%>
					<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionName)%>
					<input type="hidden" name="connname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionName)%>'/>
<%
	    }
%>
				</td>
			</tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.DescriptionColon")%></nobr></td><td class="value" colspan="4">
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
		    <input type="hidden" name="connname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionName)%>'/>
		    <input type="hidden" name="description" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(description)%>'/>
<%
	  }


	  // "Type" tab
	  if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Type")))
	  {
%>
		    <table class="displaytable">
			<tr><td class="separator" colspan="5"><hr/></td></tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.ConnectionTypeColon")%></nobr></td><td class="value" colspan="4">
<%
	    if (className.length() > 0)
	    {
		String value = connectorManager.getDescription(className);
		if (value == null)
		{
%>
					<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.UNREGISTERED")%> <%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(className)%></nobr>
<%
		}
		else
		{
%>
					<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value)%>
<%
		}
%>
					<input type="hidden" name="classname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(className)%>'/>
<%
	    }
	    else
	    {
		int i = 0;
%>
					<select name="classname" size="1">
<%
		while (i < set.getRowCount())
		{
			IResultRow row = set.getRow(i++);
			String thisClassName = row.getValue("classname").toString();
			String thisDescription = row.getValue("description").toString();
%>
						<option value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(thisClassName)%>'
							<%=className.equals(thisClassName)?"selected=\"selected\"":""%>><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(thisDescription)%></option>
<%
		}
%>
					</select>
<%
	    }
%>
				</td>
			</tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.AuthorityGroupColon")%></nobr></td>
				<td class="value" colspan="4">
<%
	    IAuthorityGroup[] set2 = authGroupManager.getAllGroups();
	    int i = 0;
%>
					<select name="authorityname" size="1">
						<option value="_none_" <%=(authorityName==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.GlobalAuthority")%></option>
<%
	    while (i < set2.length)
	    {
		IAuthorityGroup row = set2[i++];
		String thisAuthorityName = row.getName();
		String thisDescription = row.getDescription();
		if (thisDescription == null || thisDescription.length() == 0)
			thisDescription = thisAuthorityName;
%>
						<option value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(thisAuthorityName)%>'
							<%=(authorityName!=null&&authorityName.equals(thisAuthorityName))?"selected=\"selected\"":""%>><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(thisDescription)%></option>
<%
	    }
%>
					</select>
				</td>
			</tr>
		    </table>
<%
	  }
	  else
	  {
		// Hiddens for the "Type" tab
%>
		    <input type="hidden" name="classname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(className)%>'/>
		    <input type="hidden" name="authorityname" value='<%=(authorityName==null)?"_none_":org.apache.manifoldcf.ui.util.Encoder.attributeEscape(authorityName)%>'/>
<%
	  }


	  // The "Throttling" tab
%>
		    <input type="hidden" name="throttlecount" value='<%=Integer.toString(throttles.size())%>'/>
<%
	  if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Throttling")))
	  {
%>
		    <table class="displaytable">
			<tr><td class="separator" colspan="2"><hr/></td></tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.MaxconnectionsColon")%></nobr></td>
				<td class="value"><input type="text" size="6" name="maxconnections" value='<%=Integer.toString(maxConnections)%>'/></td>
			</tr>
			<tr>
				<td class="separator" colspan="2"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.ThrottlingColon")%></nobr></td>
				<td class="boxcell" colspan="4">
					<input type="hidden" name="throttleop" value="Continue"/>
					<input type="hidden" name="throttlenumber" value=""/>
					<table class="formtable">
					    <tr class="formheaderrow">
						<td class="formcolumnheader"></td>
						<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.BinRegularExpression")%></nobr></td>
						<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.Description")%></nobr></td>
						<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.MaxAvgFetchesMin")%></nobr></td>
					    </tr>
<%
		int k = 0;
		while (k < throttles.size())
		{
			Map map = (Map)throttles.get(k);
			String regexp = (String)map.get("regexp");
			String desc = (String)map.get("description");
			if (desc == null)
				desc = "";
			Long value = (Long)map.get("value");
%>
					    <tr class='<%=((k % 2)==0)?"evenformrow":"oddformrow"%>'>
						<td class="formcolumncell"><input type="button" value="<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.Delete")%>" alt='<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.Deletethrottle")+" "+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(regexp)%>' onclick='<%="javascript:DeleteThrottle("+Integer.toString(k)+");"%>'/></td>
						<td class="formcolumncell">
						    <input type="hidden" name='<%="throttle_"+Integer.toString(k)%>' value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(regexp)%>'/>
						    <input type="hidden" name='<%="throttledesc_"+Integer.toString(k)%>' value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(desc)%>'/>
						    <input type="hidden" name='<%="throttlevalue_"+Integer.toString(k)%>' value='<%=value.toString()%>'/>
						    <nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(regexp)%></nobr>
						</td>
						<td class="formcolumncell">
<%
			if (desc.length() > 0)
			{
%>
							<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(desc)%>
<%
			}
%>
						</td>
						<td class="formcolumncell"><%=value.toString()%></td>
					    </tr>
<%
			k++;
		}
		if (k == 0)
		{
%>
					    <tr class="formrow"><td colspan="4" class="formcolumnmessage"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editconnection.NoThrottlingSpecified")%></nobr></td></tr>
<%
		}
%>
					    <tr class="formrow"><td colspan="4" class="formseparator"><hr/></td></tr>
					    <tr class="formrow">
						<td class="formcolumncell"><input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.Add")%>" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.Addthrottle")%>" onclick="javascript:AddThrottle();"/></td>
						<td class="formcolumncell"><input type="text" name="throttle" size="30" value=""/></td>
						<td class="formcolumncell"><input type="text" name="throttledesc" size="30" value=""/></td>
						<td class="formcolumncell"><input type="text" name="throttlevalue" size="5" value=""/></td>
					    </tr>
					</table>
				</td>
			</tr>
		    </table>
<%
	  }
	  else
	  {
		// Hiddens for "Throttling" tab
%>
		    <input type="hidden" name="maxconnections" value='<%=Integer.toString(maxConnections)%>'/>
<%
		int k = 0;
		while (k < throttles.size())
		{
			Map map = (Map)throttles.get(k);
			String regexp = (String)map.get("regexp");
			String desc = (String)map.get("description");
			if (desc == null)
				desc = "";
			Long value = (Long)map.get("value");
%>
		    <input type="hidden" name='<%="throttle_"+Integer.toString(k)%>' value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(regexp)%>'/>
		    <input type="hidden" name='<%="throttledesc_"+Integer.toString(k)%>' value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(desc)%>'/>
		    <input type="hidden" name='<%="throttlevalue_"+Integer.toString(k)%>' value='<%=value.toString()%>'/>
<%
			k++;
		}
	  }

	  if (className.length() > 0)
		RepositoryConnectorFactory.outputConfigurationBody(threadContext,className,new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),parameters,tabName);
%>
		    <table class="displaytable">
			<tr><td class="separator" colspan="4"><hr/></td></tr>
			<tr><td class="message" colspan="4"><nobr>
<%
	  if (className.length() > 0)
	  {
%>
			    <input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.Save")%>" onClick="javascript:Save()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.SaveThisAuthorityConnection")%>"/>
<%
	  }
	  else
	  {
		if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editconnection.Type")))
		{
%>
			    <input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.Continue")%>" onClick="javascript:Continue()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.ContinueToNextPage")%>"/>
<%
		}
	  }
%>
			    &nbsp;<input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.Cancel")%>" onClick="javascript:Cancel()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editconnection.CancelConnectionEditing")%>"/></nobr></td>
			</tr>
		    </table>
		</td>
	      </tr>
	    </table>
	</form>

<%
	}
%>

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
	variableContext.setParameter("target","listconnections.jsp");
%>
	<jsp:forward page="error.jsp"/>
<%
    }
%>

