<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<header>
	<div id="header-wrapper">

		<div id="logo"></div>
		<div id="userSpace">
			
			<%
			
			if (request.getRequestURL().toString().contains("admin")){

				%>
				 &nbsp;<a href="<%=request.getContextPath()%>">Recherche</a>
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