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

        // Save search / Create Relevancy is named dynamically here (These should go respectively to : RelevancyQuery.module.js & SaveSearch.widget.js)
        $('#create-relevancy').html(window.i18n.msgStore['relevancy_button']);
        $('#save_search_label').html(window.i18n.msgStore['save_search_button']);

        //Prevents the glitch where you have to wait for relevancy search to show up  
        setTimeout(function() {
              $("#relevancy-div").trigger('click');
          },10);

        if ($(window).width() < 800) {
          // Prevents the drag on right/left side while in mobile view
          $('#container-viewport-switch').removeClass('container-fluid');
          $('#container-viewport-switch').addClass('container');         

        } else {
          $('#search-tools-sub-menu-mobile').empty();
          $('#admin-sub-menu-mobile').empty();
          $('#account-sub-menu-mobile').empty();
        }

      $("#loginDatafariLinks").click(function() {
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
        $("#dropdown-my-account").removeClass("dropdown-search-click");
      });
      $("#languageSelector").click(function(){
        $(".dropdown-toggle").removeClass("active");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-account-second").removeClass("secondary-visibility-show");
      }); 
      $("#basicSearchLink").click(function(){
        $(".dropdown-toggle").removeClass("active");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-account-second").removeClass("secondary-visibility-show");
      });  
      $("#dropdown-search-tools").click(function(){
        $(".dropdown-toggle").removeClass("active");    
        $("#search-tools-sub-menu").addClass("body-click-toggle-off");      
        $("#dropdown-search-tools").addClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
        $("#dropdown-my-account").removeClass("dropdown-search-click");
        $(".dropdown-second").toggleClass("secondary-visibility-show");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
        $(".dropdown-account-second").removeClass("secondary-visibility-show");
      });
      $("#dropdownAdminLink").click(function(){
        $(".dropdown-toggle").removeClass("active");
        $("#admin-sub-menu-item").addClass("body-click-toggle-off");
        $("#dropdownAdminLink").addClass("dropdown-search-click");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdown-my-account").removeClass("dropdown-search-click");     
        $(".dropdown-admin-second").toggleClass("secondary-visibility-show");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-account-second").removeClass("secondary-visibility-show");
      }); 

      $("#dropdown-my-account").click(function(){
        $(".dropdown-toggle").removeClass("active");
        $("#account-sub-menu-item").addClass("body-click-toggle-off");
        $("#dropdown-my-account").addClass("dropdown-search-click");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdownAdminLink").removeClass("dropdown-search-click");     
        $(".dropdown-account-second").toggleClass("secondary-visibility-show");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
      }); 

      $("#solr").click(function(){
        $("#dropdownAdminLink").removeClass("dropdown-search-click");
        $("#dropdown-search-tools").removeClass("dropdown-search-click");
        $("#dropdown-my-account").removeClass("dropdown-search-click");
        $(".dropdown-second").removeClass("secondary-visibility-show");
        $(".dropdown-admin-second").removeClass("secondary-visibility-show");
        $(".dropdown-account-second").removeClass("secondary-visibility-show");
      });
      $(".dropdown-second").click(function() {
        $("#dropdown-search-tools").addClass("dropdown-search-click");
      });   

      $(".dropdown-admin-second").click(function() {
        $("#dropdownAdminLink").addClass("dropdown-search-click");
      });

      $(".dropdown-account-second").click(function() {
        $("#dropdown-my-account").addClass("dropdown-search-click");
      });

      function closeMenu(){
      /*
     Checks if any of the sub-menus has "body-click-toggle-off" class (added on click on their respective parents)
       IF so : -Removes the green underline (.active)
               -Hides"fast" the sub-menu ("secondary-visibility-show")
               -Removes class "body-click-toggle-off" for it to be enabled on the next clic
               */
       if ($('#search-tools-sub-menu').hasClass('body-click-toggle-off') ||
        $('#admin-sub-menu-item').hasClass('body-click-toggle-off') ||
        $('#account-sub-menu-item').hasClass('body-click-toggle-off')) {

        $(".dropdown-toggle").removeClass("active");
      $(".dropdown-second").removeClass("secondary-visibility-show");
      $(".dropdown-admin-second").removeClass("secondary-visibility-show");
      $(".dropdown-account-second").removeClass("secondary-visibility-show");
      $('#search-tools-sub-menu').removeClass('body-click-toggle-off');
      $('#admin-sub-menu-item').removeClass('body-click-toggle-off');
      $('#account-sub-menu-item').removeClass('body-click-toggle-off');
      }

      } 

  $(document).click(function(e){
    // Check if click was triggered on or within the selected element target
    if( $(e.target).closest().length > 0 ) {
      return false;
    }

    closeMenu();
  });

// Add Listeners to every sub-menu items to hide"Save Search" when they are clicked.
  $('#externalSourcesLink').click(function() {
    $("#save_search_label").hide("fast");
    $("#create-relevancy").hide("fast");
  });
  $("#alertsDropdownMenu").click(function() {
    $("#save_search_label").hide("fast");
    $("#create-relevancy").hide("fast");
  });
  $("#savedSearchDropdownMenu").click(function() {
    $("#save_search_label").hide("fast");
    $("#create-relevancy").hide("fast");
  });
  $("#advancedSearchDropdownMenu").click(function() {
    $("#save_search_label").hide("fast");
    $("#create-relevancy").hide("fast");   
  });
  $("#favoritesDropdownMenu").click(function() {
    $("#save_search_label").hide("fast");
    $("#create-relevancy").hide("fast");   
  });
  $("#basicSearchLink").click(function() {
    $("#save_search_label").show();
    $("#create-relevancy").show();   
  });
}); 
      </script>

      <header>
        <div id="header-menus">
          <nav class=" navbar navbar-default">
            <div id="container-viewport-switch" class="container-fluid">
              <div class="navbar-header">
                <button id="collapse-all-sub-menus" type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#header-menu" aria-expanded="false">
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
                  <li id="mobile-node-search" class="dropdown">
                    <a class="dropdown-toggle" data-target="#" href="#" role="button" id="dropdown-search-tools" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Search Tools <span class="caret"></span></a>
                    <div class="navbar-collapse collapse" id="search-tools-sub-menu-mobile"></div>
                  </li>

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
                    <% 
                    if (request.getUserPrincipal() != null) { 
                    %>
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
        <li><a id="helpLink" class="head-link" href="/Datafari/help">Help</a></li>
      </ul>
    </div>
  </div>
</nav>
<!-- Search Tools Sub-menu for Mobile STARTS -->
<!-- <div class="navbar-collapse collapse" id="search-tools-sub-menu-mobile">
  <ul class="nav navbar-nav navbar-right">
    <li class="dropdown"><a id="userAlertsLink">Alerts</a></li>
    <li class="dropdown"><a id="userSavedSearchLink">Saved Search </a></li>
    <li class="dropdown"><a id="advancedSearchLinkMobile">Advanced Search </a></li>
    <li class="dropdown"><a id="userFavoritesLink">Favorites </a></li>
  </ul>
</div> -->
<!-- Search Tools Sub-menu for Mobile STOPS -->

<!-- Admin Sub-menu for Mobile STARTS -->
<!-- <div class="navbar-collapse collapse" id="admin-sub-menu-mobile">
  <ul class="nav navbar-nav navbar-right">
    <li class="dropdown"><a href="/Datafari/admin/?lang=en" target="blank" id="adminConsoleLink">Main </a></li>
    <li class="dropdown"><a href="/datafari-mcf-crawler-ui/" target="blank" id="adminMCFLink" >Data Crawlers </a></li>
  </ul>
</div> -->
<!-- Admin Sub-menu for Mobile STOPS -->

<!-- Account Sub-menu for Mobile STARTS -->
<!-- <div class="navbar-collapse collapse" id="account-sub-menu-mobile">
  <ul class="nav navbar-nav navbar-right">
    <li class="dropdown"><a id="externalSourcesLink">External Sources </a></li>
  </ul>
</div> -->
<!-- Account Sub-menu for Mobile STOPS -->

<!--Secondary Menu Search Tools STARTS-->
<ul class="nav navbar-nav navbar-right dropdown-second" id="search-tools-sub-menu">

  <%
  if (request.getUserPrincipal() != null) {
  if (request.getUserPrincipal().getName() != null) {
  %>
  <li class="dropdown">
      <span id="alertsDropdownMenu"><a class="btn sub-btn" data-toggle="dropdown" id="userAlertsLink" >Alerts</a></span>
    </li>

    <%
  }
}
%>

<!--Export Results not relevant / Commented out for furure developpement.   -->

      <!-- <li class="dropdown">
          <button class="btn btn-default dropdown-toggle" type="button" id="exportResultsDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <a id="exportResultsLink">Export Results</a>
            <span class="caret"></span> 
          </button>
          <ul class="dropdown-menu" aria-labelledby="exportResultsDropdownMenu">
            <li></li>
            <li></li>
          </ul>
        </li> -->
        <%
        if (request.getUserPrincipal() != null) {
        if (request.getUserPrincipal().getName() != null) {
        %>
        <%
      }
    }
    %>
    <li class="dropdown">
      <span id="advancedSearchDropdownMenu"><a class="btn sub-btn" data-toggle="dropdown" id="advancedSearchLink" >Advanced Search</a></span>
          </li>

          <%
          if (request.getUserPrincipal() != null) {
          if (request.getUserPrincipal().getName() != null) {
          %>
        <%
      }
    }
    %>
    <%
    if (request.getUserPrincipal() != null) {
    if (request.getUserPrincipal().getName() != null) {
    if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES)!=null && DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES).equals("true") ){

    %>

    <li class="dropdown">
      <span id="favoritesDropdownMenu"><a class="btn sub-btn" data-toggle="dropdown" id="userFavoritesLink" >Favorites</a></span>
            </li>
            <%
          }
        }
      }
      %>
    </ul>
    <!--Secondary Menu Search Tools STOPS-->


    <!--Secondary Menu Admin STARTS-->

    <ul class="nav navbar-nav navbar-right dropdown-admin-second" id="admin-sub-menu-item">

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

<%
      if (request.getUserPrincipal() != null) {
      if (request.getUserPrincipal().getName() != null) {
      %>

    <%
  }
}
%>     

  </ul>
  <!--Secondary Menu Admin STOPS-->


  <!--Secondary Menu Account STARTS-->
  
  <ul class="nav navbar-nav navbar-right dropdown-account-second" id="account-sub-menu-item">

    <%
    if (request.getUserPrincipal() != null) {
    if (request.getUserPrincipal().getName() != null) {
    %>
    <li class="dropdown">
      <span id="externalSourcesDropdownMenu"><a class="btn sub-btn" data-toggle="dropdown" id="externalSourcesLink" >External Sources</a></span>
    </li>

    <%
  }
}
%>   
<!--Secondary Menu Account STOPS-->
</div>


<!-- Modal -->
<div class="modal fade" id="modalSearchRelevancy" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-body">
      </div>
    </div>
  </div>
</div>
</header>
