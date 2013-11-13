<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<header>
	<div id="header-wrapper">

		<div id="logo"></div>
		<div id="fl"></div>
		<%
			if (request.getUserPrincipal() != null) {
				String username = request.getUserPrincipal().getName();
		%>
		<div id="userSpace">
			<span class="em"> <%=username%>
			</span>
			<% 
			
			if (request.getSession().getAttribute("SSOFailure") != null){%>
			 &nbsp;<a href="<%=request.getContextPath()%>/logout.jsp">(D&eacute;connexion)</a>
			<% } 
			if (username.equals("admin")){ 
			
			if (request.getRequestURL().toString().contains("admin")){

				%>
				 &nbsp;<a href="<%=request.getContextPath()%>">Recherche</a>
				<% 
			} else {
			
			%>
			 &nbsp;<a href="<%=request.getContextPath()%>/admin">Administration</a>
			<% 
			}
			
			
			}
			%>
			
			
		</div>
		<%
			}
		%>
	</div>

	<div id="bar"></div>
</header>