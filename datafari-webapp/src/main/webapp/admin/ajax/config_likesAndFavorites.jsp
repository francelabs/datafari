<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
	<head>
	<script src="<c:url value="/resources/js/admin/ajax/configLikesAndFavorites.js" />"></script>
	
		<style>
		
				#message_info.bs-callout{
						padding: 20px;
					margin: 20px 0px;
					border-width: 1px 1px 1px 5px;
					border-style: solid;
					border-color: #EEE;
					-moz-border-top-colors: none;
					-moz-border-right-colors: none;
					-moz-border-bottom-colors: none;
					-moz-border-left-colors: none;
					border-image: none;
					border-radius: 3px;
					background: transparent;
			}
			#message_info{
				display:none;
			}
			
				#message_info.bt-callout-success{
					border-left-color:#6AB77B;
				}
				#message_info.bt-callout-success h4{
				color : #6AB77B;
				}
				#message_info.bt-callout-danger{
				border-left-color:rgb(255, 124, 124);
				}
				#message_info.bt-callout-danger h4{
				color:rgb(255, 124, 124);
				}
			</style>
			<style>
			i.error_deduplication{
				color : red;
				display: none;
			}
			</style>
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
  
	<div id="likes-favorites-box" class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="title">Favorites</span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div class="box-content">
			<div id="favorites-global" class="col-xl-6">				    	
				<h4 id="favorites-title"></h4>			
				<p id="favorites-label"></p>
        <input type="checkbox" id="likesAndFavorites" name="likesAndFavorites" data-height="36" data-toggle="toggle" data-onstyle="success">		 
				<div class="bs-callout bs-callout-danger" id="message_info">
					<h4>s</h4>
					<p></p>
				</div>
			</div>
		</div>
	</div>		
	</body>	
</html>