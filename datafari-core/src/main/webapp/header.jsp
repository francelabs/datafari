<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ArrayList"%>
<!-- Logout.js used by admin/index.jsp as well -->
<script type="text/javascript" src="js/logout.js"></script>
<script type="text/javascript">
	var langHeader = new Array();
		<% 
		for (int i=0 ; i<LanguageUtils.availableLanguages.size() ; i++) { %>
			langHeader[<%= i %>] = "<%= LanguageUtils.availableLanguages.get(i) %>"; 
		<% } %>
		var portHeader = <%= request.getServerPort()%>;
		
</script>
<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/LanguageSelector.widget.js"></script>
<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/LoginDatafariLinks.widget.js"></script>
<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/LoginDatafariForm.widget.js"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="js/url.min.js"></script>
<header>
	<div id="header-wrapper">
		
		<div id="userSpace">
		

			<!-- Show the localized language section -->
			<div id="languageSelector"></div>

			<div id="loginDatafariLinks">

				<%
					if (request.getUserPrincipal() != null) {
						if (request.getUserPrincipal().getName() != null) {
				%>
						<a id="adminLink"></a>
						<a id="logout" onclick="logout();"></a>
				<%
						} 
					}
					else {
				%>
						<a id="loginLink"></a>
				<% 
					}					
				%>

			</div>
		</div>
	</div>

	<div id="bar"></div>
</header>