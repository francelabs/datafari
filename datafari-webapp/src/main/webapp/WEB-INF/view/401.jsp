<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page session="false"%>
<html>
  <head>
    <title>Forbidden! (401)</title>
    <link rel="icon" type="image/png" href="<c:url value="/resources/images/bullet.png" />"/>
    <link rel ="stylesheet" type ="text/css" href ="<c:url value="/resources/css/errors/style.css" />" />
  </head>
  
  <body>
    <div class="header">
      <a href="/@WEBAPPNAME@"><img src="<c:url value="/resources/images/datafari_small_text_logo.png" />"></a>
    </div>
    <img class="sammy" src="<c:url value="/resources/images/errors/Datafari_401_Backrground_Picture.jpg" />"></img> 
    <div class="waves"></div>
    <div class="oops-copy">
      <h1>Access denied ! You must be authenticated to access this page.</h1>
      <p class="sub-copy">Head back to the <a href="./Datafari">search page</a>.</p>
    </div>
  
  </body>
</html>