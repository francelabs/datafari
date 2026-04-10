<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<script
  src="<c:url value="/resources/js/admin/ajax/ragConfiguration.js" />"
  type="text/javascript"></script>
<link rel="stylesheet" type="text/css"
  href="<c:url value="/resources/css/admin/ragConfiguration.css" />" />
<meta charset="UTF-8">
<title>RAG and AI configuration</title>
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
          <span id="documentation-ragConf"></span>Datafari Enterprise v7.0 provides AI powered features<a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3931832326"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="ragConf-form" class="needs-validation" novalidate>

        <!-- Global -->
        <legend id="aiServicesLegend">AI services</legend>

        <!-- Max prompt size in characters (global) -->
        <div class="form-group row">
          <label id="maxRequestSizeLabel" class="col-sm-3 col-form-label" for="maxRequestSize">Max Request Size (in characters)</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="maxRequestSize" name="maxRequestSize" step="1" min="1000" placeholder="40000">
          </div>
        </div>

        <!-- Max chunk size in characters (global) -->
        <div class="form-group row">
          <label id="chunkingChunkSizeLabel" class="col-sm-3 col-form-label" for="chunkingChunkSize">Chunk Size</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="chunkingChunkSize" name="chunkingChunkSize" step="1" min="0"  placeholder="30000">
          </div>
        </div>

        <!-- Enable Chat Memory (global) -->
        <div class="form-group row">
          <label id="chatMemoryEnabledLabel" class="col-sm-3 col-form-label" for="chatMemoryEnabled">Enable Memory</label>
          <div class="col-sm-5">
            <input type="checkbox" id="chatMemoryEnabled" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Chat Memory size (global) -->
        <div class="form-group row">
          <label id="chatMemoryHistorySizeLabel" class="col-sm-3 col-form-label" for="chatMemoryHistorySize">History Size</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="chatMemoryHistorySize" min="0" step="1" placeholder="6">
          </div>
        </div>


        <legend id="ragServiceLegend">Retrieval-Augmented Generation (RAG) service</legend>

        <!-- Enable query rewriting (RAG) -->
        <div class="form-group row">
          <label id="chatQueryRewritingEnabledLabel" class="col-sm-3 col-form-label">Enable Query Rewriting</label>
          <div class="col-sm-5">
            <label>
                <input type="checkbox" id="chatQueryRewritingEnabledBM25" data-toggle="toggle" data-onstyle="success">
                <span id="chatQueryRewritingEnabledBM25Label">For BM25</span>
            </label>
            <label>
                <input type="checkbox" id="chatQueryRewritingEnabledVector" data-toggle="toggle" data-onstyle="success">
                <span id="chatQueryRewritingEnabledVectorLabel">For Vector</span>
            </label>
          </div>
        </div>


        <!-- Chunking strategy for RAG-->
        <div class="form-group row">
          <label id="chunkingStrategyLabel" class="col-sm-3 col-form-label" for="chunkingStrategy">Chunking Strategy</label>
          <div class="col-sm-5">
            <select id="chunkingStrategy" class="form-control">
              <option value="refine">Iterative Refining (recommended)</option>
              <option value="mapreduce">Map-Reduce</option>
            </select>
          </div>
        </div>

        <!-- Retrieval method for RAG -->
        <div class="form-group row">
          <label id="retrievalMethodLabel" class="col-sm-3 col-form-label" for="retrievalMethod">Retrieval Method</label>
          <div class="col-sm-5">
            <select id="retrievalMethod" class="form-control">
              <option value="bm25">BM25</option>
              <option value="vector">Vector Search</option>
              <option value="rrf">Hybrid (RRF)</option>
            </select>
          </div>
        </div>

        <!-- Max number of files for BM25 (RAG) -->
        <div class="form-group row bm25Only">
          <label id="chunkingMaxFilesLabel" class="col-sm-3 col-form-label" for="chunkingMaxFiles">Max Files (BM25)</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="chunkingMaxFiles" name="chunkingMaxFiles" step="1" placeholder="3">
          </div>
        </div>

        <!-- Search operator for BM25 (RAG) -->
        <div class="form-group row bm25Only">
          <label id="ragOperatorLabel" class="col-sm-3 col-form-label" for="ragOperator">Search Operator</label>
          <div class="col-sm-5">
            <select id="ragOperator" class="form-control">
              <option value="OR">OR (recommended)</option>
              <option value="AND">AND</option>
            </select>
          </div>
        </div>

        <!-- Common for Vector & Hybrid Search -->
        <!-- topK for vector (RAG) -->
        <div class="form-group row vectorSearchOnly rrfOnly">
          <label id="ragTopKLabel" class="col-sm-3 col-form-label" for="ragTopK">Solr topK</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="ragTopK" step="1" min="1" placeholder="10">
          </div>
        </div>

        <!-- Solr Hybrid Search -->
        <!-- topK for hybrid (RAG) -->
        <div class="form-group row rrfOnly">
          <label id="rrfTopKLabel" class="col-sm-3 col-form-label" for="rrfTopK">RRF topK</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="rrfTopK" step="1" min="1" placeholder="50">
          </div>
        </div>

        <!-- RRF constant for hybrid (RAG) -->
        <div class="form-group row rrfOnly">
          <label id="rrfRankConstantLabel" class="col-sm-3 col-form-label" for="rrfRankConstant">RRF Rank Constant</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="rrfRankConstant" step="1" min="1" placeholder="60">
          </div>
        </div>


        <!-- ### Agentic service ### -->
        <legend id="agenticServiceLegend">Agentic service</legend>

        <!-- Enable loop control -->
        <div class="form-group row">
          <div class="col-sm-3" >
            <label id="enableLoopControlLabel" class="col-form-label" for="enableLoopControl">Enable Loop Control for Agentic</label>
          </div>
          <div class="col-sm-5">
            <input type="checkbox" id="enableLoopControl" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Max number of iterations in Loop Control -->
        <div class="form-group row loopControlOnly">
          <div class="col-sm-3" >
            <label id="loopControlMaxIterationsLabel" class="col-form-label" for="loopControlMaxIterations">Max number of iterations in Loop Control</label>
          </div>
          <div class="col-sm-5">
            <input type="number" id="loopControlMaxIterations" step="1" min="1" placeholder="3">
          </div>
        </div>

        <!-- Loop control threshold -->
        <div class="form-group row loopControlOnly">
          <div class="col-sm-3" >
            <label id="loopControlMinScoreLabel" class="col-form-label" for="loopControlMinScore">Loop control threshold (default: 0.8)</label>
          </div>
          <div class="col-sm-5">
            <input type="number" id="loopControlMinScore" step="0.05" min="0" max="1" placeholder="0.8">
          </div>
        </div>

        <!-- Iterations before secondary Loop Control threshold -->
        <div class="form-group row loopControlOnly">
          <div class="col-sm-3" >
            <label id="loopControlMaxIterationsBeforeSecondaryLabel" class="col-form-label" for="loopControlMaxIterationsBeforeSecondary">
                Iterations before secondary Loop Control threshold (0 to disable)
            </label>
          </div>
          <div class="col-sm-5">
            <input type="number" id="loopControlMaxIterationsBeforeSecondary" step="1" min="0" placeholder="3">
          </div>
        </div>

        <!-- Secondary Loop Control threshold -->
        <div class="form-group row loopControlOnly">
          <div class="col-sm-3" >
            <label id="loopControlMinScoreSecondaryLabel" class="col-form-label" for="loopControlMinScoreSecondary">Secondary Loop Control threshold (default: 0.6)</label>
          </div>
          <div class="col-sm-5">
            <input type="number" id="loopControlMinScoreSecondary" step="0.05" min="0" max="1" placeholder="0.6">
          </div>
        </div>

        <!-- TODO : add loop control options -->


        <div class="form-group row">
          <label id="submitLabel" for="submit" class="col-sm-3 col-form-label"></label>
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