<%@ page language="java" contentType="text/html; charset=utf-8"
pageEncoding="utf-8"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ArrayList"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script type="text/javascript" src="<c:url value="/resources/js/search-header.js" />"></script>
<!-- Logout.js used by admin/index.jsp as well -->
<script type="text/javascript" src="<c:url value="/resources/js/logout.js" />"></script>
<script type="text/javascript">
  var langHeader = new Array();
  <%
  for (int i=0 ; i<LanguageUtils.availableLanguages.size() ; i++) { %>
    langHeader[<%= i %>] = "<%= LanguageUtils.availableLanguages.get(i) %>";
    <% } %>
    var portHeader = <%= request.getServerPort()%>;

</script>
<script type="text/javascript"
src="<c:url value="/resources/js/AjaxFranceLabs/widgets/LanguageSelector.widget.js" />"></script>
<script type="text/javascript"
src="<c:url value="/resources/js/AjaxFranceLabs/widgets/LoginDatafariForm.widget.js" />"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="<c:url value="/resources/js/url.min.js" />"></script>
<script type ="text/javascript" src ="<c:url value="/resources/js/header.js" />"></script>

<header>
  <div id="connection-info-header" class="bg-light" style="display:none"></div>
  <div id="header-menus">
    <nav class="navbar navbar-expand-md navbar-light bg-light">
      <div id="container-viewport-switch" class="container-fluid">
        <div id="logo_desktop" class="navbar-brand">
            <a href="/Datafari/Search"><img class="datafari-mini-logo" src="<c:url value="/resources/images/empty-pixel.png" />"></a>
        </div>
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#header-menu" aria-controls="header-menu" aria-expanded="false" aria-label="Toggle navigation">
	        <span class="navbar-toggler-icon"></span>
	      </button>
        <div class="collapse navbar-collapse" id="header-menu">
          <ul class="navbar-nav ml-auto" id= "loginDatafariLinks">

            <!-- Show the localized language section -->
            <li class="nav-item" id="languageSelector"></li>
            <li class="nav-item"><a class="nav-link" id="basicSearchLink">Basic Search</a></li>
            <li class="nav-item dropdown">
              <a class="nav-link dropdown-toggle" data-target="#" href="#" role="button" id="dropdown-search-tools" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Search Tools <span class="caret"></span></a>
              <ul class="dropdown-menu header-item keep-down" id="search-tools-sub-menu" aria-labelledby="dropdown-search-tools">
                <%
                if (request.getUserPrincipal() != null) {
                %>
                  <li id="alertsDropdownMenu"><a class="dropdown-item" href="#"  id="userAlertsLink" >Alerts</a></li>
                  <li id="savedSearchDropdownMenu"><a class="dropdown-item" href="#" id="userSavedSearchLink" >Saved Search</a></li>
                <%
                }
                %>
                <li id="advancedSearchDropdownMenu"><a class="dropdown-item" href="#" id="advancedSearchLink" >Advanced Search</a></li>
                <%
                if (request.getUserPrincipal() != null) {
                %>
                  <li><a id="save_search_label" href="#" class="dropdown-item"  data-toggle="popover" data-placement="bottom" >Save Search</a></li>
                <%
                  if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES)!=null && DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES).equals("true") ){
                %>
                    <li id="favoritesDropdownMenu"><a class="dropdown-item" href="#" id="userFavoritesLink" >Favorites</a></li>
                <%
                  }
                }
                %>
              </ul>
            </li>

            <%
            if (request.getUserPrincipal() != null) {

              String[] adminRoles = {"SearchAdministrator", "SearchExpert"};
              for(String role : adminRoles) {
                if(request.isUserInRole(role)) {
            %>
              <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle" data-target="#" href="#"  role="button" id="dropdownAdminLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                  Admin <span class="caret"></span>
                </a>
                <ul class="dropdown-menu header-item" id="sub-menu-admin" aria-labelledby="dropdownAdminLink">
                  <li>
                    <a href="#" target="blank" href="#" class="dropdown-item" id="adminConsoleLink" ></a>
                  </li>
                  <li>
                    <a href="#" target="blank" href="#" class="dropdown-item" id="adminMCFLink" ></a>
                  </li>
                  <li id="relevancy-node">
                    <a id="create-relevancy" class="dropdown-item" data-toggle="popover" href="#" data-placement="bottom">
                      Créer une requête de pertinence
                    </a>
                  </li>    
                </ul>
              </li>
            <%
                  break;
                }
              }
            %>
<!--               <li class="nav-item dropdown"> -->
<!--                 <a class="nav-link dropdown-toggle" data-target="#" href="#"  role="button" id="dropdown-my-account" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"> -->
<!--                   My Account <span class="caret"></span> -->
<!--                 </a> -->
<!--                 <ul id="account-sub-menu-item" class="dropdown-menu header-item keep-down" aria-labelledby="dropdown-my-account"> -->
<!--                   <li id="externalSourcesDropdownMenu"><a class="dropdown-item" href="#" id="externalSourcesLink" >External Sources</a></li> -->
<!--                 </ul> -->
<!--               </li> -->
              <li class="nav-item"><a id="logout" class="nav-link" onclick="logout();"></a></li>
            <%
            }
            else {
            %>
              <li class="nav-item"><a id="loginLink" class="nav-link"></a></li>
            <%
            }
            %>
            <li class="nav-item"><a id="helpLink" class="nav-link" href="/Datafari/help">Help</a></li>
          </ul>
        </div>
      </div>
    </nav>
</div>

<div id="save-search-popover" style="display:none;"></div>
<div id="create-relevancy-popover" style="display:none;"></div>

</header>
