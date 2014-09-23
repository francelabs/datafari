<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
	<%@ page import="com.francelabs.datafari.utils.*" %>
<header>
	<div id="header-wrapper">
		<%
			if (ScriptConfiguration.isConfigAvailable() || ScriptConfiguration.getProperty("SOLRCLOUD").equals("false")){
		%>
		<div id="logo"></div>
		<%
			} else {
		%>
		
		<div id="biglogo"></div>
		
		<%
			}
		%>
		<div id="userSpace">

			<%
				if (request.getRequestURL().toString().contains("admin")){
			%>
			&nbsp;<a href="<%=request.getContextPath()%>">Search</a>
			<%
				} else {
			%>
			&nbsp;<a href="<%=request.getContextPath()%>/admin">Administration</a>
			<%
				}
			%>


		</div>
	</div>

	<div id="bar"></div>
</header>