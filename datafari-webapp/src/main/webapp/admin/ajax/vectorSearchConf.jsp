<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<script
  src="<c:url value="/resources/js/admin/ajax/vectorSearchConf.js" />"
  type="text/javascript"></script>
<link rel="stylesheet" type="text/css"
  href="<c:url value="/resources/css/admin/ragConfiguration.css" />" />
<meta charset="UTF-8">
<title>Vector Search configuration</title>
<Link rel="stylesheet"
  href="<c:url value="/resources/css/animate.min.css" />" />
</head>
<body>
  <nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i
      class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink"
        id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page"
        id="topbar3"></li>
    </ol>
  </nav>

  <div class="col-sm-12"></div>
  <div class="box">
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="title"></span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div id="thBox" class="box-content">
      <div class="documentation-style-no-margin">
        <p class="documentation-preview">
          <span id="documentation-vectorSearchConf"></span>Datafari Enterprise v7.0 provides a Vector Search features<a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3920297985"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="vectorSearchConf-form" class="needs-validation" novalidate>



        <!-- TopK for Hybrid Search Search Vector Search -->
        <div class="form-group row vectorSearchOnly rrfOnly">
          <label id="solrTopKLabel" class="col-sm-3 col-form-label" for="solrTopK">TopK for Vector Search (recommended 10)</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="solrTopK" step="1" min="1" placeholder="10">
          </div>
        </div>

        <!-- TopK for Hybrid Search Search -->
        <div class="form-group row rrfOnly">
          <label id="rrfTopKLabel" class="col-sm-3 col-form-label" for="rrfTopK">TopK for Hybrid Search Search (recommended 50)</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="rrfTopK" step="1" min="1" placeholder="50">
          </div>
        </div>

        <!-- RRF Rank constant -->
        <div class="form-group row rrfOnly">
          <label id="rrfRankConstantLabel" class="col-sm-3 col-form-label" for="rrfRankConstant">RRF Rank Constant for Hybrid Search (recommended 60)</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="rrfRankConstant" step="1" min="1" placeholder="60">
          </div>
        </div>

        <!-- Enable/disable ACORN optimization (filtered search) -->
        <!-- ACORN is an algorithm designed to make hybrid searches consisting of a filter and a vector search more efficient. This approach tackles both the performance limitations of pre- and post- filtering. It modifies the construction of the HNSW graph and the search on it. -->
        <div class="form-group row">
          <div class="col-sm-3" >
              <label id="enableAcornLabel" for="enableAcorn" class="col-form-label">Enable ACORN algorithm optimization</label>
          </div>
          <div class="col-sm-5">
             <input type="checkbox" id="enableAcorn" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- filteredSearchThreshold (for ACORN optimization -->
        <!--  If the percentage of documents that satisfies the filter is less than the threshold ACORN will be used. From 0 (never use ACORN) to 100 (always use ACORN) -->
        <div class="form-group row acornOnly">
          <div class="col-sm-3" >
              <label id="filteredSearchThresholdLabel" class="col-form-label" for="filteredSearchThreshold">Filtered search threshold for ACORN optimization (recommended value is 60)</label>
          </div>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="filteredSearchThreshold" name="filteredSearchThreshold" step="1" placeholder="60" min="0" max="100">
          </div>
        </div>

        <!-- Lexically-Accelerated Dente Retrieval (LADR) -->
        <!-- Use SeededKnnVectorQuery to initiate the entry points in the HNSW graph with a seedQuery, in order to improve the relevancy of the results. -->
        <div class="form-group row">
          <div class="col-sm-3" >
              <label id="enableLadrLabel" class="col-form-label" for="enableLadr">Enable Lexically-Accelerated Dense Retrieval (LADR) optimization</label>
          </div>
          <div class="col-sm-5">
            <input type="checkbox" id="enableLadr" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <div class="form-group row">
          <label id="submitLabel" for="submit" class="col-sm-s3 col-form-label"></label>
          <div class="col-sm-5">
            <button type="submit" class="btn btn-primary" value="save" id="save-conf">Save</button>
            <span id="loadingIndicator" style="display:none; margin-left:10px;">
              <i class="fas fa-spinner fa-spin"></i> Saving...
            </span>
          </div>
        </div>
        <div class="col feedback-message alert" id="message"></div>

      </form>

    </div>
  </div>

</body>
</html>