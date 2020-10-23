<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<script src="<c:url value="/resources/js/admin/ajax/MCFChangePassword.js" />"></script>
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
					<i class="fas fa-table"></i><span id="changePasswordLabel"></span>
				</div>
				<div class="box-icons">
				</div>
				<div class="no-move"></div>
			</div>
			<div class="documentation-style">
				<p class="documentation-preview"> <span id="documentation-mcfchangepassword"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/116228306/MCF+Change+Password" target="_blank">...see more</a></p>
			</div>			
			<div class="box-content">
				<form id="modifyElevateForm">
					<fieldset>
						<legend id="selectPassword"></legend>
						<div>							
							<span id="enterPasswordLabel"></span><br /><input class="textInput col-sm-3" type="password" id="password" onkeyup='checkPassword();'/><br /> 
							<span id="confirmPasswordLabel"></span><br /><input class="textInput col-sm-3" type="password" id="confirm_password" onkeyup='checkPassword();' />
							<span id="messagePassword"></span><br />
						</div>
						<div>
						</div>
					</fieldset>
				</form>
				<br>
				<div class="row">
					<div class="col-sm-3">
						<label id="MCFConfirm" for=""></label>
					</div>
					<button id="savePasswordButton" name="savePasswordButton" class="btn btn-primary btn-label-left col-sm-2"></button>
					<div><span id="message"></span></div>
				</div>	
			</div>							
		</html>