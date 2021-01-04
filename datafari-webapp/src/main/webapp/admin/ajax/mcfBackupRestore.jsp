<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<!-- Page specific JS -->
<script src="<c:url value="/resources/js/admin/ajax/mcfBackupRestore.js" />" type="text/javascript"></script>
<!-- Page specific CSS -->
<link href="<c:url value="/resources/css/admin/mcfBackupRestore.css" />" rel="stylesheet"></link>
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
		
	<div class="box">
	
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="box-title"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>		
		
		<div class="box-content" id="thBox">
			<div class="documentation-style-no-margin">
			<p class="documentation-preview"> <span id="documentation-mcfbackuprestore"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/506789890/MCF+Backup+and+Restore+Configuration" target="_blank">...see more</a></p>
		</div>
			<div class="row">
				<label for="backupDir-input" id="backupDir-label" class="col-sm-3 control-label"></label>
				<input type="text" id="backupDir-input" class="col-sm-5"/>
				<label id="backupDir-label-default" class="col-sm-8"></label>
			</div>
			<br>
			<div class="row">
				<div class="col-sm-3">
					<label id="" for="">Save the connectors</label>
				</div>
				<button type="button" class="backupRestore-btn btn btn-primary btn-label-left"
					id="doSave-btn" onclick="javascript : doSave()">
				</button>				
			</div>
			<div class="row">
				<div class="col-sm-3">
					<label id="" for="">Restore the connectors</label>
				</div>
				<button type="button" class="backupRestore-btn btn btn-primary btn-label-right"
					id="doRestore-btn" onclick="javascript : doRestore()">
				</button>
			</div>
			<br>
			<div class="row">
				<label class="col-sm-4 control-label" id="doRestoreReturnStatus-label"></label>
			</div>							
		</div>
	</div>
</body>
</html>