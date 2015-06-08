<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: error.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
		<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"error.ApacheManifoldCFError")%>
	</title>

</head>

<body class="standardbody">

    <table class="page">
      <tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
      <tr><td class="navigation"><jsp:include page="navigation.jsp" flush="true"/></td>
       <td class="window">
	<p class="windowtitle"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"error.Error")%></p>

<%
	// These have to be fetched from request rather than variableContext since error
	// forwards screw up the multipart wrapper
	String errorText = variableContext.getParameter("text");
	String target = variableContext.getParameter("target");
%>
	<table class="displaytable">
		<tr><td class="message"><%=org.apache.manifoldcf.ui.util.Encoder.bodyEscape(errorText)%></td></tr>
		<tr><td class="separator"><hr/></td></tr>
		<tr><td class="message"><a href='<%=org.apache.manifoldcf.core.util.URLEncoder.encode(target)%>' alt="<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"error.Return")%>">OK</a></td></tr>
	</table>

       </td>
      </tr>
    </table>

</body>

</html>
