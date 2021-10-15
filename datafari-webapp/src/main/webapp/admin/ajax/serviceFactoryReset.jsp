<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<script src="<c:url value="/resources/js/admin/ajax/serviceFactoryReset.js" />"></script>
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
					<i class="fas fa-table"></i><span id="factoryResetMenu"></span>
				</div>
				<div class="box-icons">
				</div>
				<div class="no-move"></div>
			</div>
			<div class="documentation-style">
				<p class="documentation-preview"> <span id="documentation-servicefactoryreset"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2599682049/Factory+reset+of+Datafari" target="_blank">...see more</a></p>
			</div>			
			<div class="box-content">
				<span id="factoryResetLabel"></span>
				<span id="factoryResetLink"><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2599682049/Factory+reset+of+Datafari" target="_blank">https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2599682049/Factory+reset+of+Datafari</a></span>		
							
			</div>							
		</html>