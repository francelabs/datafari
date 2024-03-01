<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/fieldWeightAPI.js" />"></script>
<meta charset="UTF-8">
<title>Insert title here</title>
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
	<div class="col"><span id="globalAnswer"></span></div>	
	<div class="col"></div>
	<div class="box">
	<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="thname2"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div class="documentation-style">
			<p class="documentation-preview"> <span id="documentation-fieldweightapi"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/17858566/Index+fields+relevancy+weights+Configuration" target="_blank">...see more</a></p>
		</div>
	<div id="tableBox" class="box-content">
			<form class="form-horizontal" role="form">
			<table class="table table-striped table-bordered table-hover table-heading no-border-bottom" id="tableau">
			<thead id="thead">
				<tr>
					<th id="name"></th>
					<th id="type"></th>
					<th id="delete"></th>
				</tr>
			</thead>
			<tbody id="tbody"></tbody>
			</table>
			</form>
			<div>
				<p><span id="explanationsText"></span></p>
			</div>
			<div class="row">
        <label class="col-sm-3 col-form-label" id="fieldWeightAdd" for=""></label>
				<div class="col-sm-3 col-form-label">
					<button id="addRow" name="addRow" class="btn btn-primary"></button>
				</div>
			</div>
			<div class="row">
        <label class="col-sm-3 col-form-label" id="fieldWeightConfirm" for=""></label>
				<div class="col-sm-3 col-form-label">
					<button id="submittab" name="submittab" class="btn btn-primary" ></button>
				</div>
			</div>			
  </div>
  </div>
	
<div class="col"></div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="thname"></span>
			</div>
			<div class="box-icons">
				<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div id="thBox" class="box-content">
			<div class="documentation-style-no-margin">
				<p class="documentation-preview">Because the concepts of PF and QF can be challenging to understand, both are available only via the expert mode. In order to better understand these concepts, please consult the Solr documentation on the edismax query parser<a class="documentation-link" href="https://lucene.apache.org/solr/guide/7_5/the-dismax-query-parser.html" target="_blank"> ...see more</a></p>
			</div>
			<form class="form-horizontal" role="form">
		  <div class="row">
				<label id="labelth" class="col-sm-4 col-form-label"></label>
          <div class="col-sm-5 col-form-label">
            <input type="text"  id="qfAPI" name="qfAPI" class="form-control">
          </div>	
        </div>
        								
		<div class="row">
		  <label id="labelth2" class="col-sm-4 col-form-label"></label>
          <div class="col-sm-5 col-form-label">
            <input type="text"  id="pfAPI" name="pfAPI" class="form-control">
          </div>
		</div>

		<div class="row">
		  <label id="labelth3" class="col-sm-4 col-form-label" for="boostAPI"></label>
          <div class="col-sm-5 col-form-label">
            <input type="text"  id="boostAPI" name="boostAPI" class="form-control">
          </div>
		</div>

		<div class="row">
		  <label id="labelth4" class="col-sm-4 col-form-label" for="bqAPI"></label>
          <div class="col-sm-5 col-form-label">
            <input type="text"  id="bqAPI" name="bqAPI" class="form-control">
          </div>
		</div>

		<div class="row">
		  <label id="labelth5" class="col-sm-4 col-form-label" for="bfAPI"></label>
          <div class="col-sm-5 col-form-label">
            <input type="text"  id="bfAPI" name="bfAPI" class="form-control">
          </div>
		</div>
        			
		<div class="row" id="field-weight-expert-margin">
          <label class="col-sm-3 col-form-label" id="fieldWeightExpertConfirm" for=""></label>				
		  <div class="col-sm-3 col-form-label">
			<button id="submitth" name="submitth" class="btn btn-primary btn-label-left"></button>
		  </div>
		</div>
        <div class="col p-0"><span id="answerth"></span></div>
			</form>
		</div>
	</div>	
	
</body>
</html>