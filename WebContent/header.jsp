<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
	<%@ page import="com.francelabs.datafari.utils.*" %>
<header>
	<div id="header-wrapper">
		<%
			if (ScriptConfiguration.getProperty("SOLRCLOUD")!= null && ScriptConfiguration.getProperty("SOLRCLOUD").equals("true")){
		%>
		
		
		<div id="biglogo"></div>
		<%
			} else {
		%>
		
		
		<!-- <div id="logo"></div> -->
		
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
			&nbsp;<a id="loginSettings" href="<%=request.getContextPath()%>/admin"></a>
			<%
				}
			%>


		</div>
	</div>

	<div id="bar"></div>
</header>