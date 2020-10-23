<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>Add Users</title>
	<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/addUser.css" />" />
	<Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
</head>
<script src="<c:url value="/resources/js/admin/ajax/addUser.js" />" type="text/javascript"></script>
<body>
	<nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>

	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="title">Add Users</span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div id="thBox" class="box-content">
			<div class="documentation-style-no-margin">
				<p class="documentation-preview"> <span id="documentation-adduser"></span> <a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/49709061/Managing+Datafari+Roles+and+Users" target="_blank"> ...see more</a></p>
			</div>
			<form id="addForm">
				<div class="form-group row">
					<label class="col-sm-3 col-form-label" for="username-input" id="username-label">Username</label>
					<div class="col-sm-3">
						<input class="form-control" type="text" name="username" id="username-input" placeHolder="username"/>
						<div class="invalid-feedback">
              Invalid user
            </div>
					</div>
				</div>
				<div class="form-group row" id="passwordSection">
					<label class="col-sm-3 col-form-label" for="password-input" id="password-label">Password</label>
					<div class="col-sm-3">
						<input id="password-input" type="password" class="form-control" name="password" placeHolder="password"/>
						<div class="invalid-feedback">
            </div>
					</div>
				</div>
				<div class="form-group row" id="confirmPasswordSection">
					<label class="col-sm-3 col-form-label" for="confirm-password-input" id="confirm-password-label">Confirm password</label>
					<div class="col-sm-3">
						<input id="confirm-password-input" type="password" class="form-control" name="confirmPassword" placeHolder="password"/>
						<div class="invalid-feedback">
            </div>
					</div>
				</div>
				<div class="form-group row">
					<label class="col-sm-3 col-form-label" for="roles-input" id="roles-label">Add roles</label>
					<div class="col-sm-3">
						<input type="text" id="roles-input" class="form-control" name="role" placeHolder="SearchExpert" />
            <div class="invalid-feedback">
              At least one role must be assigned
            </div>
					</div>
				</div>
				<div class="form-group row">
          <div class="col-sm-3"></div>
				  <div id="roles" class="col-sm-3"></div>
				</div>
				<div class="form-group row">
				  <label class="col-sm-3 col-form-label">Save and activate the added user</label>
				  <div class="col">
				    <button type="submit" class="btn btn-primary" value="ADD" id="add-button"
				    data-loading-text="<i class='fa fa-spinner fa-spin'></i> Add User">Add User</button>
          </div>
				</div>
			</form>
			<div id="Message" style="display:none;">No Document Saved Yet</div>
			<div id="MessageSuccess" style="display:none;"><i class="fas fa-check"></i> User Saved Successfully</div>
		</div>
	</div>
</body>
</html>
