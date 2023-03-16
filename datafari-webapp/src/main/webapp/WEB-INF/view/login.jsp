<%@page import="org.springframework.security.web.savedrequest.DefaultSavedRequest"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="java.util.regex.Matcher"%>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<html>
  <head>
    <meta charset="utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <title>Login</title>
    <link rel="icon" type="image/png" href="<c:url value="/resources/images/bullet.png" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/reset.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/login.css" />">
    <link href="<c:url value="/resources/libs/bootstrap/4.3.1/css/bootstrap.min.css" />" rel="stylesheet">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.theme.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.structure.min.css" />">
    <link href="<c:url value="/resources/css/style_v2.css" />" rel="stylesheet">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/widgets/login-datafari-form-widget.css" />">
    
    <!-- Usefull for the timeout script -->
    <script type="text/javascript">var sessionTimeout = ${pageContext.session.maxInactiveInterval};</script>
    
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery/3.4.1/jquery-3.4.1.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/popper/1.16.0/popper.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/bootstrap/4.3.1/js/bootstrap.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery-ui/1.12.1/jquery-ui.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/function/empty.func.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/polyfill.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/Core.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractModule.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractWidget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/Parameter.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/ParameterStore.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractManager.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/manager/Manager.js" />"></script>
    <script type ="text/javascript" src ="<c:url value="/resources/customs/js/customFieldsForResults.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/main.js" />"></script>
    <!-- JS library useful to extract parameters value from URL  -->
    <script type ="text/javascript" src ="<c:url value="/resources/js/url.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/i18njs.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/LanguageSelector.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/LoginDatafariForm.widget.js" />"></script>
    
    <!-- Timeout script -->
    <script type="text/javascript" src="<c:url value="/resources/js/loginSessionTimeout.js" />"></script>
  </head>
  <body>
   <%
   String langParam = null;
   
   // Try to retrieve the original request that has been redirected to the login page
   String originalRequest = null;
   Object savedRequestObj = session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
   if(savedRequestObj != null && savedRequestObj instanceof DefaultSavedRequest) {
     originalRequest = ((DefaultSavedRequest) savedRequestObj).getRequestURL();
   }
   
   // If the language parameter is defined take it, othwerwise use the referrer in the message header
   String lang = request.getParameter("lang");
   
   if (lang != null){
     
     langParam = "?lang=" + lang;
     
   } else {
     
     String referer = request.getHeader("referer");
     
     // If referer is defined and contains language parameter
     if (referer != null && referer.length() != 0 && referer.contains("lang=")){
       
       Matcher langMatcher = Pattern.compile("lang=\\w{2}").matcher(referer);
       
       // If matcher has found the lang parameter, extract it
       if (langMatcher.find()){
         langParam = '?' + langMatcher.group();
       }     
     }
   }
   final String mainPage = request.getContextPath();
   String loginPage = mainPage + "/login";
   String redirect = request.getParameter("redirect");
   // If there is no explicit redirect but there is an original request, then set the original request as redirect
   if(redirect == null && originalRequest != null) {
     redirect = originalRequest;
   }
   if(redirect != null) {
     redirect = URLEncoder.encode(redirect, "UTF-8").replace("+", "%20");
     loginPage += "?redirect=" + redirect;
   } else {
   
     // If the language param was defined in the source URL, append the language 
     // selection to the adminUi login page URL to be able to display it in the correct language
     if (langParam != null){
       loginPage = loginPage + langParam;
     }
   }
   %>
   
   <form id="loginDatafariForm" name='loginDatafariForm' class="box login" action="<%=loginPage%>" accept-charset='utf-8' method='POST'>
      <fieldset class="boxBody">
        <label id="loginFormAdminUiLabel"></label>
         
        <label id="loginAdminUiLabel"></label> 
        <input type="text" tabindex="1" name="username" required>
  
        <label id="passwordAdminUiLabel"></label>
        <input type="password" tabindex="2" required name="password">
      </fieldset>
      
      <div class="row m-0">
        <div class="col-sm-9">
          <c:if test="${not empty errorType}">
          
            <span class="invalid">
              <c:if test="${errorType == 'credentials'}">
                <label id="invalidLoginAdminUiLabel"></label>
              </c:if>
              <c:if test="${errorType == 'session'}">
                <label id="expiredSessionLabel"></label>
              </c:if>
              <c:if test="${errorType == 'logout'}">
                <label id="loggedOutLabel"></label>
              </c:if>
            </span>
            
          </c:if>
        </div>
        <div class="col-sm-1">
          <input type="submit" id="loginAdminUiBtn" class="btn btn-primary" value="Log in" tabindex="4">
        </div>
      </div>
      
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  </form>
</body>
</html>