<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<script src="<c:url value="/resources/js/admin/ajax/atomicUpdate.js" />"></script>
	<meta charset="UTF-8">
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

	<div id="modBox1" class="box">
		<div class="box-header">
				<div class="box-name">
					<i class="fas fa-table"></i><span id="atomicUpdateTitle"></span>
				</div>
				<div class="box-icons">
				</div>
				<div class="no-move"></div>
			</div>
			<div class="documentation-style">
				<p class="documentation-preview"> <span id="documentation-atomicUpdate"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2939387906/Atomic+Update+Management" target="_blank">...see more</a></p>
			</div>
			<div class="box-content">
			  <p>
          <span id="atomicUpdate-doc-label"></span>
          <span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2939387906/Atomic+Update+Management" target="_blank">https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2939387906/Atomic+Update+Management</a></span>
        </p>
        <p>
          <span><a id="atomicUpdate-log-link" class="ajax-link" href="?page=downloadLogs"></a></span>
        </p>
			</div>
		</html>