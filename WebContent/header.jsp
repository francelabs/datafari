<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<!-- Logout.js to be merged into LoginSettings.widget.js -->
<script type="text/javascript" src="js/logout.js"></script>
<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/LanguageSelector.widget.js"></script>
<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/LoginSettings.widget.js"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="js/url.min.js"></script>
<header>
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
			<div id="languageSelector"></div>

			<div id="loginSettings">

				<%
					if (request.getUserPrincipal() != null) {
						if (request.getUserPrincipal().getName() != null) {
				%>
						<a id="adminLink" href="/Datafari/admin"></a>
						<a id="logout" onclick="logout();"></a>
				<%
						} 
					}
					else {
				%>
						<a id="loginLink" href="/Datafari/admin"></a>
				<% 
					}					
				%>

			</div>
		</div>
	</div>

	<div id="bar"></div>
</header>