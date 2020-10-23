<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script src="<c:url value="/resources/js/admin/ajax/mcfErrorsStatistics.js" />" type="text/javascript"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/statistics.css" />" />
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
<div  class="row-fluid" style="height: 100%;"> <!-- QUICK AND DIRTY, must be another way using configuration of bootstrap mixins-->
    <p id="warning"></p>
    <iframe id="iFrameContent" width="100%" height="1024px"></iframe>
 </div>