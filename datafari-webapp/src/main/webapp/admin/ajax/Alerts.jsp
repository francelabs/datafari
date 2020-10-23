<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/alerts.js" />"></script>
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
	<div class="row">
	
		<div class="col-xl-8 col-sm-4">
			<div id="search">
				<input type="text" id="searchBar" style="background-color: #FFFFFF;" onchange="javascript : getTab()"/>
				 <i style="color : #000000; top : 10px;"class="text-center fa fa-search" onclick="javascript : getTab()"></i>
			</div>
		</div>
	</div>
	
	
	<div id="anotherSection">
	<fieldset id="alphafeature"></fieldset>
		<fieldset>
			<legend id="contextMenu"></legend>
			<div id="ajaxResponse" class="form-group" role="form" style="min-height : 50px;"></div>
		</fieldset>
	</div>
	<div id="addAlerts">
		<fieldset>
			<legend id="legendAdd"></legend>
			<div id="addAlertsForm"></div>
		</fieldset>
	</div>
</body>
</html>