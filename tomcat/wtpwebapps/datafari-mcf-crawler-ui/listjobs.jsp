<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: listjobs.jsp 1603199 2014-06-17 15:17:34Z kwright $ */

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
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.ApacheManifoldCFListJobDescriptions")%>
	</title>

	<script type="text/javascript">
	<!--

	function Delete(jobID)
	{
		if (confirm("Warning: Deleting this job will remove all\nassociated documents from the index.\nDo you want to proceed?"))
		{
			document.listjobs.op.value="Delete";
			document.listjobs.jobid.value=jobID;
			document.listjobs.submit();
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
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.JobList")%></p>
	<form class="standardform" name="listjobs" action="execute.jsp" method="POST">
		<input type="hidden" name="op" value="Continue"/>
		<input type="hidden" name="type" value="job"/>
		<input type="hidden" name="jobid" value=""/>

<%
    try
    {
	// Get the job manager handle
	IJobManager manager = JobManagerFactory.make(threadContext);
	IJobDescription[] jobs = manager.getAllJobs();
%>
		<table class="datatable">
			<tr>
				<td class="separator" colspan="5"><hr/></td>
			</tr>
			<tr class="headerrow">
				<td class="columnheader"></td>
				<td class="columnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.Name")%></nobr></td>
				<td class="columnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.OutputConnection")%></nobr></td>
				<td class="columnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.RepositoryConnection")%></nobr></td>
				<td class="columnheader"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.ScheduleType")%></nobr></td>
			</tr>
<%
	for (int i = 0; i < jobs.length; i++)
	{
		IJobDescription jd = jobs[i];

		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < jd.countPipelineStages(); j++)
		{
			if (jd.getPipelineStageIsOutputConnection(j))
			{
				if (sb.length() > 0)
					sb.append(",");
				sb.append(jd.getPipelineStageConnectionName(j));
			}
		}
		String outputConnectionNames = sb.toString();
		
		String jobType = "";
		switch (jd.getType())
		{
		case IJobDescription.TYPE_CONTINUOUS:
			jobType = "Continuous crawl";
			break;
		case IJobDescription.TYPE_SPECIFIED:
			jobType = "Specified time";
			break;
		default:
		}

%>
		<tr <%="class=\""+((i%2==0)?"evendatarow":"odddatarow")+"\""%>>
		    <td class="columncell">
			<nobr>
				<a href='<%="viewjob.jsp?jobid="+jd.getID()%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listjobs.Viewjob")+" "+jd.getID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.View")%></a>
				<a href='<%="editjob.jsp?jobid="+jd.getID()%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listjobs.Editjob")+" "+jd.getID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.Edit")%></a>
				<a href='<%="javascript:Delete(\""+jd.getID()+"\")"%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listjobs.DeleteJob")+" "+jd.getID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.Delete")%></a>
				<a href='<%="editjob.jsp?origjobid="+jd.getID()%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listjobs.CopyJob")+" "+jd.getID()%>'><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.Copy")%></a>
			</nobr>
		    </td>
		    <td class="columncell"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(jd.getDescription())%></td>
		    <td class="columncell"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(outputConnectionNames)%></td>
		    <td class="columncell"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(jd.getConnectionName())%></td>
		    <td class="columncell"><%=jobType%></td>
		</tr>
<%
	}
%>
			<tr>
				<td class="separator" colspan="5"><hr/></td>
			</tr>
		<tr><td class="message" colspan="5"><a href="editjob.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"listjobs.Addajob")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"listjobs.AddaNewJob")%></a></td></tr>
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
