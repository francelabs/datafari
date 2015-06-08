<% response.setHeader("Pragma","No-cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control", "no-cache");
response.setDateHeader("max-age", 0);
response.setContentType("text/html;charset=utf-8");
%><%@ include file="adminDefaults.jsp" %>

<%
/* $Id: setupAdminProfile.jsp 1506449 2013-07-24 09:05:31Z kwright $ */

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
String userID = variableContext.getParameter("userID");
String password = variableContext.getParameter("password");
if (userID == null)
	userID = "";
if (password == null)
	password = "";

adminprofile.login(threadContext,userID,password);
if (adminprofile.getLoggedOn())
	response.sendRedirect("index.jsp");
else
{
	// Go back to login page, but with signal that login failed
	response.sendRedirect("login.jsp?loginfailed=true");
}
%>
