<% response.setHeader("Pragma","No-cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control", "no-cache");
response.setDateHeader("max-age", 0);
response.setContentType("text/html;charset=utf-8");
%><%@ include file="adminDefaults.jsp" %>

<%
/* $Id: login.jsp 1601529 2014-06-09 23:19:08Z kwright $ */

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
			<%=Messages.getBodyString(pageContext.getRequest().getLocale(),"index.ApacheManifoldCFLogin")%>
		</title>
		<script type="text/javascript">
			<!--
			function login()
			{
				document.loginform.submit();
			}
			//-->
		</script>
	</head>
	<body class="standardbody">
		<table class="page">
			<tr><td colspan="2" class="banner"><jsp:include page="banner.jsp" flush="true"/></td></tr>
			<tr>
				<td colspan="2" class="window">

					<form class="standardform" name="loginform" action="setupAdminProfile.jsp" method="POST">
						<table class="displaytable">
<%
String value = variableContext.getParameter("loginfailed");
if (value != null && value.equals("true"))
{
%>
							<tr><td class="message" colspan="2"><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"index.LoginFailed")%></td></tr>
							<tr><td class="separator" colspan="2"><hr/></td></tr>
<%
}
%>
							<tr>
								<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"index.UserIDColon")%></nobr></td>
								<td class="value">
									<input name="userID" type="text" size="32" value=""/>
								</td>
							</tr>
							<tr>
								<td class="description"><nobr><%=Messages.getBodyString(pageContext.getRequest().getLocale(),"index.PasswordColon")%></nobr></td>
								<td class="value">
									<input name="password" type="password" size="32" value=""/>
								</td>
							</tr>
							<tr><td class="separator" colspan="2"><hr/></td></tr>
							<tr>
								<td class="message" colspan="2">
									<input type="button" onclick='Javascript:login();' value='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"index.Login")%>' alt='<%=Messages.getAttributeString(pageContext.getRequest().getLocale(),"index.Login")%>'/>
								</td>
							</tr>
						</table>
					</form>
				</td>
			</tr>
		</table>
	</body>
</html>

