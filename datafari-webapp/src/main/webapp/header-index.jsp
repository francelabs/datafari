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

  if ($(window).width() < 800) {
          // Prevents the drag on right/left side while in mobile view
          $('#container-viewport-switch').removeClass('container-fluid');
          $('#container-viewport-switch').addClass('container');

          // // Switch attributes of Admin on mobile view
          // $('#dropdownAdminLink').removeAttr('class data-target href role data-toggle aria-haspopup');
          // $('#dropdownAdminLink').attr("role", "button");
          // $('#dropdownAdminLink').attr("class", "dropdown-toggle");
          // $('#dropdownAdminLink').attr("data-toggle", "collapse");
          // $('#dropdownAdminLink').attr("data-target", "#admin-sub-menu-mobile");

        }

    $("#loginDatafariLinks").click(function() {
      $("#dropdown-search-tools").removeClass("dropdown-search-click");
      $("#dropdownAdminLink").removeClass("dropdown-search-click");
    });
    $("#languageSelector").click(function(){
      $(".dropdown-toggle").removeClass("active");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
          $(".dropdown-admin-second").removeClass("secondary-visibility-show");
          $(".dropdown-second").removeClass("secondary-visibility-show");
    }); 
    $("#basicSearchLink").click(function(){
      $(".dropdown-toggle").removeClass("active");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
          $(".dropdown-admin-second").removeClass("secondary-visibility-show");
          $(".dropdown-second").removeClass("secondary-visibility-show");
    });  
    $("#dropdown-search-tools").click(function(){
    $(".dropdown-toggle").removeClass("active");    
      $("#search-tools-sub-menu").addClass("body-click-toggle-off");      
      $("#dropdown-search-tools").addClass("dropdown-search-click");
      $("#dropdownAdminLink").removeClass("dropdown-search-click");
        $(".dropdown-second").toggleClass("secondary-visibility-show");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
    });
    $("#dropdownAdminLink").click(function(){
      $(".dropdown-toggle").removeClass("active");
      $("#admin-sub-menu-item").addClass("body-click-toggle-off");
      $("#dropdownAdminLink").addClass("dropdown-search-click");
      $("#dropdown-search-tools").removeClass("dropdown-search-click");  
        $(".dropdown-admin-second").toggleClass("secondary-visibility-show");
        $(".dropdown-second").removeClass("secondary-visibility-show");
    }); 
    $("#solr").click(function(){
      $("#dropdownAdminLink").removeClass("dropdown-search-click");
      $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
    });
    $(".dropdown-second").click(function() {
      $("#dropdown-search-tools").addClass("dropdown-search-click");
    });   
    
    $(".dropdown-admin-second").click(function() {
      $("#dropdownAdminLink").addClass("dropdown-search-click");
    });
 
    function closeMenu(){
      /*
     Checks if any of the sub-menus has "body-click-toggle-off" class (added on click on their respective parents)
       IF so : -Removes the green underline (.active)
               -Hides the sub-menu ("secondary-visibility-show")
               -Removes class "body-click-toggle-off" for it to be enabled on the next clic
    */
      if ($('#search-tools-sub-menu').hasClass('body-click-toggle-off') || 
        $('#admin-sub-menu-item').hasClass('body-click-toggle-off')) {
        $(".dropdown-toggle").removeClass("active");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
        $('#search-tools-sub-menu').removeClass('body-click-toggle-off');
        $('#admin-sub-menu-item').removeClass('body-click-toggle-off');
      }
        
    }

    $(document).click(function(e){

    // Check if click was triggered on or within the selected element target
    if( $(e.target).closest().length > 0 ) {
        return false;
    }

    closeMenu();
    });
});
</script>

<header>
  <div id="header-menus">
    <nav class=" navbar navbar-default">
      <div id="container-viewport-switch" class="container-fluid">
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
	          <a href="/Datafari/Search"><img class="datafari-mini-logo" src="images/empty-pixel.png"></a>
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
                          <a class="dropdown-toggle" data-target="#" href="#" role="button" id="dropdownAdminLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            Admin <span class="caret"></span>
                          </a>
                          <!-- <ul class="dropdown-menu">
                            <li><a id="adminConsoleLink">Central Administration Console</a></li>
                            <li><a id="adminMCFLink">Connectors Framework</a></li> 
                             Those will need a rework of the admin UI to work  
                            
                            <li><a id="adminGoldenQueriesLink">Activate Golden Queries Mode</a></li>
                            <li><a id="adminDocBoostLink">Activate Document Boost Mode</a></li>
                            <li><a id="adminAnalyticsLink">Analytics</a></li>
                            
                          </ul> -->
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
            <li><a id="helpLink" class="head-link" href="/Datafari/help">Help</a></li>
          </ul>
        </div>
      </div>
    </nav>
    <!-- Admin Sub-menu for Mobile STARTS -->
<!--     <div class="navbar-collapse collapse" id="admin-sub-menu-mobile">
      <ul class="nav navbar-nav navbar-right">
        <li class="dropdown"><a href="/Datafari/admin/?lang=en" target="blank" id="adminConsoleLink">Main </a></li>
        <li class="dropdown"><a href="/datafari-mcf-crawler-ui/" target="blank" id="adminMCFLink" >Data Crawlers </a></li>
      </ul>
    </div> -->
<!-- Admin Sub-menu for Mobile STOPS -->

    <!--Secondary Menu Admin STOPS-->

    <ul class="nav navbar-nav navbar-right dropdown-admin-second dropdown-admin-second-main" id="admin-sub-menu-item">
    
    <%
                  if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal().getName() != null) {
                  %>
      <li class="dropdown">
        <span><a href="/Datafari/admin/?lang=en" target="blank" class="btn sub-btn" id="adminConsoleLink" >Main</a></span>
      </li>
      
      <%
                          }
                      }
    %>
        <%
                  if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal().getName() != null) {
                  %>
        <li class="dropdown">
           <span><a href="/datafari-mcf-crawler-ui/" target="blank" class="btn sub-btn" id="adminMCFLink" >Data Crawlers</a></span>
          </li>
          <%
                          }
                      }
    %>          
            
    </ul>
    <!--Secondary Menu Admin STOPS-->
  </div>
</header>
