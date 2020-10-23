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
    <div class="documentation-style">
        <p class="documentation-preview"> <span id="documentation-entityextraction"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/495321093/Basic+Text+Tagging+at+indexing+and+Searching+time" target="_blank"> ...see more</a></p>
      </div>
    <div id="thBox" class="box-content">      
      <div class="alert alert-danger"><i class='fa fa-exclamation-triangle'></i> Changes in those parameters require reloading the solr core and indexing your content from scratch (by deleting your jobs and creating them from scratch).</div>
      <form class="form-horizontal" role="form" id="simpleEntityExtractionForm">
        <div class="form-group">
          <label class="col-sm-3 control-label" for="activateSimpleExtraction" id="activateSimpleExtractionLabel">Enable simple entity extraction:</label>
          <div class="col-sm-3">
            <input type="checkbox" id="activateSimpleExtraction" name="activateSimpleExtraction" data-size="sm" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>
        <div class="form-group" id="simplePersonsSection">
          <label class="col-sm-3 control-label" for="simplePersons" id="simplePersonsLabel">Activate simple person extraction:</label>
          <div class="col-sm-3">
            <input type="checkbox" id="simplePersons" name="simplePersons" data-size="sm" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>
        <div class="form-group" id="simplePhonesSection">
          <label class="col-sm-3 control-label" for="simplePhones" id="simplePhonesLabel">Activate simple phones extraction:</label>
          <div class="col-sm-3">
            <input type="checkbox" id="simplePhones" name="simplePhones" data-size="sm" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>
        <div class="form-group" id="simpleSpecialSection">
          <label class="col-sm-3 control-label" for="simpleSpecials" id="simpleSpecialsLabel">Activate simple special extraction:</label>
          <div class="col-sm-3">
            <input type="checkbox" id="simpleSpecials" name="simpleSpecials" data-size="sm" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>
        <div class="form-group">
          <div class="col-sm-3">
            <label id="extractionSave" for=""></label>
          </div>
				  <div class="col-sm-2">
				    <button type="submit" class="btn btn-primary" value="SAVE" id="simpleSaveButton"
				    data-loading-text="<i class='fa fa-spinner fa-spin'></i> Save Parameters">Save Parameters</button>
				  </div>
				</div>
      </form>
      <div id="simpleFeedbackMessage" style="display:none;"><i class="fas fa-check"></i>Configuration saved successfuly</div>
    </div>
  </div>
</body>
</html>
