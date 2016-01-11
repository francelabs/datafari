<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ResourceBundle"  %>
<script type="text/javascript" src="js/logout.js"></script>
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

			<!-- Show the localized language section -->

			<div id="selectLang">

				<%
					out.print(resourceBundle.getString("selectLang"));
				%>

				<select>
					<option value="en"> <% out.print(resourceBundle.getString("english_locale")); %> </option>
					<option value="fr"> <% out.print(resourceBundle.getString("french_locale")); %> </option>
					<option value="it"> <% out.print(resourceBundle.getString("italian_locale")); %> </option>
				</select>

			</div>



			<div id="loginSettings">

				<%
					if (request.getUserPrincipal() != null) {
						if (request.getUserPrincipal().getName() != null) {
				%>
				<a id="adminLink" href="/Datafari/admin"><% out.print(resourceBundle.getString("settings")); %></a>
				<a id="logout" onclick="logout();" style="cursor: pointer; font-weight: bold;"><% out.print(resourceBundle.getString("signout")); %></a>

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