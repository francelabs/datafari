<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>

<script src="<c:url value="/resources/js/admin/ajax/indexField.js" />"></script>

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
	<div class="col-sm-12"><span id="globalAnswer"></span></div><br/>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="tablename"></span>
			</div>
			<div class="box-icons">
			<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div id="tableBox" class="box-content">
			<form class="form-horizontal" role="form">
			<table class="table table-striped table-bordered table-hover table-heading no-border-bottom">
			<thead id="thead"><tr><th id="name"></th><th id="type"></th><th id="indexed"></th><th id="stored"></th><th id="required"></th><th id="multivalued"></th></tr></thead>
			<tbody id="tbody"></tbody>
			</table>
			</form>
		</div>
	</div>
</body>