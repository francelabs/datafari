<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>Modify User</title>
	<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/modifyUsers.css" />" />
	<Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
</head>
<script src="<c:url value="/resources/js/admin/ajax/modifyServiceUsers.js" />" type="text/javascript"></script>
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
  				<i class="fas fa-table"></i><span  id="title">Modify Users</span>
  			</div>
  			<div class="box-icons">
  				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
  			</div>
  			<div class="no-move"></div>
  		</div>
  		<div id="thBox" class="box-content">
  			<h1>Modify a User  <i style="color:#A8C900" class="fas fa-users"></i></h1><br/>
  			<div class="documentation-style-no-margin">
  				<p class="documentation-preview"> <span id="documentation-modifyusersapache"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/644448261/Users+management+in+Apache+proxy" target="_blank"> ...see more</a></p>
  			</div>
  			<form class="form-horizontal" role="form">
  				<div class="col">					
  					<p>Here you can change the password of a user from Apache.<br/></p><br/>
  					
  					<table id="tableResult" class="table table-bordered table-striped">
  						<thead>
  							<tr>
  								<th>Username</th>
  								<th>Change Password<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Be carefull that once you type enter after having entered your new password, it will be immediately taken into account. No further confirmation will be displayed'>i</button></span></th>
  							</tr>
  						</thead>
  						<tbody id="apacheBody">
  									
  						</tbody>
  					</table>
  					<div id="Message"><i class="fas fa-check success"></i> Change done. The web proxy is restarting, please wait 2 minutes before using the admin pages again</i></div>
  				</div>
  			</form>
        <br/>
  		</div>	
    </div>
</body>
</html>