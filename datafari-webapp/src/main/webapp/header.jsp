<%@page import="org.apache.jena.sparql.function.library.print"%>
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
		
		<div id="upright-menu">
		

			<!-- Show the localized language section -->
			<!--<div id="languageSelector"></div> -->

			<div id="loginDatafariLinks">
			 <div id="commonLinks">
			  <a id="basicSearchLink" class="head-link"></a>
				<a id="advancedSearchLink" class="head-link"></a>
				<%
					if (request.getUserPrincipal() != null) {
						if (request.getUserPrincipal().getName() != null) {
				%>
							<a id="favorites" class="head-link"></a>
							<a id="parameters" class="head-link"></a>
				<%
						}
				%>
						</div>
						<div id="connectionLink">
				<% 
						String[] adminRoles = {"SearchAdministrator", "SearchExpert"};
		        for(String role : adminRoles) {
		          if(request.isUserInRole(role)) {
		            %>
		              <a id="adminLink" class="head-link"></a>
		            <%
		            break;
		          }
		        }
				%>
						<a id="logout" class="head-link" onclick="logout();"></a></div>
				<%
						} 
					else {
				%>
				    </div>
						<div id="connectionLink">
						<img id="logo_mobile" src="images/datafari.png">
						<a id="loginLink" class="head-link"></a></div>
				<% 
					}					
				%>

			</div>
		</div>
	</div>

	<div id="bar"></div>
</header>