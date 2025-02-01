<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script type ="text/javascript" src ="<c:url value="/resources/js/footer.js" />"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/footer.css" />" media="screen">
<footer  class="bar-footer" id="footer-bar"> 
   <img id="footer-logo" class="datafari-logo" src="<c:url value="/resources/images/empty-pixel.png" />">
   <a  id="footer-link" href="mailto:support@francelabs.com" class="link"> Datafari @VERSION@ &copy; Copyright France Labs</a>
   <a id="gdpr-link" href="/@WEBAPPNAME@/privacypolicy" class="link">privacy policy</a>
   <div id="warning-message"></div>
</footer>
