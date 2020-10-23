<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/config_zookeeper.js" />" type="text/javascript"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/config_zookeeper.css" />" />
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
	<div class="col-sm-12"><span id="globalAnswer"></span></div>
	<div class="col-sm-12"></div>
	<div class="documentation-style">
		<p class="documentation-preview"> <span id="documentation-configzookeeper"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/34603014/System+Configuration+Manager+Zookeeper" target="_blank"> ...see more</a></p>
	</div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="upldZKconf"></span>
			</div>
			<div class="box-icons">
			<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>				
		<div id="upldZKconfBox" class="box-content">			
			<form class="form-horizontal" role="form">
				<div class="row" >
					<div class="col-sm-6">
						<span id="upldZKconfLabel" class="col-form-label"></span>
					</div>
					<div class="col-sm-3 col-form-label">
						<button id="upload" name="upload" class="btn btn-primary btn-label-left"></button>
					</div>
				</div>
        <div class="col p-0 r-col"><span id="uploadResult"></span></div>
			</form>
		</div>
	</div>
	<div class="col-sm-12"></div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="rldZKconf"></span>
			</div>
			<div class="box-icons">
			<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div id="rldZKconfBox" class="box-content">
			<form class="form-horizontal" role="form">
				<div class="row" >
					<div class="col-sm-6">
						<span id="rldZKconfLabel" class="col-form-label"></span>
					</div>
					<div class="col-sm-3 col-form-label">
						<button id="reload" name="reload" class="btn btn-primary btn-label-left"></button>
					</div>
				</div>
        <div class="col p-0 r-col"><span id="reloadResult"></span></div>
			</form>
		</div>
	</div>
	<div class="col-sm-12"></div>
  <div class="box">
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span  id="downZKconf"></span>
      </div>
      <div class="box-icons">
      <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div id="downZKconfBox" class="box-content">
      <form class="form-horizontal" role="form">
        <div class="row" >
          <div class="col-sm-6">
            <span id="downZKconfLabel" class="col-form-label"></span>
          </div>
          <div class="col-sm-3 col-form-label">
            <button id="download" name="download" class="btn btn-primary btn-label-left"></button>
          </div>
        </div>
        <div class="col p-0 r-col"><span id="downloadResult"></span></div>
      </form>
    </div>
  </div>
</body>