<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<!-- Page specific JS -->
<script type="text/javascript" src="<c:url value="/resources/libs/jquery-datatables/datatables.min.js" />"></script>
<script src="<c:url value="/resources/js/admin/ajax/duplicates.js" />" type="text/javascript"></script>
<!-- Page specific CSS -->
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-datatables/datatables.min.css" />"/>
<link href="<c:url value="/resources/css/admin/duplicates.css" />" rel="stylesheet"></link>
<meta charset="UTF-8">
<title>Insert title here</title>
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
	<br/>
  <div class="documentation-style-no-margin">
    You can configure Datafari to detect duplicated files and display them in a specific admin UI. The default algorithm used to determine duplicates is the one provided by this Solr update processor<a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/681574448/Detect+duplicates+configuration+-+Enterprise+Edition" target="_blank">...see more</a>
  </div>
  <br/>
	<div class="box">
	
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="box-title"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>		
		
		<div class="box-content" id="thBox">
      <div id="duplicates-explanation" class="alert alert-info"></div>
      <table id="duplicates_table">
        <thead>
          <tr>
            <th id="duplicate_files"></th>
            <th id="dupe_nb"></th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>	
      <div id="duplicate-files-message" style="display:none;" class="feedback-message alert alert-danger"><i class="fas fa-check"></i>Configuration successfully saved.</div>						
		</div>
	</div>
  <br>
  <div id="details-box" class="box" style="display:none;">
  
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="details-box-title"></span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>    
    
    <div class="box-content" id="thBox">
      <table id="duplicates_details_table">
        <thead>
          <tr>
            <th id="duplicate_file_names"></th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>  
      <div id="duplicates_details_table_info"></div>    
      <div id="duplicates_details_table_paginate">
        <a id="duplicates_details_table_previous" class="paginate_button previous">Previous</a>
        <span></span>
        <a id="duplicates_details_table_next" class="paginate_button next">Next</a>
      </div>      
    </div>
    <div id="duplicate-details-message" style="display:none;" class="feedback-message alert alert-danger"><i class="fas fa-check"></i>Configuration successfully saved.</div>
  </div>
</body>
</html>