<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/duplicatesConfiguration.js" />" type="text/javascript"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/duplicatesConfiguration.css" />" />
<meta charset="UTF-8">
<title>Duplicates Configuration</title>
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
  <br/>
	<div class="documentation-style-no-margin">
    You can configure Datafari to detect duplicated files and display them in a specific admin UI. The default algorithm used to determine duplicates is the one provided by this Solr update processor<a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/681574448/Detect+duplicates+configuration+-+Enterprise+Edition" target="_blank">...see more</a>
  </div>
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
		<div id="thBox" class="box-content">
			<form id="duplicates-conf-form" name="duplicates-conf-form" class="form-horizontal" role="form">
					
					<div class="row baseline-content">
            <label id="duplicatesActivationLabel" class="col-sm-4 control-label"></label>
						<div class="col-sm-5 p-0">
						  <input type="checkbox" id="duplicates_activation" name="duplicates_activation" data-height="36" data-toggle="toggle" data-onstyle="success">
						</div>	
					</div>
			
          <div class="row baseline-content">
            <label id="duplicatesHostLabel" class="col-sm-4 control-label"></label>
            <input type="text" id="duplicatesHost" class="col-sm-5">
          </div>
          <div class="row baseline-content">
            <label id="duplicatesCollectionLabel" class="col-sm-4 control-label"></label>
            <input type="text" id="duplicatesCollection" class="col-sm-5">
          </div>
					<div class="row baseline-content">
            <label id="duplicatesConfSaveLabel" class="col-sm-4 control-label"></label>
						<button id="submit" name="submitth" class="btn btn-primary btn-label-left"></button>
					</div>
					
					<br/>
					
				
			</form>
			<div id="duplicates-conf-message" style="visibility:hidden;" class="feedback-message alert alert-success"><i class="fas fa-check"></i>Configuration successfully saved.</div>
		</div>
	</div>
  <div class="box">
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span  id="algorithm-title"></span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div id="thBox" class="box-content">
      <form id="algorithm-conf-form" name="algorithm-conf-form" class="form-horizontal" role="form">
          
          <div class="row baseline-content">
            <label id="duplicatesFiledsLabel" class="col-sm-4 control-label"></label>
            <input type="text" id="duplicatesFields" class="col-sm-5">
          </div>
          <div class="row baseline-content">
            <label id="duplicatesQuantLabel" class="col-sm-4 control-label"></label>
            <input type="text" id="duplicatesQuant" class="col-sm-5">
          </div>
          <div class="row baseline-content">
            <label id="algorithmSaveLabel" class="col-sm-4 control-label"></label>
            <button id="submit-algorithm" name="submitth" class="btn btn-primary btn-label-left"></button>
          </div>
          
          <br/>
      </form>
      <div id="algorithm-conf-message" style="visibility:hidden;" class="feedback-message alert alert-success"><i class="fas fa-check"></i>Configuration successfully saved.</div>
    </div>
  </div>
</body>
</html>