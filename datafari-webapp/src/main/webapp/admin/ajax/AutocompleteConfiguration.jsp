<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
	<head>
		<script src="<c:url value="/resources/js/admin/ajax/autocompleteConfiguration.js" />"></script>
		<meta charset="UTF-8">
		<title>Insert title here</title>
	</head>
<body>
	<nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
  
	<div class="col-sm-12"><span id="globalAnswer"></span></div>	
	<!-- <br/> -->
	<div class="col-sm-12"></div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="thname"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div class="documentation-style">
			<p class="documentation-preview"> <span id="documentation-autocompleteconfiguration"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/8192094/Autocomplete+configuration" target="_blank"> ...see more</a></p>
		</div>
		<div id="thBox" class="box-content">
			<form class="form-horizontal" role="form" novalidate>
				<div class="row">					
					<label id="labelth" class="col-sm-3 col-form-label"></label>
					<div class="col-sm-3">
						<input type="number" step="0.001" min="0" max="1" id="autocompleteThreshold" name="autocompleteThreshold" class="form-control">
						<div class="invalid-feedback">
              Invalid value. Please refer to the tooltip for more details
            </div>						
					</div>					
				</div>
				<div class="row">
					<label id="autocompleteConfirm" for="" class="col-sm-3 col-form-label"></label>
					<div class="col-sm-3 col-form-label">
					 <button id="submitth" name="submitth" class="btn btn-primary btn-label-left"></button>
					</div>
				</div>
				<div class="col p-0"><span id="answerth"></span></div>
			</form>
		</div>
	</div>
</body>
</html>