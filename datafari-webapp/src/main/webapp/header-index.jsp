<!--
  IMPORTANT NOTE:
  Search tools are not included in the index menu because the modules related to 
  search tools (saved searches, favorites, advanced search, ...)
  are not loaded on this page. Integration of those in this page requires a bit 
  of work because the structure of the page is not the same as the search page.
-->
<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ArrayList"%>
<link rel="stylesheet" type ="text/css" href="css/search-header.css">
<script type="text/javascript" src="js/search-header.js"></script>
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
    src="js/AjaxFranceLabs/widgets/HeaderMenus.widget.js"></script>
<script type="text/javascript"
    src="js/AjaxFranceLabs/widgets/LoginDatafariForm.widget.js"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="js/url.min.js"></script>

<script>
    $(document).ready(function(){
		$("#loginDatafariLinks").click(function() {
			$("#dropdown-search-tools").removeClass("dropdown-search-click");
		});
		$("#dropdown-search-tools").click(function(){
			$("#dropdown-search-tools").addClass("dropdown-search-click");
  			$(".dropdown-second").toggleClass("secondary-visibility-show");
		});
		$("#solr").click(function(){
			$("#dropdown-search-tools").removeClass("dropdown-search-click");
  			$(".dropdown-second").removeClass("secondary-visibility-show");
		});
		$(".dropdown-second").click(function() {
			$("#dropdown-search-tools").addClass("dropdown-search-click");
		});
	});
</script>

<header>
  <div id="header-menus">
    <nav class=" navbar navbar-default">
      <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#header-menu" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="#"></a>
        </div>

        <div class="collapse navbar-collapse" id="header-menu">
        
          <div id="logo_desktop">
	          <img class="datafari-mini-logo" src="images/empty-pixel.png">
	        </div>

          <ul class="nav navbar-nav navbar-right" id= "loginDatafariLinks">

          <!-- Show the localized language section -->
          <li id="languageSelector"></li>
          <li><a id="basicSearchLink">Basic Search</a></li>
            <%
              if (request.getUserPrincipal() != null) {
                
                String[] adminRoles = {"SearchAdministrator", "SearchExpert"};
                for(String role : adminRoles) {
                  if(request.isUserInRole(role)) {
                    %>
                        <li class="dropdown">
                          <a class="dropdown-toggle" data-target="#" href="#" role="button" id="dropdownMenuLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            Admin <span class="caret"></span>
                          </a>
                          <ul class="dropdown-menu">
                            <li><a id="adminConsoleLink">Central Administration Console</a></li>
                            <li><a id="adminMCFLink">Connectors Framework</a></li>
                            <!-- Those will need a rework of the admin UI to work --
                            <li><a id="adminGoldenQueriesLink">Activate Golden Queries Mode</a></li>
                            <li><a id="adminDocBoostLink">Activate Document Boost Mode</a></li>
                            <li><a id="adminAnalyticsLink">Analytics</a></li>
                            <!-- -->
                          </ul>
                        </li>
                    <%
                    break;
                  }
                }
                %>
                <li><a id="logout" class="" onclick="logout();"></a></li>
              <%
              }
              else {
              %>
                <li><a id="loginLink" class="head-link"></a></li>
              <%
              }
              %>
          </ul>
        </div>
      </div>
    </nav>
  </div>
</header>
