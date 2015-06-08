<%@ include file="adminDefaults.jsp" %>

<%

/* $Id: navigation.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
	// This module is meant to be called within a table cell, and will drop the
	// lcf navigation into the cell

%>

<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Outputs")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href="listtransformations.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listtransformationconnections")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListTransformationConnections")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="listoutputs.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listoutputconnections")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListOutputConnections")%></a></nobr>
	</li>
</ul>
<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Authorities")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href="listgroups.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listauthoritygroups")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListAuthorityGroups")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="listmappers.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listusermappings")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListUserMappings")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="listauthorities.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listauthorities")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListAuthorityConnections")%></a></nobr>
	</li>
</ul>
<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Repositories")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href="listconnections.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listrepositoryconnections")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListRepositoryConnections")%></a></nobr>
	</li>
</ul>
<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Jobs")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href="listjobs.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Listjobs")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ListAllJobs")%></a></nobr>						
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="showjobstatus.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Managejobs")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.StatusAndJobManagement")%></a></nobr>						
	</li>
</ul>
<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.StatusReports")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href="documentstatus.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Documentstatus")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.DocumentStatus")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="queuestatus.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Queuestatus")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.QueueStatus")%></a></nobr>
	</li>
</ul>
<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.HistoryReports")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href="simplereport.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Simplehistory")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.SimpleHistory")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="maxactivityreport.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Maximumactivity")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.MaximumActivity")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="maxbandwidthreport.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Maximumbandwidth")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.MaximumBandwidth")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="resultreport.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Resulthistogram")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.ResultHistogram")%></a></nobr>
	</li>
</ul>
<p class="menumain"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Miscellaneous")%></nobr></p>
<ul class="menusecond">
	<li class="menuitem">
		<nobr><a class="menulink" href='<%="http://manifoldcf.apache.org/release/trunk/"+Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Locale")+"/end-user-documentation.html"%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.Help")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.Help")%></a></nobr>
	</li>
	<li class="menuitem">
		<nobr><a class="menulink" href="logout.jsp" alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"navigation.LogOut")%>"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"navigation.LogOut")%></a></nobr>
	</li>
</ul>
