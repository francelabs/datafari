<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8" />
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
		<title>Search</title>
		<link rel="icon" type="image/png" href="<c:url value="/resources/images/bullet.png" />"/>
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/bootstrap/4.3.1/css/bootstrap.min.css" />"/>
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.min.css" />">
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.theme.min.css" />">
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.structure.min.css" />">
		<link rel ="stylesheet" type ="text/css" href ="<c:url value="/resources/css/animate.min.css" />" />
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/libs/font-awesome/5.11.2/css/all.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/main.css" />" />
		<link rel ="stylesheet" type ="text/css" href ="<c:url value="/resources/css/results.css" />" />
		<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/custom/new-style.css" />" media="screen" />
    <link rel ="stylesheet" type ="text/css" href ="<c:url value="/resources/css/index.css" />" />
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/css/index-header.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/searchView-status-bar.css" />" />
	</head>
	<body class="gecko win">

	  <script type ="text/javascript" src ="<c:url value="/resources/libs/jquery/3.4.1/jquery-3.4.1.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/popper/1.16.0/popper.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/bootstrap/4.3.1/js/bootstrap.min.js" />"></script>
    <script type="text/javascript" src ="<c:url value="/resources/libs/jquery-ui/1.12.1/jquery-ui.min.js" />"></script>
		<script type="text/javascript" src="<c:url value="/resources/libs/lucene-parser/lucene_parser.js" />"></script>
    <script type ="text/javascript" src ="<c:url value="/resources/js/polyfill.js" />"></script>
    <script type ="text/javascript" src ="<c:url value="/resources/js/function/empty.func.js" />"></script>

    <script type="text/javascript" src="<c:url value="/resources/js/searchSessionTimeout.js" />"></script>
		<script type ="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/Core.js" />"></script>
		<script type ="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractModule.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractWidget.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/core/Parameter.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/core/ParameterStore.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/i18njs.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/uuid.core.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractManager.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/manager/Manager.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/modules/Pager.module.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/modules/Autocomplete.module.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/widgets/SearchBar.widget.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/widgets/AdvancedSearch.widget.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/widgets/AdvancedSearchWidget/AdvancedSearchField.js" />"></script>


    <script type ="text/javascript" src ="<c:url value="/resources/customs/js/customFieldsForResults.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/main.js" />"></script>
		<script type ="text/javascript" src ="<c:url value="/resources/js/searchBar.js" />"></script>


	<jsp:include page="header.jsp" />
	<div id="content_index">
		<img class="fadeInDown animated datafari-logo" id="logo-search" src="<c:url value="/resources/images/empty-pixel.png" />"/>
<!-- start main section -->
		<div id="solr">
			<div id="searchBar"></div>
			<div class="clear"></div>
		</div>
<!-- end main section -->			
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
