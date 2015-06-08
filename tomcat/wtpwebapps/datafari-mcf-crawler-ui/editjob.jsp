<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: editjob.jsp 1625187 2014-09-15 23:42:59Z kwright $ */

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
    // a job or beginning the process of editing an existing job, or it is called via redirection from execute.jsp, in which case
    // the job object being edited will be placed in the thread context under the name "JobObject".
    // It may also be called directly with a parameter of "origjobid", which implies that a copy operation should be started.
    try
    {
	// Get the job manager handle
	IJobManager manager = JobManagerFactory.make(threadContext);
	IRepositoryConnectionManager connMgr = RepositoryConnectionManagerFactory.make(threadContext);
	IRepositoryConnection[] connList = connMgr.getAllConnections();
	IOutputConnectionManager outputMgr = OutputConnectionManagerFactory.make(threadContext);
	IOutputConnection[] outputList = outputMgr.getAllConnections();
	ITransformationConnectionManager transformationMgr = TransformationConnectionManagerFactory.make(threadContext);
	ITransformationConnection[] transformationList = transformationMgr.getAllConnections();

	IOutputConnectorPool outputConnectorPool = OutputConnectorPoolFactory.make(threadContext);
	IRepositoryConnectorPool repositoryConnectorPool = RepositoryConnectorPoolFactory.make(threadContext);
	ITransformationConnectorPool transformationConnectorPool = TransformationConnectorPoolFactory.make(threadContext);

	// Figure out tab name and sequence number
	String tabName = variableContext.getParameter("tabname");
	String tabSequenceNumber = variableContext.getParameter("sequencenumber");
	int tabSequenceInt;
	if (tabName == null || tabName.length() == 0)
	{
		tabName = Messages.getString(pageContext.getRequest().getLocale(),"editjob.Name");
		tabSequenceInt = -1;
	}
	else
	{
		if (tabSequenceNumber == null || tabSequenceNumber.length() == 0)
			tabSequenceInt = -1;
		else
			tabSequenceInt = Integer.parseInt(tabSequenceNumber);
	}
	
	// Get a loaded job object, somehow.
	String jobID = null;
	IJobDescription job = (IJobDescription)threadContext.get("JobObject");
	if (job == null)
	{
		// We did not go through execute.jsp
		// We might have received an argument specifying the connection name.
		jobID = variableContext.getParameter("jobid");
		String origJobID = variableContext.getParameter("origjobid");
		if (origJobID == null || origJobID.length() == 0)
			origJobID = jobID;
		if (origJobID != null)
			job = manager.load(new Long(origJobID));
	}
	else
		jobID = job.getID().toString();

	// Setup default fields
	String connectionName = "";
	String description = "";
	int type = IJobDescription.TYPE_SPECIFIED;
	Specification documentSpecification = new Specification();
	
	// Pipeline data
	String[] pipelineConnectionNames = new String[0];
	String[] pipelineDescriptions = new String[0];
	boolean[] pipelineIsOutputs = new boolean[0];
	int[] pipelinePrerequisites = new int[0];
	Specification[] pipelineSpecifications = new Specification[0];
	
	ArrayList scheduleRecords = new ArrayList();

	EnumeratedValues dayOfWeek = null;
	EnumeratedValues dayOfMonth = null;
	EnumeratedValues monthOfYear = null;
	EnumeratedValues year = null;
	EnumeratedValues hourOfDay = null;
	EnumeratedValues minutesOfHour = null;
	// Duration in minutes
	Long duration = null;
	// RequestMinimum flag
	boolean requestMinimum = false;

	// Priority
	int priority = 5;
	// Minimum recrawl interval (Default: 1 day)
	Long recrawlInterval = new Long(60L * 24L);
	// Maximum recrawl interval (Default: none)
	Long maxRecrawlInterval = null;
	// Reseed interval (Default: 60 minutes)
	Long reseedInterval = new Long(60L);
	// Expiration interval (Default: never)
	Long expirationInterval = null;
	// Start method
	int startMethod = IJobDescription.START_DISABLE;
	// Hopcount mode
	int hopcountMode = IJobDescription.HOPCOUNT_ACCURATE;
	// Hop filters
	Map hopFilterMap = new HashMap();

	// If the job is not null, prepopulate everything with what comes from it.
	if (job != null)
	{
		// Set up values
		description = job.getDescription();
		connectionName = job.getConnectionName();
		
		pipelineConnectionNames = new String[job.countPipelineStages()];
		pipelineDescriptions = new String[job.countPipelineStages()];
		pipelineIsOutputs = new boolean[job.countPipelineStages()];
		pipelinePrerequisites = new int[job.countPipelineStages()];
		pipelineSpecifications = new Specification[job.countPipelineStages()];
		for (int j = 0; j < job.countPipelineStages(); j++)
		{
			pipelineConnectionNames[j] = job.getPipelineStageConnectionName(j);
			pipelineDescriptions[j] = job.getPipelineStageDescription(j);
			pipelineIsOutputs[j] = job.getPipelineStageIsOutputConnection(j);
			pipelinePrerequisites[j] = job.getPipelineStagePrerequisite(j);
			pipelineSpecifications[j] = job.getPipelineStageSpecification(j);
		}
		type = job.getType();
		startMethod = job.getStartMethod();
		hopcountMode = job.getHopcountMode();
		documentSpecification = job.getSpecification();
		// Fill in schedule records from job
		for (int j = 0; j < job.getScheduleRecordCount(); j++)
		{
			scheduleRecords.add(job.getScheduleRecord(j));
		}

		priority = job.getPriority();
		Long value = job.getInterval();
		recrawlInterval = (value==null)?null:new Long(value.longValue()/60000L);
		value = job.getMaxInterval();
		maxRecrawlInterval = (value==null)?null:new Long(value.longValue()/60000L);
		value = job.getReseedInterval();
		reseedInterval = (value==null)?null:new Long(value.longValue()/60000L);
		value = job.getExpiration();
		expirationInterval = (value==null)?null:new Long(value.longValue()/60000L);
		hopFilterMap = job.getHopCountFilters();
	}


	// This form reposts to itself.  It basically only allows the connection to be picked once; once done, the repost occurs
	// and cannot be undone.
	// Therefore, there are three possible entry conditions:
	// 1) no jobid w/no connection name, which indicates a brand-new job without a chosen connection
	// 2) no jobid w/a connection name, which indicates that the connection at least has been chosen
	// 3) a jobid and a connection name, which indicates that we are editing an existing connection.
	// There are similar combinations for output connections.

	int model = IRepositoryConnector.MODEL_ADD_CHANGE_DELETE;
	String[] relationshipTypes = null;
	List<String> tabsArray = new ArrayList<String>();
	List<Integer> sequenceArray = new ArrayList<Integer>();
	
	IRepositoryConnection connection = null;
	if (connectionName.length() > 0)
	{
		connection = connMgr.load(connectionName);
		model = RepositoryConnectorFactory.getConnectorModel(threadContext,connection.getClassName());
		relationshipTypes = RepositoryConnectorFactory.getRelationshipTypes(threadContext,connection.getClassName());
	}

	// Set up the predefined tabs
	tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Name"));
	sequenceArray.add(null);
	tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Connection"));
	sequenceArray.add(null);
	if (connectionName.length() > 0)
	{
		tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Scheduling"));
		sequenceArray.add(null);
		if (relationshipTypes != null && relationshipTypes.length > 0)
		{
			tabsArray.add(Messages.getString(pageContext.getRequest().getLocale(),"editjob.HopFilters"));
			sequenceArray.add(null);
		}
	}

	// Get the names of the various Javascript methods we'll need to call
	String checkMethod = "checkSpecification";
	String saveCheckMethod = "checkSpecificationForSave";
	String[] pipelineCheckMethods = new String[pipelineConnectionNames.length];
	String[] pipelineCheckForSaveMethods = new String[pipelineConnectionNames.length];
	for (int j = 0; j < pipelineConnectionNames.length; j++)
	{
		pipelineCheckMethods[j] = "unknown";
		pipelineCheckForSaveMethods[j] = "unknown";
	}
	
	if (connection != null)
	{
		IRepositoryConnector connector = RepositoryConnectorFactory.getConnectorNoCheck(connection.getClassName());
		if (connector != null)
		{
			checkMethod = connector.getFormCheckJavascriptMethodName(0);
			saveCheckMethod = connector.getFormPresaveCheckJavascriptMethodName(0);
		}
	}

	for (int j = 0; j < pipelineConnectionNames.length; j++)
	{
		if (pipelineIsOutputs[j])
		{
			IOutputConnection outputConnection = outputMgr.load(pipelineConnectionNames[j]);
			if (outputConnection != null)
			{
				IOutputConnector outputConnector = OutputConnectorFactory.getConnectorNoCheck(outputConnection.getClassName());
				if (outputConnector != null)
				{
					pipelineCheckMethods[j] = outputConnector.getFormCheckJavascriptMethodName(1+j);
					pipelineCheckForSaveMethods[j] = outputConnector.getFormPresaveCheckJavascriptMethodName(1+j);
				}
			}
		}
		else
		{
			ITransformationConnection transformationConnection = transformationMgr.load(pipelineConnectionNames[j]);
			if (transformationConnection != null)
			{
				ITransformationConnector transformationConnector = TransformationConnectorFactory.getConnectorNoCheck(transformationConnection.getClassName());
				if (transformationConnector != null)
				{
					pipelineCheckMethods[j] = transformationConnector.getFormCheckJavascriptMethodName(1+j);
					pipelineCheckForSaveMethods[j] = transformationConnector.getFormPresaveCheckJavascriptMethodName(1+j);
				}
			}
		}
	}

%>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<link rel="StyleSheet" href="style.css" type="text/css" media="screen"/>
	<title>
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ApacheManifoldCFEditJob")%>
	</title>

	<script type="text/javascript">
	<!--

	// Use this method to repost the form and pick a new tab
	function SelectTab(newtab)
	{
		if (checkForm())
		{
			document.editjob.tabname.value = newtab;
			document.editjob.sequencenumber.value = "";
			document.editjob.submit();
		}
	}

	// Use this method to repost the form and pick a new tab
	function SelectSequencedTab(newtab, sequencenumber)
	{
		if (checkForm())
		{
			document.editjob.tabname.value = newtab;
			document.editjob.sequencenumber.value = sequencenumber;
			document.editjob.submit();
		}
	}

	// Use this method to repost the form,
	// and set the anchor request.
	function postFormSetAnchor(anchorValue)
	{
		if (checkForm())
		{
			if (anchorValue != "")
				document.editjob.action = document.editjob.action + "#" + anchorValue;
			document.editjob.submit();
		}
	}

	// Use this method to repost the form
	function postFormNew()
	{
		if (checkForm())
		{
			document.editjob.submit();
		}
	}

	// Deprecated
	function postForm(schedCount)
	{
		if (checkForm())
		{
			document.editjob.submit();
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
			if (editjob.description.value == "")
			{
				alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.JobMustHaveAName")%>");
				SelectTab("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.Name")%>");
				document.editjob.description.focus();
				return;
			}
<%
	for (int j = 0; j < pipelineCheckForSaveMethods.length; j++)
	{
%>
			if (window.<%=pipelineCheckForSaveMethods[j]%>)
			{
				if (<%=pipelineCheckForSaveMethods[j]%>() == false)
					return;
			}
<%
	}
%>
			if (window.<%=saveCheckMethod%>)
			{
				if (<%=saveCheckMethod%>() == false)
					return;
			}
			document.editjob.op.value="Save";
			document.editjob.submit();
		}
	}

	function Cancel()
	{
		document.editjob.op.value="Cancel";
		document.editjob.submit();
	}

	function Continue()
	{
		document.editjob.op.value="Continue";
		postFormNew();
	}

	function InsertPipelineStageTransformation(n)
	{
		if (editjob.transformation_connectionname.value == "")
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.SelectATransformationStageConnectionName")%>");
			editjob.transformation_connectionname.focus();
			return;
		}
		eval("document.editjob.pipeline_"+n+"_op.value = 'InsertTransformation'");
		postFormSetAnchor("pipeline_"+(n+1)+"_tag");
	}

	function InsertPipelineStageOutput(n)
	{
		if (editjob.output_connectionname.value == "")
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.SelectAnOutputStageConnectionName")%>");
			editjob.output_connectionname.focus();
			return;
		}
		eval("document.editjob.pipeline_"+n+"_op.value = 'InsertOutput'");
		postFormSetAnchor("pipeline_"+(n+1)+"_tag");
	}

	function AppendPipelineStageOutput()
	{
		if (editjob.output_connectionname.value == "")
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.SelectAnOutputStageConnectionName")%>");
			editjob.output_connectionname.focus();
			return;
		}
		document.editjob.output_op.value="Add";
		postFormSetAnchor("output_tag");
	}
	
	function DeletePipelineStage(n)
	{
		eval("document.editjob.pipeline_"+n+"_op.value = 'Delete'");
		if (n == 0)
			postFormSetAnchor("pipeline_tag");
		else
			postFormSetAnchor("pipeline_"+(n-1)+"_tag");
	}
	
	function AddScheduledTime()
	{
		if (editjob.duration.value != "" && !isInteger(editjob.duration.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.DurationMustBeAValidInteger")%>");
			editjob.duration.focus();
			return;
		}
		document.editjob.recordop.value="Add Scheduled Time";
		postFormSetAnchor("add_schedule");
	}

	function RemoveSchedule(n)
	{
		eval("document.editjob.recordop"+n+".value = 'Remove Schedule'");
		if (n == 0)
			postFormSetAnchor("add_schedule");
		else
			postFormSetAnchor("remove_schedule_"+(n-1));
	}

	function checkForm()
	{
		if (!checkRecrawl())
			return false;
		if (!checkMaxRecrawl())
			return false;
		if (!checkRecrawlConsistent())
			return false;
		if (!checkReseed())
			return false;
		if (!checkExpiration())
			return false;
		if (!checkSchedule())
			return false;
<%
	for (int j = 0; j < pipelineCheckMethods.length; j++)
	{
%>
		if (window.<%=pipelineCheckMethods[j]%>)
		{
			if (<%=pipelineCheckMethods[j]%>() == false)
				return false;
		}
<%
	}
%>
		// Check the connector part
		if (window.<%=checkMethod%>)
		{
			if (<%=checkMethod%>() == false)
				return false;
		}
		return true;
	}

	function checkSchedule()
	{
		var i = 0;
		var schedCount = <%=Integer.toString(scheduleRecords.size())%>;
		while (i < schedCount)
		{
			var propertyname = "duration" + i;
			if (eval("editjob."+propertyname+".value") != "" && !isInteger(eval("editjob."+propertyname+".value")))
			{
				alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.DurationMustBeAValidInteger")%>");
				eval("editjob."+propertyname+".focus()");
				return false;
			}
			i = i+1;
		}
		return true;
	}

	function checkRecrawl()
	{
		if (editjob.recrawlinterval.value != "" && !isInteger(editjob.recrawlinterval.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.RecrawlIntervalMustBeAValidIntegerOrNull")%>");
			editjob.recrawlinterval.focus();
			return false;
		}
		return true;
	}

	function checkMaxRecrawl()
	{
		if (editjob.maxrecrawlinterval.value != "" && !isInteger(editjob.maxrecrawlinterval.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.MaxRecrawlIntervalMustBeAValidIntegerOrNull")%>");
			editjob.maxrecrawlinterval.focus();
			return false;
		}
		return true;
	}
	
	function checkRecrawlConsistent()
	{
		if (editjob.maxrecrawlinterval.value != "" && editjob.recrawlinterval.value != "" && parseInt(editjob.maxrecrawlinterval.value) < parseInt(editjob.recrawlinterval.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.MaxRecrawlIntervalMustBeLargerThanRecrawlInterval")%>");
			editjob.maxrecrawlinterval.focus();
			return false;
		}
		return true;
	}

	function checkReseed()
	{
		if (editjob.reseedinterval.value != "" && !isInteger(editjob.reseedinterval.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.ReseedIntervalMustBeAValidIntegerOrNull")%>");
			editjob.reseedinterval.focus();
			return false;
		}
		return true;
	}

	function checkExpiration()
	{
		if (editjob.expirationinterval.value != "" && !isInteger(editjob.expirationinterval.value))
		{
			alert("<%=Messages.getBodyJavascriptString(pageContext.getRequest().getLocale(),"editjob.ExpirationIntervalMustBeAValidIntegerOrNull")%>");
			editjob.expirationinterval.focus();
			return false;
		}
		return true;
	}

	function isInteger(value)
	{
		var anum=/(^\d+$)/;
		return anum.test(value);
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

	//-->
	</script>
<%
	if (connection != null)
	{
		IRepositoryConnector repositoryConnector = repositoryConnectorPool.grab(connection);
		if (repositoryConnector != null)
		{
			try
			{
				repositoryConnector.outputSpecificationHeader(new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),documentSpecification,0,tabsArray);
			}
			finally
			{
				repositoryConnectorPool.release(connection,repositoryConnector);
			}
		}
		Integer repositoryConnectionSequenceNumber = new Integer(0);
		while (sequenceArray.size() < tabsArray.size())
		{
			sequenceArray.add(repositoryConnectionSequenceNumber);
		}
	}
%>

<%
	for (int j = 0; j < pipelineConnectionNames.length; j++)
	{
		if (pipelineIsOutputs[j])
		{
			IOutputConnection outputConnection = outputMgr.load(pipelineConnectionNames[j]);
			if (outputConnection != null)
			{
				IOutputConnector outputConnector = outputConnectorPool.grab(outputConnection);
				if (outputConnector != null)
				{
					try
					{
						outputConnector.outputSpecificationHeader(new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),pipelineSpecifications[j],1+j,tabsArray);
					}
					finally
					{
						outputConnectorPool.release(outputConnection,outputConnector);
					}
				}
			}
		}
		else
		{
			ITransformationConnection transformationConnection = transformationMgr.load(pipelineConnectionNames[j]);
			if (transformationConnection != null)
			{
				ITransformationConnector transformationConnector = transformationConnectorPool.grab(transformationConnection);
				if (transformationConnector != null)
				{
					try
					{
						transformationConnector.outputSpecificationHeader(new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),pipelineSpecifications[j],1+j,tabsArray);
					}
					finally
					{
						transformationConnectorPool.release(transformationConnection,transformationConnector);
					}
				}
			}
		}
		Integer connectionSequenceNumber = new Integer(1+j);
		while (sequenceArray.size() < tabsArray.size())
		{
			sequenceArray.add(connectionSequenceNumber);
		}
	}
	
%>

</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="darkwindow">
<%
	if (connList.length == 0)
	{
%>
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.EditaJob")%></p>
	<table class="displaytable"><tr><td class="message"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NoRepositoryConnectionsDefinedCreateOneFirst")%></td></tr></table>
<%
	}
	else if (outputList.length == 0)
	{
%>
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.EditaJob")%></p>
	<table class="displaytable"><tr><td class="message"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NoOutputConnectionsDefinedCreateOneFirst")%></td></tr></table>
<%
	}
	else
	{
%>
	<form class="standardform" name="editjob" action="execute.jsp" method="POST" enctype="multipart/form-data">
	  <input type="hidden" name="op" value="Continue"/>
	  <input type="hidden" name="type" value="job"/>
	  <input type="hidden" name="index" value=""/>
	  <input type="hidden" name="tabname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(tabName)%>'/>
	  <input type="hidden" name="sequencenumber" value='<%=((tabSequenceInt==-1)?"":Integer.toString(tabSequenceInt))%>'/>
<%
	if (jobID != null)
	{
%>
	  <input type="hidden" name="jobid" value='<%=jobID%>'/>
<%
	}
%>
	    <table class="tabtable">
	      <tr class="tabspacerrow">
		<td class="spacertab" colspan="<%=tabsArray.size()%>"></td>
		<td class="remaindertab" rowspan="3">
<%
	if (description.length() > 0)
	{
%>
			  <nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.EditJob")%> '<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(description)%>'</nobr>
<%
	}
	else
	{
%>
		          <nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.EditaJob")%></nobr>
<%
	}
%>
		</td>
	      </tr>
	      <tr class="tabsequencerow">
<%
	Integer currentSequenceNumber = null;
	int startColumn = 0;
	for (int tabNum = 0; tabNum < tabsArray.size(); tabNum++)
	{
		boolean doswitch = false;
		Integer sequenceNumber = sequenceArray.get(tabNum);
		if (sequenceNumber == null || currentSequenceNumber == null)
			doswitch = (sequenceNumber != null || currentSequenceNumber != null);
		else
			doswitch = !sequenceNumber.equals(currentSequenceNumber);
		if (doswitch)
		{
			int colspan = tabNum - startColumn;
			if (colspan > 0)
			{
				if (currentSequenceNumber == null)
				{
%>
		      <td class="blanksequencetab" colspan="<%=colspan%>"></td>
<%
				}
				else
				{
%>
		      <td class="sequencetab" colspan="<%=colspan%>"><%=(currentSequenceNumber.intValue()+1)%>.</td>
<%
				}
			}
			startColumn = tabNum;
			currentSequenceNumber = sequenceNumber;
		}
	}
	if (startColumn != tabsArray.size())
	{
		int colspan = tabsArray.size() - startColumn;
		if (currentSequenceNumber == null)
		{
%>
		      <td class="blanksequencetab" colspan="<%=colspan%>"></td>
<%
		}
		else
		{
%>
		      <td class="sequencetab" colspan="<%=colspan%>"><%=(currentSequenceNumber.intValue()+1)%>.</td>
<%
		}
	}
	// Final (remainder) cell left out on purpose; filled in above.
%>
	      </tr>
	      <tr class="tabrow">
<%
	for (int tabNum = 0; tabNum < tabsArray.size(); tabNum++)
	{
		String tab = tabsArray.get(tabNum);
		Integer sequenceNumber = sequenceArray.get(tabNum);
		int sequenceNumberInt = (sequenceNumber == null)?-1:sequenceNumber.intValue();
		if (tab.equals(tabName) && (tabSequenceInt == -1 || sequenceNumberInt == tabSequenceInt))
		{
%>
		      <td class="activetab"><nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(tab)%></nobr></td>
<%
		}
		else
		{
%>
		      <td class="passivetab"><nobr><a href="javascript:void(0);" alt='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(tab)+" "+Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.tab")%>' onclick='<%="javascript:SelectSequencedTab(\""+tab+"\",\""+((sequenceNumber==null)?"":sequenceNumber.toString())+"\");return false;"%>'><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(tab)%></a></nobr></td>
<%
		}
	}
	// Missing remainder tab ON PURPOSE -- comes from rowspan=2 tab above
%>
	      </tr>
	      <tr class="tabbodyrow">
		<td class="tabbody" colspan='<%=Integer.toString(tabsArray.size()+1)%>'>

		  <input type="hidden" name="schedulerecords" value='<%=Integer.toString(scheduleRecords.size())%>'/>
<%
	// The NAME tab
	if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Name")) && tabSequenceInt == -1)
	{
%>
		  <table class="displaytable">
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NameColon")%></nobr></td><td class="value" colspan="3">
					<input type="text" size="50" name="description" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(description)%>'/>
				</td>
			</tr>
		  </table>
<%
	}
	else
	{
%>
		  <input type="hidden" name="description" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(description)%>'/>
<%
	}

	// Hop Filters tab
	if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editjob.HopFilters")) && tabSequenceInt == -1)
	{
	    if (relationshipTypes != null)
	    {
%>
		  <table class="displaytable">
			<tr>
				<td class="separator" colspan="4"><input type="hidden" name="hopfilters" value="true"/><hr/></td>
			</tr>
<%
		int i = 0;
		while (i < relationshipTypes.length)
		{
			String relationshipType = relationshipTypes[i++];
			String mapField = "";
			Long mapValue = (Long)hopFilterMap.get(relationshipType);
			if (mapValue != null)
				mapField = mapValue.toString();
%>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.MaximumHopCountForType")%> '<%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(relationshipType)%>'<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.colon")%></nobr></td>
				<td class="value" colspan="3" >
					<input name='<%="hopmax_"+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(relationshipType)%>' type="text" size="5" value='<%=mapField%>'/>
				</td>
			</tr>
<%
		}
%>
			<tr><td class="separator" colspan="4"><hr/></td></tr>
			<tr>
				<td class="description" colspan="1"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.HopCountModeColon")%></nobr></td>
				<td class="value" colspan="3">
					<nobr><input type="radio" name="hopcountmode" value='<%=Integer.toString(IJobDescription.HOPCOUNT_ACCURATE)%>' <%=((hopcountMode==IJobDescription.HOPCOUNT_ACCURATE)?"checked=\"true\"":"")%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.DeleteUnreachableDocuments")%></input></nobr><br/>
					<nobr><input type="radio" name="hopcountmode" value='<%=Integer.toString(IJobDescription.HOPCOUNT_NODELETE)%>' <%=((hopcountMode==IJobDescription.HOPCOUNT_NODELETE)?"checked=\"true\"":"")%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.KeepUnreachableDocumentsForNow")%></input></nobr><br/>
					<nobr><input type="radio" name="hopcountmode" value='<%=Integer.toString(IJobDescription.HOPCOUNT_NEVERDELETE)%>' <%=((hopcountMode==IJobDescription.HOPCOUNT_NEVERDELETE)?"checked=\"true\"":"")%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.KeepUnreachableDocumentsForever")%></input></nobr><br/>
				</td>
			</tr>
		  </table>
<%
	    }
	}
	else
	{
	    if (relationshipTypes != null)
	    {
%>
	<input type="hidden" name="hopfilters" value="true"/>
<%
		int i = 0;
		while (i < relationshipTypes.length)
		{
			String relationshipType = relationshipTypes[i++];
			String mapField = "";
			Long mapValue = (Long)hopFilterMap.get(relationshipType);
			if (mapValue != null)
				mapField = mapValue.toString();
%>
	<input name='<%="hopmax_"+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(relationshipType)%>' type="hidden" value='<%=mapField%>'/>
	<input name="hopcountmode" type="hidden" value='<%=Integer.toString(hopcountMode)%>'/>
<%
		}
	    }
	}

	// Connection tab
	if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Connection")) && tabSequenceInt == -1)
	{
		int rowCounter = 0;

%>
		  <table class="displaytable">
			<tr><td class="separator" colspan="4"><hr/></td></tr>
			<tr>
				<td colspan="1" class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.PipelineColon")%></nobr></td>
				<td class="boxcell" colspan="3">
					<table class="formtable">
						<tr class="formheaderrow">
							<td class="formcolumnheader">
								<input name="pipeline_count" type="hidden" value="<%=pipelineConnectionNames.length%>"/>
							</td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StageNumber")%></nobr></td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StageType")%></nobr></td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StagePrecedent")%></nobr></td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StageDescription")%></nobr></td>
							<td class="formcolumnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StageConnectionName")%></nobr></td>
						</tr>
						<tr class="<%=((rowCounter++ % 2)==0)?"evenformrow":"oddformrow"%>">
							<td class="formcolumncell"></td>
							<td class="formcolumncell">1.</td>
							<td class="formcolumncell"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Repository")%></td>
							<td class="formcolumncell"></td>
							<td class="formcolumncell"></td>
							<td class="formcolumncell">
<%
		if (connectionName.length() == 0)
		{
%>
								<select name="connectionname" size="1">
									<option <%="".equals(connectionName)?"selected=\"selected\"":""%> value="">-- <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NoneSelected")%> --</option>
<%
			for (IRepositoryConnection conn : connList)
			{
%>
									<option <%=conn.getName().equals(connectionName)?"selected=\"selected\"":""%> value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(conn.getName())%>'><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(conn.getName())%></option>
<%
			}
%>
								</select>
<%
		}
		else
		{
%>
								<input type="hidden" name="connectionname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionName)%>'/><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(connectionName)%>
<%
		}
%>
							</td>
						</tr>
<%
		// A map of stage number to reference count
		Map<Integer,Integer> referenceCounts = new HashMap<Integer,Integer>();
		// A list of precedents to pick from, displayed at the end
		List<Integer> precedents = new ArrayList<Integer>();
		// Repository connection is always allowed
		precedents.add(new Integer(-1));
		Set<String> alreadyPresent = new HashSet<String>();
		for (int j = 0; j < pipelineConnectionNames.length; j++)
		{
			if (pipelineIsOutputs[j])
				alreadyPresent.add(pipelineConnectionNames[j]);
			else
				precedents.add(new Integer(j));
			if (pipelinePrerequisites[j] != -1)
			{
				Integer thisOne = new Integer(pipelinePrerequisites[j]);
				Integer x = referenceCounts.get(thisOne);
				if (x == null)
					referenceCounts.put(thisOne,new Integer(1));
				else
					referenceCounts.put(thisOne,new Integer(x.intValue() + 1));
			}
		}
		boolean anyTransformationButtons = false;
		for (int j = 0; j < pipelineConnectionNames.length; j++)
		{
			String pipelineConnectionName = pipelineConnectionNames[j];
			String pipelineDescription = pipelineDescriptions[j];
			if (pipelineDescription == null)
				pipelineDescription = "";
			String pipelineType = pipelineIsOutputs[j]?Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Output"):Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Transformation");
%>
						<tr class="<%=((rowCounter++ % 2)==0)?"evenformrow":"oddformrow"%>">
							<td class="formcolumncell">
								<input name="pipeline_<%=j%>_op" type="hidden" value="Continue"/>
								<a name="pipeline_<%=j%>_tag"/>
<%
			// We don't want to leave orphans around.  If the pipeline stage is an output, we can delete it ONLY if:
			// -- the precedent is -1, OR
			// -- the precedent is not -1 BUT more than one stage refers to the precedent
			if (!pipelineIsOutputs[j] || pipelinePrerequisites[j] == -1 || referenceCounts.get(new Integer(pipelinePrerequisites[j])).intValue() > 1)
			{
%>
								<input type="button" value="<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Delete")%>" alt='<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Deletepipelinestage")%>' onclick="javascript:DeletePipelineStage(<%=j%>);"/>
<%
			}
			if (transformationList.length > 0)
			{
				anyTransformationButtons = true;
%>
								<input type="button" value="<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.InsertTransformationBefore")%>" alt='<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Insertnewtransformationhere")%>' onclick="javascript:InsertPipelineStageTransformation(<%=j%>);"/>
<%
			}
			if (outputList.length != alreadyPresent.size())
			{
%>
								<input type="button" value="<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.InsertOutputBefore")%>" alt='<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Insertnewoutputhere")%>' onclick="javascript:InsertPipelineStageOutput(<%=j%>);"/>
<%
			}
%>
							</td>
							<td class="formcolumncell"><%=(j+2)%>.</td>
							<td class="formcolumncell"><%=pipelineType%>
								<input name="pipeline_<%=j%>_isoutput" type="hidden" value='<%=pipelineIsOutputs[j]?"true":"false"%>'/>
							</td>
							<td class="formcolumncell"><%=(pipelinePrerequisites[j] + 2)%>.
								<input name="pipeline_<%=j%>_precedent" type="hidden" value="<%=pipelinePrerequisites[j]%>"/>
							</td>
							<td class="formcolumncell">
								<input name="pipeline_<%=j%>_description" type="text" size="30" value="<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pipelineDescription)%>"/>
							</td>
							<td class="formcolumncell">
								<nobr><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(pipelineConnectionName)%></nobr>
								<input name="pipeline_<%=j%>_connectionname" type="hidden" value="<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pipelineConnectionName)%>"/>
							</td>
						</tr>
<%
		}
		if (anyTransformationButtons)
		{
%>
						<tr class="formrow"><td class="formseparator" colspan="4"><hr/></td></tr>
						<tr class="formrow">
							<td class="formcolumncell">
								<a name="transformation_tag"/>
							</td>
							<td class="formcolumncell"></td>
							<td class="formcolumncell"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Transformation")%></td>
							<td class="formcolumncell"></td>
							<td class="formcolumncell">
								<input name="transformation_description" type="text" size="30" value=""/>
							</td>
							<td class="formcolumncell">
								<select name="transformation_connectionname" size="1">
									<option selected="selected" value="">-- <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NoneSelected")%> --</option>
<%
			for (ITransformationConnection conn : transformationList)
			{
%>
									<option value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(conn.getName())%>'><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(conn.getName())%></option>
<%
			}
%>
								</select>
							</td>
						</tr>
<%
		}
		if (outputList.length != alreadyPresent.size())
		{
%>
						<tr class="formrow"><td class="formseparator" colspan="4"><hr/></td></tr>
						<tr class="formrow">
							<td class="formcolumncell">
								<a name="output_tag"/>
								<input type="button" value='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.AddOutput")%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.AddAnOutput")%>' onclick="javascript:AppendPipelineStageOutput();"/>
								<input name="output_op" type="hidden" value="Continue"/>
							</td>
							<td class="formcolumncell"></td>
							<td class="formcolumncell"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Output")%></td>
							<td class="formcolumncell">
								<select name="output_precedent" size="1">
<%
			for (Integer pre : precedents)
			{
%>
									<option value="<%=pre%>"><%=(pre.intValue()+2)%></option>
<%
			}
%>
								</select>
							</td>
							<td class="formcolumncell">
								<input name="output_description" type="text" size="30" value=""/>
							</td>
							<td class="formcolumncell">
								<select name="output_connectionname" size="1">
									<option selected="selected" value="">-- <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NoneSelected")%> --</option>
<%
			for (IOutputConnection conn : outputList)
			{
				if (!alreadyPresent.contains(conn.getName()))
				{
%>
									<option value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(conn.getName())%>'><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(conn.getName())%></option>
<%
				}
			}
%>
								</select>
							</td>
						</tr>
<%
		}
%>
					</table>
				</td>
			</tr>
			
			<tr><td class="separator" colspan="4"><hr/></td></tr>
			
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.PriorityColon")%></nobr></td>
				<td class="value">
					<select name="priority" size="1">
						<option value="1" <%=(priority==1)?"selected=\"selected\"":""%>>1 <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Highest")%></option>
						<option value="2" <%=(priority==2)?"selected=\"selected\"":""%>>2</option>
						<option value="3" <%=(priority==3)?"selected=\"selected\"":""%>>3</option>
						<option value="4" <%=(priority==4)?"selected=\"selected\"":""%>>4</option>
						<option value="5" <%=(priority==5)?"selected=\"selected\"":""%>>5</option>
						<option value="6" <%=(priority==6)?"selected=\"selected\"":""%>>6</option>
						<option value="7" <%=(priority==7)?"selected=\"selected\"":""%>>7</option>
						<option value="8" <%=(priority==8)?"selected=\"selected\"":""%>>8</option>
						<option value="9" <%=(priority==9)?"selected=\"selected\"":""%>>9</option>
						<option value="10" <%=(priority==10)?"selected=\"selected\"":""%>>10 <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Lowest")%></option>
					</select>
				</td>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StartMethodColon")%></nobr></td>
				<td class="value">
					<select name="startmethod" size="1">
						<option value='<%=IJobDescription.START_WINDOWBEGIN%>' <%=(startMethod==IJobDescription.START_WINDOWBEGIN)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StartWhenScheduleWindowStarts")%></option>
						<option value='<%=IJobDescription.START_WINDOWINSIDE%>' <%=(startMethod==IJobDescription.START_WINDOWINSIDE)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.StartEvenInsideAScheduleWindow")%></option>
						<option value='<%=IJobDescription.START_DISABLE%>' <%=(startMethod==IJobDescription.START_DISABLE)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.DontAutomaticallyStartThisJob")%></option>
					</select>
				</td>
			</tr>
		  </table>
<%
	}
	else
	{
%>
		  <input type="hidden" name="connectionname" value='<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(connectionName)%>'/>
		  <input type="hidden" name="pipeline_count" value="<%=pipelineConnectionNames.length%>"/>
<%
		for (int j = 0; j < pipelineConnectionNames.length; j++)
		{
			String pipelineConnectionName = pipelineConnectionNames[j];
			String pipelineDescription = pipelineDescriptions[j];
			if (pipelineDescription == null)
				pipelineDescription = "";
%>
		  <input name="pipeline_<%=j%>_isoutput" type="hidden" value='<%=pipelineIsOutputs[j]?"true":"false"%>'/>
		  <input name="pipeline_<%=j%>_precedent" type="hidden" value="<%=pipelinePrerequisites[j]%>"/>
		  <input type="hidden" name="pipeline_<%=j%>_connectionname" value="<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pipelineConnectionName)%>"/>
		  <input type="hidden" name="pipeline_<%=j%>_description" value="<%=org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pipelineDescription)%>"/>
<%
		}
%>
		  <input type="hidden" name="priority" value='<%=priority%>'/>
		  <input type="hidden" name="startmethod" value='<%=startMethod%>'/>
<%
	}

	// Scheduling tab
	if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Scheduling")) && tabSequenceInt == -1)
	{
%>
		  <table class="displaytable">
<%
	    if (model != -1 && model != IRepositoryConnector.MODEL_ADD_CHANGE_DELETE && model != IRepositoryConnector.MODEL_CHAINED_ADD_CHANGE_DELETE)
	    {
%>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description">
					<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ScheduleTypeColon")%></nobr>
				</td>
				<td class="value" colspan="3">
					<select name="scheduletype" size="1">
						<option value='<%=IJobDescription.TYPE_CONTINUOUS%>' <%=(type==IJobDescription.TYPE_CONTINUOUS)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.RescanDocumentsDynamically")%></option>
						<option value='<%=IJobDescription.TYPE_SPECIFIED%>' <%=(type==IJobDescription.TYPE_SPECIFIED)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ScanEveryDocumentOnce")%></option>
					</select>
				</td>
			</tr>
			<tr>
				<td class="description">
					<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.RecrawlIntervalIfContinuousColon")%></nobr>
				</td>
				<td class="value" colspan="3">
					<nobr><input type="text" size="5" name="recrawlinterval" value='<%=((recrawlInterval==null)?"":recrawlInterval.toString())%>'/> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutesBlankInfinity")%></nobr>
				</td>
			</tr>
			<tr>
				<td class="description">
					<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.MaxRecrawlIntervalIfContinuousColon")%></nobr>
				</td>
				<td class="value" colspan="3">
					<nobr><input type="text" size="5" name="maxrecrawlinterval" value='<%=((maxRecrawlInterval==null)?"":maxRecrawlInterval.toString())%>'/> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutesBlankInfinity")%></nobr>
				</td>
			</tr>
			<tr>
				<td class="description">
					<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ExpirationIntervalIfContinuousColon")%></nobr>
				</td>
				<td class="value" colspan="3">
					<nobr><input type="text" size="5" name="expirationinterval" value='<%=((expirationInterval==null)?"":expirationInterval.toString())%>'/> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutesBlankInfinity")%></nobr>
				</td>
			</tr>
			<tr>
				<td class="description">
					<nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ReseedIntervalIfContinuousColon")%></nobr>
				</td>
				<td class="value" colspan="3">
					<nobr><input type="text" size="5" name="reseedinterval" value='<%=((reseedInterval==null)?"":reseedInterval.toString())%>'/> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutesBlankInfinity")%></nobr>
				</td>
			</tr>
<%
	    }
	    else
	    {
%>
			<input type="hidden" name="scheduletype" value='<%=type%>'/>
			<input type="hidden" name="recrawlinterval" value='<%=((recrawlInterval==null)?"":recrawlInterval.toString())%>'/>
			<input type="hidden" name="maxrecrawlinterval" value='<%=((maxRecrawlInterval==null)?"":maxRecrawlInterval.toString())%>'/>
			<input type="hidden" name="reseedinterval" value='<%=((reseedInterval==null)?"":reseedInterval.toString())%>'/>
			<input type="hidden" name="expirationinterval" value='<%=((expirationInterval==null)?"":expirationInterval.toString())%>'/>
<%
	    }
%>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
<%
	    if (scheduleRecords.size() == 0)
	    {
%>
			<tr><td class="message" colspan="4"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.NoScheduleSpecified")%></td></tr>
<%
	    }
	    else
	    {
	      int l = 0;
	      while (l < scheduleRecords.size())
	      {
		ScheduleRecord sr = (ScheduleRecord)scheduleRecords.get(l);
		Long srDuration = sr.getDuration();
		EnumeratedValues srDayOfWeek = sr.getDayOfWeek();
		EnumeratedValues srMonthOfYear = sr.getMonthOfYear();
		EnumeratedValues srDayOfMonth = sr.getDayOfMonth();
		EnumeratedValues srYear = sr.getYear();
		EnumeratedValues srHourOfDay = sr.getHourOfDay();
		EnumeratedValues srMinutesOfHour = sr.getMinutesOfHour();
		boolean srRequestMinimum = sr.getRequestMinimum();
		String postFix = Integer.toString(l);
		int k;

		if (l > 0)
		{
%>
			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
<%
		}
%>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ScheduledTimeColon")%></nobr></td>
				<td colspan="3" class="value">
				    <select class="schedulepulldown" multiple="true" name='<%="dayofweek"+postFix%>' size="3">
					<option value="none" <%=(srDayOfWeek==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.AnyDayOfWeek")%></option>
					<option value="0" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(0))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Sundays")%></option>
					<option value="1" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(1))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Mondays")%></option>
					<option value="2" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(2))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Tuesdays")%></option>
					<option value="3" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(3))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Wednesdays")%></option>
					<option value="4" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(4))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Thursdays")%></option>
					<option value="5" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(5))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Fridays")%></option>
					<option value="6" <%=(srDayOfWeek!=null&&srDayOfWeek.checkValue(6))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Saturdays")%></option>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.at")%> 
				    <select class="schedulepulldown" multiple="true" name='<%="hourofday"+postFix%>' size="3">
					<option value="none" <%=(srHourOfDay==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.MidnightAnyHourOfDay")%></option>
<%
					k = 0;
					while (k < 24)
					{
						int q = k;
						String ampm;
						if (k < 12)
							ampm = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.am");
						else
						{
							ampm = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.pm");
							q -= 12;
						}
						String hour;
						if (q == 0)
							q = 12;
%>
						<option value='<%=k%>' <%=(srHourOfDay!=null&&srHourOfDay.checkValue(k))?"selected=\"selected\"":""%>><%=Integer.toString(q)+" "+ampm%></option>
<%						
						k++;
					}
%>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.plus")%> 
				    <select class="schedulepulldown" multiple="true" name='<%="minutesofhour"+postFix%>' size="3">
					<option value="none" <%=(srMinutesOfHour==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Nothing")%></option>
<%
					k = 0;
					while (k < 60)
					{
%>
						<option value='<%=k%>' <%=(srMinutesOfHour!=null&&srMinutesOfHour.checkValue(k))?"selected=\"selected\"":""%>><%=Integer.toString(k)%> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutes")%></option>
<%
						k++;
					}
%>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.in")%> 
				    <select class="schedulepulldown" multiple="true" name='<%="monthofyear"+postFix%>' size="3">
					<option value="none" <%=(srMonthOfYear==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.EveryMonthOfYear")%></option>
					<option value="0" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(0))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.January")%></option>
					<option value="1" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(1))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.February")%></option>
					<option value="2" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(2))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.March")%></option>
					<option value="3" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(3))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.April")%></option>
					<option value="4" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(4))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.May")%></option>
					<option value="5" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(5))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.June")%></option>
					<option value="6" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(6))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.July")%></option>
					<option value="7" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(7))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.August")%></option>
					<option value="8" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(8))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.September")%></option>
					<option value="9" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(9))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.October")%></option>
					<option value="10" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(10))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.November")%></option>
					<option value="11" <%=(srMonthOfYear!=null&&srMonthOfYear.checkValue(11))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.December")%></option>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.on")%>
				    <select class="schedulepulldown" multiple="true" name='<%="dayofmonth"+postFix%>' size="3">
					<option value="none" <%=(srDayOfMonth==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.AnyDayOfMonth")%></option>
<%
					k = 0;
					while (k < 31)
					{
						int value = (k+1) % 10;
						String suffix;
						if (value == 1 && k != 10)
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.st");
						else if (value == 2 && k != 11)
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.nd");
						else if (value == 3 && k != 12)
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.rd");
						else
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.th");
%>
						<option value='<%=Integer.toString(k)%>' <%=(srDayOfMonth!=null&&srDayOfMonth.checkValue(k))?"selected=\"selected\"":""%>><%=Integer.toString(k+1)+suffix+" "+Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.dayofmonth")%></option>
<%
						k++;
					}
%>
				    </select><input type="hidden" name='<%="year"+postFix%>' value="none"/>
				</td>
			</tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.MaximumRunTimeColon")%></nobr></td>
				<td class="value">
					<input type="text" size="5" name='<%="duration"+postFix%>' value='<%=((srDuration==null)?"":new Long(srDuration.longValue()/60000L).toString())%>'/> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutes")%>
				</td>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.JobInvocationColon")%></nobr></td>
				<td class="value">
					<select class="schedulepulldown" multiple="false" name='<%="invocation"+postFix%>' size="2">
						<option value="complete" <%=(srRequestMinimum==false)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Complete")%></option>
						<option value="minimal" <%=srRequestMinimum?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Minimal")%></option>
					</select>
				</td>
			</tr>
			<tr>
				<td class="message" colspan="4">
					<a name='<%="remove_schedule_"+Integer.toString(l)%>'><input type="button" value="<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.RemoveSchedule")%>" onClick='<%="Javascript:RemoveSchedule("+Integer.toString(l)+")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.RemoveScheduleRecord")+Integer.toString(l)%>'/></a>
					<input type="hidden" name='<%="recordop"+postFix%>' value=""/>
				</td>
			</tr>
<%
		l++;
	      }
	    }
%>

			<tr>
				<td class="separator" colspan="4"><hr/></td>
			</tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.ScheduledTimeColon")%></nobr></td>
				<td colspan="3" class="value">
				    <select class="schedulepulldown" multiple="true" name="dayofweek" size="3">
					<option value="none" <%=(dayOfWeek==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.AnyDayOfWeek")%></option>
					<option value="0" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(0))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Sundays")%></option>
					<option value="1" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(1))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Mondays")%></option>
					<option value="2" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(2))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Tuesdays")%></option>
					<option value="3" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(3))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Wednesdays")%></option>
					<option value="4" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(4))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Thursdays")%></option>
					<option value="5" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(5))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Fridays")%></option>
					<option value="6" <%=(dayOfWeek!=null&&dayOfWeek.checkValue(6))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Saturdays")%></option>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.at")%> 
				    <select class="schedulepulldown" multiple="true" name="hourofday" size="3">
					<option value="none" <%=(hourOfDay==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.MidnightAnyHourOfDay")%></option>
<%
					int k = 0;
					while (k < 24)
					{
						int q = k;
						String ampm;
						if (k < 12)
							ampm = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.am");
						else
						{
							ampm = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.pm");
							q -= 12;
						}
						String hour;
						if (q == 0)
							q = 12;
%>
						<option value='<%=k%>' <%=(hourOfDay!=null&&hourOfDay.checkValue(k))?"selected=\"selected\"":""%>><%=Integer.toString(q)+" "+ampm%></option>
<%						
						k++;
					}
%>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.plus")%> 
				    <select class="schedulepulldown" multiple="true" name="minutesofhour" size="3">
					<option value="none" <%=(minutesOfHour==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Nothing")%></option>
<%
					k = 0;
					while (k < 60)
					{
%>
						<option value='<%=k%>' <%=(minutesOfHour!=null&&minutesOfHour.checkValue(k))?"selected=\"selected\"":""%>><%=Integer.toString(k)%> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutes")%></option>
<%
						k++;
					}
%>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.in")%> 
				    <select class="schedulepulldown" multiple="true" name="monthofyear" size="3">
					<option value="none" <%=(monthOfYear==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.EveryMonthOfYear")%></option>
					<option value="0" <%=(monthOfYear!=null&&monthOfYear.checkValue(0))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.January")%></option>
					<option value="1" <%=(monthOfYear!=null&&monthOfYear.checkValue(1))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.February")%></option>
					<option value="2" <%=(monthOfYear!=null&&monthOfYear.checkValue(2))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.March")%></option>
					<option value="3" <%=(monthOfYear!=null&&monthOfYear.checkValue(3))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.April")%></option>
					<option value="4" <%=(monthOfYear!=null&&monthOfYear.checkValue(4))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.May")%></option>
					<option value="5" <%=(monthOfYear!=null&&monthOfYear.checkValue(5))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.June")%></option>
					<option value="6" <%=(monthOfYear!=null&&monthOfYear.checkValue(6))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.July")%></option>
					<option value="7" <%=(monthOfYear!=null&&monthOfYear.checkValue(7))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.August")%></option>
					<option value="8" <%=(monthOfYear!=null&&monthOfYear.checkValue(8))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.September")%></option>
					<option value="9" <%=(monthOfYear!=null&&monthOfYear.checkValue(9))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.October")%></option>
					<option value="10" <%=(monthOfYear!=null&&monthOfYear.checkValue(10))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.November")%></option>
					<option value="11" <%=(monthOfYear!=null&&monthOfYear.checkValue(11))?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.December")%></option>
				    </select> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.on")%> 
				    <select class="schedulepulldown" multiple="true" name="dayofmonth" size="3">
					<option value="none" <%=(dayOfMonth==null)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.AnyDayOfMonth")%></option>
<%
					k = 0;
					while (k < 31)
					{
						int value = (k+1) % 10;
						String suffix;
						if (value == 1 && k != 10)
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.st");
						else if (value == 2 && k != 11)
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.nd");
						else if (value == 3 && k != 12)
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.rd");
						else
							suffix = Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.th");
%>
						<option value='<%=Integer.toString(k)%>' <%=(dayOfMonth!=null&&dayOfMonth.checkValue(k))?"selected=\"selected\"":""%>><%=Integer.toString(k+1)+suffix+" "+Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.dayofmonth")%></option>
<%
						k++;
					}
%>
				    </select><input type="hidden" name="year" value="none"/>
				</td>
			</tr>
			<tr>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.MaximumRunTimeColon")%></nobr></td>
				<td class="value">
					<input type="text" size="5" name="duration" value='<%=((duration==null)?"":duration.toString())%>'/> <%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.minutes")%>
				</td>
				<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.JobInvocationColon")%></nobr></td>
				<td class="value">
					<select class="schedulepulldown" multiple="false" name="invocation" size="2">
						<option value="complete" <%=(requestMinimum==false)?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Complete")%></option>
						<option value="minimal" <%=requestMinimum?"selected=\"selected\"":""%>><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"editjob.Minimal")%></option>
					</select>
				</td>
			</tr>
			<tr>
				<td class="message" colspan="4">
					<input type="hidden" name="recordop" value=""/>
					<a name="add_schedule"><input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.AddScheduledTime")%>" onClick="javascript:AddScheduledTime()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.AddNewScheduleRecord")%>"/></a>
				</td>
			</tr>
		  </table>
<%
	}
	else
	{
%>
		  <input type="hidden" name="scheduletype" value='<%=type%>'/>
		  <input type="hidden" name="recrawlinterval" value='<%=((recrawlInterval==null)?"":recrawlInterval.toString())%>'/>
		  <input type="hidden" name="maxrecrawlinterval" value='<%=((maxRecrawlInterval==null)?"":maxRecrawlInterval.toString())%>'/>
		  <input type="hidden" name="reseedinterval" value='<%=((reseedInterval==null)?"":reseedInterval.toString())%>'/>
		  <input type="hidden" name="expirationinterval" value='<%=((expirationInterval==null)?"":expirationInterval.toString())%>'/>
<%

	      int l = 0;
	      while (l < scheduleRecords.size())
	      {
		ScheduleRecord sr = (ScheduleRecord)scheduleRecords.get(l);
		Long srDuration = sr.getDuration();
		EnumeratedValues srDayOfWeek = sr.getDayOfWeek();
		EnumeratedValues srMonthOfYear = sr.getMonthOfYear();
		EnumeratedValues srDayOfMonth = sr.getDayOfMonth();
		EnumeratedValues srYear = sr.getYear();
		EnumeratedValues srHourOfDay = sr.getHourOfDay();
		EnumeratedValues srMinutesOfHour = sr.getMinutesOfHour();
		boolean srRequestMinimum = sr.getRequestMinimum();
		String postFix = Integer.toString(l);

		if (srDayOfWeek == null)
		{
%>
		  <input type="hidden" name='<%="dayofweek"+postFix%>' value="none"/>
<%
		}
		else
		{
			Iterator iter = srDayOfWeek.getValues();
			while (iter.hasNext())
			{
				Integer value = (Integer)iter.next();
%>
		  <input type="hidden" name='<%="dayofweek"+postFix%>' value='<%=value%>'/>
<%
			}
		}

		if (srHourOfDay == null)
		{
%>
		  <input type="hidden" name='<%="hourofday"+postFix%>' value="none"/>
<%
		}
		else
		{
			Iterator iter = srHourOfDay.getValues();
			while (iter.hasNext())
			{
				Integer value = (Integer)iter.next();
%>
		  <input type="hidden" name='<%="hourofday"+postFix%>' value='<%=value%>'/>
<%
			}
		}

		if (srMinutesOfHour == null)
		{
%>
		  <input type="hidden" name='<%="minutesofhour"+postFix%>' value="none"/>
<%
		}
		else
		{
			Iterator iter = srMinutesOfHour.getValues();
			while (iter.hasNext())
			{
				Integer value = (Integer)iter.next();
%>
		  <input type="hidden" name='<%="minutesofhour"+postFix%>' value='<%=value%>'/>
<%
			}
		}

		if (srDayOfMonth == null)
		{
%>
		  <input type="hidden" name='<%="dayofmonth"+postFix%>' value="none"/>
<%
		}
		else
		{
			Iterator iter = srDayOfMonth.getValues();
			while (iter.hasNext())
			{
				Integer value = (Integer)iter.next();
%>
		  <input type="hidden" name='<%="dayofmonth"+postFix%>' value='<%=value%>'/>
<%
			}
		}

		if (srMonthOfYear == null)
		{
%>
		  <input type="hidden" name='<%="monthofyear"+postFix%>' value="none"/>
<%
		}
		else
		{
			Iterator iter = srMonthOfYear.getValues();
			while (iter.hasNext())
			{
				Integer value = (Integer)iter.next();
%>
		  <input type="hidden" name='<%="monthofyear"+postFix%>' value='<%=value%>'/>
<%
			}
		}
%>
		  <input type="hidden" name='<%="duration"+postFix%>' value='<%=((srDuration==null)?"":new Long(srDuration.longValue()/60000L).toString())%>'/>
		  <input type="hidden" name='<%="invocation"+postFix%>' value='<%=srRequestMinimum?"minimal":"complete"%>'/>
		  <input type="hidden" name='<%="year"+postFix%>' value="none"/>
<%
		l++;
	      }
	}

	if (connection != null)
	{
		IRepositoryConnector repositoryConnector = repositoryConnectorPool.grab(connection);
		if (repositoryConnector != null)
		{
			try
			{
				repositoryConnector.outputSpecificationBody(new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),documentSpecification,0,tabSequenceInt,tabName);
			}
			finally
			{
				repositoryConnectorPool.release(connection,repositoryConnector);
			}
%>
		  <input type="hidden" name="connectionpresent" value="true"/>
<%
		}
	}
	
	boolean outputPresent = false;
	for (int j = 0; j < pipelineConnectionNames.length; j++)
	{
		if (pipelineIsOutputs[j])
		{
			outputPresent = true;
			IOutputConnection outputConnection = outputMgr.load(pipelineConnectionNames[j]);
			if (outputConnection != null)
			{
				IOutputConnector outputConnector = outputConnectorPool.grab(outputConnection);
				if (outputConnector != null)
				{
					try
					{
						outputConnector.outputSpecificationBody(new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),pipelineSpecifications[j],1+j,tabSequenceInt,tabName);
					}
					finally
					{
						outputConnectorPool.release(outputConnection,outputConnector);
					}
				}
			}
		}
		else
		{
			ITransformationConnection transformationConnection = transformationMgr.load(pipelineConnectionNames[j]);
			if (transformationConnection != null)
			{
				ITransformationConnector transformationConnector = transformationConnectorPool.grab(transformationConnection);
				if (transformationConnector != null)
				{
					try
					{
						transformationConnector.outputSpecificationBody(new org.apache.manifoldcf.ui.jsp.JspWrapper(out,adminprofile),pageContext.getRequest().getLocale(),pipelineSpecifications[j],1+j,tabSequenceInt,tabName);
					}
					finally
					{
						transformationConnectorPool.release(transformationConnection,transformationConnector);
					}
				}
			}
		}
	}

%>
		  <table class="displaytable">
			<tr><td class="separator" colspan="4"><hr/></td></tr>
			<tr><td class="message" colspan="4"><nobr>
<%
	if (connectionName.length() > 0 && outputPresent)
	{
%>
			<input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.Save")%>" onClick="javascript:Save()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.SaveThisJob")%>"/>
<%
	}
	else
	{
		if (tabName.equals(Messages.getString(pageContext.getRequest().getLocale(),"editjob.Connection")) && tabSequenceInt == -1)
		{
%>
			<input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.Continue")%>" onClick="javascript:Continue()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.ContinueToNextScreen")%>"/>
<%
		}
	}
%>
			&nbsp;<input type="button" value="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.cancel")%>" onClick="javascript:Cancel()" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"editjob.CancelJobEditing")%>"/>
			</nobr></td>
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
	variableContext.setParameter("target","listjobs.jsp");
%>
	<jsp:forward page="error.jsp"/>
<%
    }
%>

