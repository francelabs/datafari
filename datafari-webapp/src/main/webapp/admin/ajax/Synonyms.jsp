<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<script src="<c:url value="/resources/js/admin/ajax/synonyms.js" />"></script>
	<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/synonyms.css" />" />
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
				<i class="fas fa-table"></i><span id="synonymsBox"></span>
			</div>
			<div class="box-icons">
			</div>
			<div class="no-move"></div>
		</div>
		<div id="modBox" class="box-content">
			<div class="documentation-style-no-margin">
				<p class="documentation-preview"> <span id="documentation-synonyms"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/6324239/Synonyms+Configuration" target="_blank"> ...see more</a></p>
			</div>
			<form>
				<fieldset>
					<legend id="Modify"></legend>
					<div class="col-sm-2">
						<select id="language" class="form-control"
							onchange="javascript: getFile()">
							<OPTION></OPTION>
							<OPTION>fr</OPTION>
							<OPTION>en</OPTION>
							<OPTION>de</OPTION>
							<OPTION>es</OPTION>
						</select>
					</div>
				</fieldset>
			</form>

			<div id="anotherSection">
				<fieldset>
					<div id="legendDiv" style="margin-top: 10px;"></div>
					<div id="synonymsDisplay">
						<div class="box">
							<div class="box-header">
								<div class="box-name"><i class="fas fa-table"></i><span id="synonymsListLabel"></span></div>
								<div class="box-icons">
									<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
									<a class="expand-link"><i class="fas fa-expand"></i></a>
								</div>
							</div>
							<div id="boxSynonyms" class="box-content no-padding">
								<table id="synonymsTable" class="table table-striped table-bordered table-hover table-heading no-border-bottom">
									<thead>
										<tr>
											<th id="thWords"></th>
											<th id="thSynonyms"></th>
											<th id="thDelete"></th>
										</tr>
									</thead>
									<tbody id="synonymsTableContent">
									</tbody>
								</table>
							</div>
						</div>
						<div>
							<div class="alert alert-warning" id="reload-warning">Validating uploads the config to zookeeper and refreshes the core immediately.
							This is not recommended while the server is heaviely loaded (indexation time for example) and may take more than 
							a minute if your core contains millions of entries. The search service responses will be slower than usual while 
							the core is reloading.</div>
							<div class='row'>
								<label class='col-sm-3 col-form-label' id="validateModification" for=""></label>
                <div class='col-sm-3 col-form-label'>
                  <button class='btn btn-primary btn-label-left' name='validateSynonyms' id='validate'></button>
                </div>
							</div>
						</div>
					</div>
					<div id="ajaxResponse" style="margin-top: 10px;"></div>
				</fieldset>
			</div>
		</div>
	</div>
</html>