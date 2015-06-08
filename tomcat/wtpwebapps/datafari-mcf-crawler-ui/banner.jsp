<%@ include file="adminDefaults.jsp" %>

<%

/* $Id: banner.jsp 1555930 2014-01-06 17:32:16Z kwright $ */

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
	// lcf banner into the cell
	String dateString = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
%>

<table class="bannertable">
    <tr class="bannerrow">
	<td class="headerimage"><img src="ManifoldCF-logo.png"/></td>
          <td class="headercell">
	    <table class="headertable">
		<tr class="headerdaterow">
			<td class="headerdate"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(dateString)%></td>
		</tr>
	          <tr class="headerrow">
			<td class="header"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"banner.DocumentIngestion")%></td>
		</tr>
	    </table>
	</td>
    </tr>
</table>



