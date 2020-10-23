<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <script src="<c:url value="/resources/js/admin/ajax/helpeditor.js" />"></script>
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
	<!-- TODO get the list of language in the list_language.txt  -->
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span></span>
			</div>
			<div class="box-icons">
			</div>
			<div class="no-move"></div>
		</div>
		<div id="modBox" class="box-content">
			<div id="editor">
				<fieldset>
					<div id="ajaxResponse" style="margin-top: 10px;"></div>
				</fieldset>
			</div>
		</div>
	</div>
</body>
</html>