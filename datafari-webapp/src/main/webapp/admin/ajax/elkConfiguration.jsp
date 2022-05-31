<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/elkConfiguration.js" />" type="text/javascript"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/elkConfiguration.css" />" />
<meta charset="UTF-8">
<title>Analytics (ELK) Configuration</title>
<Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
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
	<div class="col-sm-12"><span id="globalAnswer"></span></div><!-- <br/> -->
	<div class="col-sm-12"></div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="title"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>		
		<div class="box-content" id="thBox">
			<div class="documentation-style-no-margin">
			 <p class="documentation-preview"> <span id="documentation-elkconfiguration"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/114720796/Analytics+ELK+Configuration+Datafari+4.0+and+above" target="_blank"> ...see more</a></p>
		  </div>
  		<form class="form-horizontal" role="form">
  				
  				<div class="row baseline-content">
          <label id="elkActivationLabel" class="col-sm-4 col-form-label"></label>
  					<div class="col-sm-5 p-0">
  					 <input type="checkbox" id="elk_activation" name="elk_activation" data-size="sm" data-toggle="toggle" data-onstyle="success" data-offstyle="danger">
  					</div>	
  				</div>
  				<div>
  				<br/>
  			</div>
  		</form>
		  <div class="col" id="message"></div>		
	  </div>
  </div>
</body>
</html>