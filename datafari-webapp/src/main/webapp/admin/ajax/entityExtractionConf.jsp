<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Add Users</title>
  <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/entityExtraction.css" />" />
  <Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
</head>
<script src="<c:url value="/resources/js/admin/ajax/entityExtractionConfig.js" />" type="text/javascript"></script>
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
        <i class="fas fa-table"></i><span  id="title">Simple Entity Extraction Configuration</span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    
    <div id="thBox" class="box-content">      
      <div class="alert alert-danger"><i class='fa fa-exclamation-triangle'></i><span id="removedEntityExtraction"></span></div>
      
      
    </div>
  </div>
</body>
</html>
