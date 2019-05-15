<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ArrayList"%>
<link rel="stylesheet" type ="text/css" href="css/search-header.css">
<!-- Logout.js used by admin/index.jsp as well -->
<script type="text/javascript" src="js/logout.js"></script>
<script type="text/javascript" src="js/search-header.js"></script>
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
  src="https://code.jquery.com/jquery-3.4.0.min.js"
  integrity="sha256-BJeo0qm959uMBGb65z40ejJYGSgR7REI4+CW1fNKwOg="
  crossorigin="anonymous"></script>
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
          <li class="dropdown">
            <a class="dropdown-toggle" data-target="#" href="#" role="button" id="dropdown-search-tools" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Search Tools <span class="caret"></span></a>
          </li>
          
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
            <% 
            if (false) { 
              %>
              <li class="dropdown">
                <a class="dropdown-toggle" data-target="#" href="#" role="button" id="dropdownMenuLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                  My Account <span class="caret"></span>
                </a>
                <ul class="dropdown-menu">
                  <li><a>First Item</a></li>
                  <li><a>Second Item item</a></li>
                </ul>
              </li>
              <%
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

 <!--secondary menu started-->
    <ul class="nav navbar-nav navbar-right dropdown-second" id="search-tools-sub-menu">
    
    <%
                  if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal().getName() != null) {
                  %>
      <li class="dropdown">
        <button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
          <a id="userAlertsLink">Alerts</a><span class="caret"></span>
        
        </button>
        <!--  
        <ul class="dropdown-menu" aria-labelledby="dropdownMenu1">
          <li></li>
          <li></li>
        </ul>
        -->
      </li>
      
      <%
                          }
                      }
    %>
    
    <!--  
      <li class="dropdown">
          <button class="btn btn-default dropdown-toggle" type="button" id="exportResultsDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <a id="exportResultsLink">Export Results</a>
          <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" aria-labelledby="exportResultsDropdownMenu">
            <li></li>
            <li></li>
          </ul>
        </li>
        
        -->
        <%
                  if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal().getName() != null) {
                  %>
                  <!--  
        <li class="dropdown">
            <button class="btn btn-default dropdown-toggle" type="button" id="savedSearchDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
              <a id="userSavedSearchLink">Saved Search</a>
            <span class="caret"></span>
            </button>
            <!--  
            <ul class="dropdown-menu" aria-labelledby="savedSearchDropdownMenu">
              <li></li>
              <li></li>
            </ul>
            
          </li>
          -->
          <%
                          }
                      }
    %>
          <li class="dropdown">
              <button class="btn btn-default dropdown-toggle" type="button" id="advancedSearchDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                <a id="advancedSearchLink">Advanced Search</a>
              <span class="caret"></span>
              </button>
              <!-- 
              <ul class="dropdown-menu" aria-labelledby="dropdownMenu4">
                <li></li>
                <li></li>
              </ul>
              -->
            </li>
            <%
                  if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal().getName() != null) {
                      if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES)!=null && DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES).equals("true") ){
                    
                  %>
                  
            <li class="dropdown">
                <button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                  <a id="userFavoritesLink">Favorites</a>
                <span class="caret"></span>
                </button>
                <!-- 
                <ul class="dropdown-menu" aria-labelledby="dropdownMenu5">
                  <li></li>
                  <li></li>
                </ul>
                -->
              </li>
              <%
                          }
                      }
                    }
                    %>
    </ul>
    <!--secondary menu end-->
</header>
