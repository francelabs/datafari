<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<%@ page import="java.util.ArrayList"%>
<link rel="stylesheet" type ="text/css" href="css/preview/preview-header.css">
<!-- Logout.js used by admin/index.jsp as well -->
<script type="text/javascript"
    src="js/AjaxFranceLabs/widgets/LanguageSelector.widget.js"></script>
<script type="text/javascript">
  var langHeader = new Array();
    <%
    for (int i=0 ; i<LanguageUtils.availableLanguages.size() ; i++) { %>
      langHeader[<%= i %>] = "<%= LanguageUtils.availableLanguages.get(i) %>";
    <% } %>
    var portHeader = <%= request.getServerPort()%>;
</script>
<script type="text/javascript"
    src="js/AjaxFranceLabs/widgets/HeaderMenus.widget.js"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="js/url.min.js"></script>
<header id="header-menus">
  <div>
    <nav class=" navbar navbar-default">
      <div class="container-fluid">
<!--         <div class="navbar-header"> -->
<!--           <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#header-menu" aria-expanded="false"> -->
<!--             <span class="sr-only">Toggle navigation</span> -->
<!--             <span class="icon-bar"></span> -->
<!--             <span class="icon-bar"></span> -->
<!--             <span class="icon-bar"></span> -->
<!--           </button> -->
<!--           <a class="navbar-brand" href="#"></a> -->
<!--         </div> -->

        <div class="collapse navbar-collapse" id="header-menu">
        
          <div id="logo_desktop">
	          <img class="datafari-mini-logo" src="images/empty-pixel.png">
	        </div>

          <ul class="nav navbar-nav navbar-right" id= "loginDatafariLinks">

            <!-- Show the localized language section -->
            <li id="languageSelector"></li>
            <% 
            if (request.getUserPrincipal() != null) { 
            %>
              <li><a id="logout" class="" onclick="logout();"></a></li>
            <%
            } else {
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
