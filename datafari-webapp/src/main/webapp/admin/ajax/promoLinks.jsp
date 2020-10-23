<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html >
<head>
<script src="<c:url value="/resources/js/admin/ajax/promoLinks.js" />" type="text/javascript"></script>
<link href="<c:url value="/resources/css/admin/promolinks.css" />" rel="stylesheet"></link>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<!--Start Breadcrumb-->
	<nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
	<!--End Breadcrumb-->
	<div id="creBox" class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="promoLinksBox"></span>
			</div>
			<div class="box-icons">
			</div>
			<div class="no-move"></div>
		</div>	
	<div class="documentation-style">
		<p class="documentation-preview"> <span id="documentation-promolinks"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/491542/Promolinks+Configuration" target="_blank"> ...see more</a></p>
	</div>
	<div id="promolinkSearchBar" class="row">
		<div class="col-xl-8 col-sm-4">
			<div id="search">
				<input type="text" id="searchBar" style="background-color: #FFFFFF;" onchange="javascript : getTab()"/>
				 <i style="color : #000000; top : 10px;"class="text-center fa fa-search" onclick="javascript : getTab()"></i>
			</div>
		</div>
	</div>
	<div id="anotherSection">
		<fieldset>
			<legend id="contextMenu"></legend>
			<div id="ajaxResponse" class="form-group" role="form" ></div>
		</fieldset>
	</div>
	<div id="addProm">
		<fieldset>
			<legend id="legendAdd"></legend>
			<div id="addPromFormBis"></div>
			<div id="addPromForm"></div>
		</fieldset>
	</div>
	</div>
</body>
</html>