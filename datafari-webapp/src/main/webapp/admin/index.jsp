<%@page import="org.keycloak.KeycloakPrincipal"%>
<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="java.util.Locale" %>

<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<title>Admin Datafari</title>
		<meta name="description" content="description">
		<meta name="author" content="Admin Datafari">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-param" content="${_csrf.parameterName}">
    <meta name="csrf-token" content="${_csrf.token}">
    
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/bootstrap/4.3.1/css/bootstrap.min.css" />"/>
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.min.css" />">
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.theme.min.css" />">
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.structure.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/bootstrap-toggle/3.6.1/css/bootstrap4-toggle.min.css" />"/>
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/font-awesome/5.11.2/css/all.css" />">
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/css/google-fonts/righteous.css" />">
    <link rel="stylesheet" href="<c:url value="/resources/libs/jquery-CLEditor/1.4.5/jquery.cleditor.css" />">
    <Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
    
    <link href="<c:url value="/resources/css/style_v2.css" />" rel="stylesheet">
		<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/admin-index.css" />" media="screen" />

<!-- 		<link href="../plugins/fullcalendar/fullcalendar.css" rel="stylesheet"> -->
<!-- 		<link href="../plugins/xcharts/xcharts.min.css" rel="stylesheet"> -->
<!-- 		<link href="../plugins/select2/select2.css" rel="stylesheet"> -->
<!-- 		<link href="../plugins/justified-gallery/justifiedGallery.css" rel="stylesheet"> -->
<!-- 		<link href="../plugins/chartist/chartist.min.css" rel="stylesheet"> -->
    
    
    <script type ="text/javascript" src ="<c:url value="/resources/libs/jquery/3.4.1/jquery-3.4.1.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/popper/1.16.0/popper.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/bootstrap/4.3.1/js/bootstrap.min.js" />"></script>
    <script type="text/javascript" src ="<c:url value="/resources/libs/jquery-ui/1.12.1/jquery-ui.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/bootstrap-toggle/3.6.1/js/bootstrap4-toggle.min.js" />"></script>
    <script type ="text/javascript" src ="<c:url value="/resources/js/function/empty.func.js" />"></script>
    <script type ="text/javascript" src ="<c:url value="/resources/js/polyfill.js" />"></script>
    <script src="<c:url value="/resources/libs/jquery-ui-timepicker-addon/jquery-ui-timepicker-addon.js" />"></script>
    
		<script src="<c:url value="/resources/libs/jquery-CLEditor/1.4.5/jquery.cleditor.min.js" />"></script>
		<script type="text/javascript" src="<c:url value="/resources/js/logout.js" />"></script>
		<script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/i18njs.js" />"></script>
		<script src="<c:url value="/resources/js/admin/i18nInit.js" />" ></script>
		<!-- JS library useful to extract parameters value from URL  -->
		<script type ="text/javascript" src ="<c:url value="/resources/js/url.min.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/admin/i18nAdminIndex.js" />"></script>
		<script type="text/javascript" src="<c:url value="/resources/js/admin/sessionTimeout.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/admin/admin.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/admin/admin-sidebar.js" />"></script>
    
    <!--    <script src="../plugins/justified-gallery/jquery.justifiedGallery.min.js"></script> -->
    <!-- All functions for this theme + document.ready processing -->
<!--    <script src="../js/devoops.js"></script> -->
	</head>
<body>
<jsp:include page="/resources/modals/infoModal.jsp" />
<jsp:include page="admin-header.jsp" />

<!--Start Container-->
<div class="wrapper calc-height">
  <jsp:include page="admin-sidebar.jsp" />
  <div id="content-info-header" class="bg-light"></div>
	<!--Start Content-->
	<div id="content" class="calc-height">
	</div>
</div>
<jsp:include page="./admin-footer.jsp"  />
<!--End Container-->
</body>
</html>
