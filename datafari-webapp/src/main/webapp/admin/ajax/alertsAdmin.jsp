<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>

<!-- Page specific JS -->
<script src="<c:url value="/resources/js/admin/ajax/alertsAdmin.js" />" type="text/javascript"></script>

<link href="<c:url value="/resources/css/admin/alertsAdmin.css" />" rel="stylesheet"></link>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body >
	<nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
  
	<div class="box no-padding col-sm" id="box" >
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="boxname"></span>
			</div>
			<div class="box-icons pull-right">
			</div>
			<div class="no-move"></div>
		</div>		
			<div class="box-content" id="thBox">
				<div class="documentation-style-no-margin">
				<p class="documentation-preview"> <span id="documentation-alerstadmin"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/8192039/Alerts+management+-+Mail+Configuration" target="_blank"> ...see more</a></p>
			</div>
			<form class="form-horizontal" id="form" role="form">
				<div id="errorPrint"></div>
				<fieldset >
				<legend id="delayLegend"></legend>
				<div class="row">
					<label id=HourlyLabel class="col-sm-3 col-form-label"></label>
					<input type="text" class="col-sm-2" id="HourlyDelay"  style="min-width : 150px;">
					<div class="col-sm-1"></div>
					<div class="form-group col-sm-6" id="hint1">
						<label  class="control-label" id="labelHint1"></label>
					</div>				
				</div>
				<div class="row">
					<label id=DailyLabel class="col-sm-3 col-form-label"></label>
					<input type="text" class="col-sm-2" id="DailyDelay" style="min-width : 150px;">
				</div>
				<div class="row">
					<label id=WeeklyLabel class="col-sm-3 col-form-label"></label>
					<input type="text" class="col-sm-2" id="WeeklyDelay"  style="min-width : 150px;">
				</div>
				</fieldset>
				<div id="prevNext" class="form-group">
				</div>				
				<fieldset >
					<legend id="connLegend"></legend>
					<div class="row">
						<label id="HostLabel" class="col-sm-3 col-form-label"></label>
						<input type="text" id="Host" name="Host" class="col-sm-2"  style="min-width : 150px;"/>
						<div class="col-sm-1"></div>
						<div class="col-sm-6 form-group" id="hint2">
							<label class="control-label" id="labelHint2"></label>
						</div>
					</div>
					<div class="row">
						<label id="PortLabel" class="col-sm-3 col-form-label"></label>
						<input type="text" id="Port" name="Port" class="col-sm-2"  style="min-width : 150px;"/>
					</div>
					<div class="row">
						<label id="DatabaseLabel" class="col-sm-3 col-form-label"></label>
						<input type="text" id="Database" name="Database" class="col-sm-2"  style="min-width : 150px;"/>
					</div>
					<div class="row">
						<label id="CollectionLabel" class="col-sm-3 col-form-label"></label>
						<input type="text" id="Collection" name="Collection" class="col-sm-2"  style="min-width : 150px;"/>
					</div>
					</fieldset>
					<div id="mailForm" class="form-group">
					</div>
					<div id="align-buttons" class="form-group">
						<div class="row" id="divParam">
						<div class="col-sm-3">
							<label id="paramRegLabel" for="" class="col-form-label"></label>
						</div>
						<div class="col-sm-3 col-form-label">
						  <button type="button" class="btn btn-primary btn-label-center"
                id="paramReg" onclick="javascript : parameters()">
                <span><i class="fas fa-clock-o" id="paramRegtext"></i></span>
              </button>
						</div>
							
							<label id="parameterSaved" class="col-form-label"></label>
						</div>
						<div class="form-group">
						<div id="switchAlertsAdmin" class="row">
						<div class="col-sm-3">
							<label id="paramRegEmail" for="" class="col-form-label"></label>
						</div>
						<div class="col-sm-5 col-form-label">
						  <input type="checkbox" id="activated" onchange="Javascript : onOff()" name="activated" data-size="sm" data-toggle="toggle" data-onstyle="success" data-offstyle="danger">
						</div>						
						</div>
					</div>	
					</div>
			</form>		
	</div>
	<!-- <div class="col-sm-4" id="hints" > -->	
	</div>
</body>