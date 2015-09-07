<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ResourceBundle"  %>
<header>
<% ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale()); %>
	<div id="header-wrapper">
		<%
			if (ScriptConfiguration.getProperty("SOLRCLOUD") != null
					&& ScriptConfiguration.getProperty("SOLRCLOUD").equals(
							"true")) {
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


			<div id="loginSettings">

				<%
					if (request.getUserPrincipal() != null) {
						if (request.getUserPrincipal().getName() != null) {
				%>
				<a id="adminLink" href="/Datafari/admin"><% out.print(resourceBundle.getString("settings")); %></a>
				<a id="logout" href="/Datafari/SignOut"><% out.print(resourceBundle.getString("signout")); %></a>

				<%
					} 
				%>
				
				<%
					}
					else {
						%>
						<a id="loginLink" href="/Datafari/admin"><% out.print(resourceBundle.getString("signin")); %></a>
					<% 
					}
					
				%>

			</div>
		</div>
	</div>

	<div id="bar"></div>
</header>