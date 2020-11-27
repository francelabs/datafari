<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<script src="<c:url value="/resources/js/admin/ajax/queryElevator.js" />"></script>
	<link href="<c:url value="/resources/css/admin/queryElevator.css" />" rel="stylesheet"></link>
	<meta charset="UTF-8">
</head>
<body>
<!-- Confirm modal -->
<div class="modal fade" id="queryElevatorModal" data-backdrop="static" tabindex="-1" role="dialog" aria-labelledby="queryElevatorModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="queryElevatorModalLabel">Warning</h5>
      </div>
      <div class="modal-body">
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" id="queryElevatorModal_cancel" data-dismiss="modal">Cancel</button>
        <button type="button" class="btn btn-primary" id="queryElevatorModal_confirm">OK</button>
      </div>
    </div>
  </div>
</div>

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
	<div id="modBox" class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="modifyElevateLabel"></span>
			</div>
			<div class="box-icons">
			</div>
			<div class="no-move"></div>
		</div>
		<div class="box-content">
			<div class="documentation-style-no-margin">
				<p class="documentation-preview"> <span id="documentation-queryelevator"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/22052870/Boosting+documents+from+the+admin+UI" target="_blank">...see more</a></p>
			</div>
      <div class="row">
        <label id="query-elevator-ui-desc" class="col-sm-4 col-form-label"></label>
        <div class="col-sm-1 col-form-label">
          <input type="checkbox" id="query-elevator_activation" name="query-elevator_activation" data-size="sm" data-toggle="toggle" data-onstyle="success">
        </div>
        <div style="Display: none" class="col-sm-6 feedback-message alert alert-danger" id="query-elevator-activation-message">L'outil visuel ne peut pas être activé lorsque le Search Aggregator est activé. Voir doc pour plus d'explications</div>
      </div>
			<form id="modifyElevateForm">
				<fieldset>
					<legend id="selectQuery"></legend>
					<div class="select-div">
						<select id="query" class="form-control">
							<OPTION></OPTION>
						</select>
					</div>
				</fieldset>
				
				<div class="box-content" id="docs">
					<fieldset>
						<legend id="modifyDocsOderLabel"></legend>
						<div class="box">
							<div class="box-header">
								<div class="box-name"><i class="fas fa-table"></i><span id="elevatorDocsListLabel"></span></div>
								<div class="box-icons">
									<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
									<a class="expand-link"><i class="fas fa-expand"></i></a>
								</div>
							</div>
							<div id="boxCon" class="box-content no-padding">
								<table id="docsTable" class="table table-striped table-bordered table-hover table-heading no-border-bottom">
									<thead>
										<tr>
											<th>Id</th>
											<th>Position</th>
											<th></th>
										</tr>
									</thead>
									<tbody id="docsTableContent">
									</tbody>
								</table>
							</div>
						</div>
						
						<div>
							<div class="row">
									<label class="confirmElevateConf col-sm-3 col-form-label" for=""></label>
                  <div class="col-sm-2 col-form-label">
                    <button class="btn btn-primary btn-label-left" name="saveElevate" id="saveElevateConf" data-toggle="tooltip" data-placement="right" title=""></button>
                  </div>
							</div>
							<div class="row">
								<label id="deleteElevateConfButton" class="col-sm-3 col-form-label" for=""></label>
                <div class="col-sm-3 col-form-label">
								  <button class="btn btn-primary btn-label-left" name="deleteElevate" id="deleteElevateConf" data-toggle="tooltip" data-placement="right" title=""></button>
                </div>
							</div>
              <div class="col p-0">
                <span class="message" id="message"></span><br />
                <span class="message" id="messageZK"></span>
              </div>
						</div>
					</fieldset>
				</div>
			</form>
		</div>
	</div>
	
	<div id="creBox" class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="createElevateLabel"></span>
			</div>
			<div class="box-icons">
			</div>
			<div class="no-move"></div>
		</div>
		<div class="box-content">
			<form id="createElevateForm">	
				<div>
					<fieldset>
						<div class="box">
							<div class="box-content no-padding">
								<table class="table table-striped table-bordered table-hover table-heading no-border-bottom">
									<thead>
										<tr>
											<th id="queryThLabel"></th>
											<th>Documents IDs</th>
											<th></th>
										</tr>
									</thead>
									<tbody id="createTbody">
										<tr>
											<td><input type="text" class="textInput" id="queryInput"/></td>
											<td><input type="text" class="textInput docInput"/></td>
											<td><i class="fas fa-plus" id="addDocButton"></i></td>
										</tr>
									</tbody>
								</table>
							</div>
						</div>
						
						<div class="row">
							<label class="confirmElevateConf col-sm-3 col-form-label" for=""></label>
              <div class="col-sm-2 col-form-label">
							 <button class="btn btn-primary btn-label-left" name="saveNew" id="saveNewElevate"></button>
              </div>
						</div>
            <div class="col p-0">
              <span class="message" id="message2"></span><br />
              <span class="message" id="messageZK2"></span>
            </div>
					</fieldset>
				</div>
			</form>
		</div>
	</div>
</html>