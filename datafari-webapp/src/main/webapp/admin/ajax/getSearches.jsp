<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/getSearches.js" />" type="text/javascript"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/getSearches.css" />" />
<meta charset="UTF-8">
<title>Searches</title>
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
		
		<div class="col-xl-8 col-lg-offset-1">
		<h1 style="display:none"> <i class="fas fa-bookmark"></i></h1><br/>
		<p class="description" style="display:none">
			<i style="color:#F00" class="fas fa-ban"></i> 
		</p>
		<br/>
		<div class="loading"><i class="fas fa-circle-o-notch fa-spin"></i> Loading ...</div>
			<div id="Message" style="display:none;">No Search Saved Yet</div>
			<table id="tableResult" class="table table-bordered table-striped">
					<thead>
						<tr>
							<th>Search</th>
							<th>Link</th>
							<th>Delete</th>
						</tr>
					</thead>
					<tbody>
						
					</tbody>
			</table>
			
		</div>
	</body>
</html>