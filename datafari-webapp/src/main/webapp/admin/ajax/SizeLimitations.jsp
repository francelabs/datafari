<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/sizeLimitations.js" />"></script>
<meta charset="UTF-8">
<title>Size Limitations</title>
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
				<i class="fas fa-table"></i><span  id="hlname"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div class="documentation-style">
			<p class="documentation-preview"> <span id="documentation-sizelimitations"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/8192102/Highlighting+configuration" target="_blank">  ...see more</a></p>
		</div>
		<div id="hlBox" class="box-content">
			<form class="form-horizontal" role="form">
				<div class="row">
				  <label id="labelhl" class="col-sm-3 col-form-label"></label>					
					<div class="col-sm-3">
						<input type="number" min="0" id="maxhl" name="maxhl" class="form-control" >
						<div class="invalid-feedback">
              Invalid value. Please refer to the tooltip for more details
            </div>			
					</div>					
				</div>
				<div class="row">
					<label id="size-limitations-confirm" class="col-sm-3 col-form-label"></label>
					<div class="col-sm-2 col-form-label">
					 <button id="submithl" name="submithl" class="btn btn-primary btn-label-left"></button>
					</div>
				</div>
				<div class="col p-0"><span id="answerhl"></span></div>
				
			</form>
		</div>
	</div>
</body>
</html>