<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: showjobstatus.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
boolean maintenanceUnderway = org.apache.manifoldcf.crawler.system.ManifoldCF.checkMaintenanceUnderway();

%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<link rel="StyleSheet" href="style.css" type="text/css" media="screen"/>
	<title>
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.ApacheManifoldCFStatusOfAllJobs")%>
	</title>

	<script type="text/javascript">
	<!--

	function Start(jobID)
	{
		document.liststatuses.op.value="Start";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	function StartMinimal(jobID)
	{
		document.liststatuses.op.value="StartMinimal";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	function Abort(jobID)
	{
		document.liststatuses.op.value="Abort";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	function Restart(jobID)
	{
		document.liststatuses.op.value="Restart";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	function RestartMinimal(jobID)
	{
		document.liststatuses.op.value="RestartMinimal";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	function Pause(jobID)
	{
		document.liststatuses.op.value="Pause";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	function Resume(jobID)
	{
		document.liststatuses.op.value="Resume";
		document.liststatuses.jobid.value=jobID;
		document.liststatuses.submit();
	}

	//-->
	</script>

</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="window">
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.StatusOfJobs")%></p>
<%
if (maintenanceUnderway == false)
{
%>
	<form class="standardform" name="liststatuses" action="execute.jsp" method="POST">
		<input type="hidden" name="op" value="Continue"/>
		<input type="hidden" name="type" value="jobstatus"/>
		<input type="hidden" name="jobid" value=""/>

<%
    try
    {
	// Get the max count
	int maxCount = LockManagerFactory.getIntProperty(threadContext,"org.apache.manifoldcf.ui.maxstatuscount",500000);
	// Get the job manager handle
	IJobManager manager = JobManagerFactory.make(threadContext);
	JobStatus[] jobs = manager.getAllStatus(true,maxCount);
%>
		<table class="datatable">
			<tr>
				<td class="separator" colspan="8"><hr/></td>
			</tr>
			<tr class="headerrow">
				<td class="columnheader"></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Name")%></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Status")%></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.StartTime")%></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.EndTime")%></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Documents")%></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Active")%></td><td class="columnheader"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Processed")%></td>
			</tr>
<%
	int i = 0;
	while (i < jobs.length)
	{
		JobStatus js = jobs[i++];
		String statusName;
		int status = js.getStatus();
		switch (status)
		{
		case JobStatus.JOBSTATUS_NOTYETRUN:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Notyetrun");
			break;
		case JobStatus.JOBSTATUS_RUNNING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Running");
			break;
		case JobStatus.JOBSTATUS_RUNNING_UNINSTALLED:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Runningnoconnector");
			break;
		case JobStatus.JOBSTATUS_ABORTING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Aborting");
			break;
		case JobStatus.JOBSTATUS_RESTARTING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Restarting");
			break;
		case JobStatus.JOBSTATUS_STOPPING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Stopping");
			break;
		case JobStatus.JOBSTATUS_RESUMING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Resuming");
			break;
		case JobStatus.JOBSTATUS_PAUSED:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Paused");
			break;
		case JobStatus.JOBSTATUS_COMPLETED:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Done");
			break;
		case JobStatus.JOBSTATUS_WINDOWWAIT:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Waiting");
			break;
		case JobStatus.JOBSTATUS_STARTING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Startingup");
			break;
		case JobStatus.JOBSTATUS_DESTRUCTING:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Cleaningup");
			break;
		case JobStatus.JOBSTATUS_JOBENDCLEANUP:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Terminating");
			break;
		case JobStatus.JOBSTATUS_JOBENDNOTIFICATION:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Endnotification");
			break;
		case JobStatus.JOBSTATUS_ERROR:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.ErrorColon")+" "+js.getErrorText();
			break;
		default:
			statusName = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Unknown");
			break;
		}
		String startTime = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Notstarted");
		if (js.getStartTime() != -1L)
			startTime = new Date(js.getStartTime()).toString();
		String endTime = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Aborted");
		if (js.getStartTime() == -1L)
			endTime = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Neverrun");
		else
		{
			if (js.getEndTime() == -1L)
			{
				if (status == JobStatus.JOBSTATUS_COMPLETED)
					endTime = Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Aborted");
				else
					endTime = "";
			}
			else
				endTime = new Date(js.getEndTime()).toString();
		}
%>
		<tr <%="class=\""+((i%2==0)?"evendatarow":"odddatarow")+"\""%>>
			<td class="columncell">
				<nobr>
<%
		if (status == JobStatus.JOBSTATUS_NOTYETRUN ||
			status == JobStatus.JOBSTATUS_COMPLETED ||
			status == JobStatus.JOBSTATUS_ERROR)
		{
%>
					<a href='<%="javascript:Start(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Startjob")+" "+js.getJobID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Start")%></a>
					<a href='<%="javascript:StartMinimal(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Startjob")+" "+js.getJobID()+" "+Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.minimally")%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Startminimal")%></a>
<%
		}
		if (status == JobStatus.JOBSTATUS_RUNNING ||
			status == JobStatus.JOBSTATUS_RUNNING_UNINSTALLED ||
			status == JobStatus.JOBSTATUS_WINDOWWAIT ||
			status == JobStatus.JOBSTATUS_PAUSED ||
			status == JobStatus.JOBSTATUS_STARTING)
		{
%>
					<a href='<%="javascript:Restart(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Restartjob")+" "+js.getJobID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Restart")%></a>
					<a href='<%="javascript:RestartMinimal(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Restartjob")+" "+js.getJobID()+" "+Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.minimally")%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Restartminimal")%></a>
<%
		}
		if (status == JobStatus.JOBSTATUS_RUNNING ||
			status == JobStatus.JOBSTATUS_RUNNING_UNINSTALLED ||
			status == JobStatus.JOBSTATUS_WINDOWWAIT)
		{
%>
					<a href='<%="javascript:Pause(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Pausejob")+" "+js.getJobID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Pause")%></a>
<%
		}
		if (status == JobStatus.JOBSTATUS_RUNNING ||
			status == JobStatus.JOBSTATUS_RUNNING_UNINSTALLED ||
			status == JobStatus.JOBSTATUS_STOPPING ||
			status == JobStatus.JOBSTATUS_RESUMING ||
			status == JobStatus.JOBSTATUS_WINDOWWAIT ||
			status == JobStatus.JOBSTATUS_PAUSED ||
			status == JobStatus.JOBSTATUS_STARTING ||
			status == JobStatus.JOBSTATUS_RESTARTING)
		{
%>
					<a href='<%="javascript:Abort(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Abortjob")+" "+js.getJobID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Abort")%></a>
<%
		}
		if (status == JobStatus.JOBSTATUS_PAUSED)
		{
%>
					<a href='<%="javascript:Resume(\""+js.getJobID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.Resumejob")+" "+js.getJobID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Resume")%></a>
<%
		}
%>
				</nobr>
			</td>
			<td class="columncell"><%="<!--jobid="+js.getJobID()+"-->"%><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(js.getDescription())%></td><td class="columncell"><%=statusName%></td><td class="columncell"><%=startTime%></td><td class="columncell"><%=endTime%></td>
			<td class="columncell"><%=(js.getQueueCountExact()?"":"&gt; ")%><%=new Long(js.getDocumentsInQueue()).toString()%></td>
			<td class="columncell"><%=(js.getOutstandingCountExact()?"":"&gt; ")%><%=new Long(js.getDocumentsOutstanding()).toString()%></td>
			<td class="columncell"><%=(js.getProcessedCountExact()?"":"&gt; ")%><%=new Long(js.getDocumentsProcessed()).toString()%></td>
		</tr>
<%
	}
%>

			<tr>
				<td class="separator" colspan="8"><hr/></td>
			</tr>
		<tr><td class="message" colspan="8"><a href="showjobstatus.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"showjobstatus.RefreshStatus")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.Refresh")%></a></td></tr>
		</table>

<%
    }
    catch (ManifoldCFException e)
    {
	out.println("Error: "+e.getMessage());

	e.printStackTrace();
    }
%>
	    </form>
<%
}
else
{
%>
		<table class="displaytable">
			<tr><td class="separator" colspan="1"><hr/></td></tr>
			<tr><td class="message"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"showjobstatus.PleaseTryAgainLater")%></td></tr>
		</table>
<%
}
%>
       </td>
      </tr>
    </table>


</body>

</html>
