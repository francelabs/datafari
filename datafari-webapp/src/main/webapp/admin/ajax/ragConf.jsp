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
          <span id="documentation-ragCong"></span>Datafari Enterprise v6.2 provides AI powered features<a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3931832326"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="ragCong-form" class="needs-validation" novalidate>


        <!-- Global AI Features -->

        <!-- Enable RAG -->
        <div class="form-group row">
          <label id="enableRagLabel" class="col-sm-2 col-form-label" for="enableRag">Enable RAG</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableRag" name="enableRag" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Enable summarization -->
        <div class="form-group row">
          <label id="enableSummarizationLabel" class="col-sm-2 col-form-label" for="enableSummarization">Enable summarization</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableSummarization" name="enableSummarization" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <hr>
        <!-- Web Service Parameters -->

        <div class="form-group row">
          <label id="apiEndpointLabel" class="col-sm-2 col-form-label" for="apiEndpoint">API Endpoint</label>
          <div class="col-sm-5">
            <input type="text" class="form-control" id="apiEndpoint" name="apiEndpoint" placeholder="https://api.openai.com/v1/">
          </div>
        </div>

        <div class="form-group row">
          <label id="apiTokenLabel" class="col-sm-2 col-form-label" for="apiToken">API Token</label>
          <div class="col-sm-5">
            <input type="password" class="form-control" id="apiToken" name="apiToken">
          </div>
        </div>

        <div class="form-group row">
          <label id="llmServiceLabel" class="col-sm-2 col-form-label" for="llmService">LLM Service</label>
          <div class="col-sm-5">
            <select id="llmService" class="form-control">
              <option value="openai">OpenAI</option>
            </select>
          </div>
        </div>

        <hr>
        <!-- LLM Settings -->

        <div class="form-group row">
          <label id="llmModelLabel" class="col-sm-2 col-form-label" for="llmModel">LLM Model</label>
          <div class="col-sm-5">
            <input type="text" class="form-control" id="llmModel" name="llmModel" placeholder="gpt-4o-mini">
          </div>
        </div>

        <div class="form-group row">
          <label id="llmTemperatureLabel" class="col-sm-2 col-form-label" for="llmTemperature">Temperature</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="llmTemperature" name="llmTemperature" min="0" max="1" step="0.01">
          </div>
        </div>

        <div class="form-group row">
          <label id="llmMaxTokensLabel" class="col-sm-2 col-form-label" for="llmMaxTokens">Max Tokens</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="llmMaxTokens" name="llmMaxTokens">
          </div>
        </div>


        <hr>
        <!-- Prompting & Chunking -->

        <div class="form-group row">
          <label id="chunkingStrategyLabel" class="col-sm-2 col-form-label" for="chunkingStrategy">Chunking Strategy</label>
          <div class="col-sm-5">
            <select id="chunkingStrategy" class="form-control">
              <option value="refine">Iterative Refining (recommended)</option>
              <option value="mapreduce">Map-Reduce</option>
            </select>
          </div>
        </div>

        <div class="form-group row">
          <label id="maxRequestSizeLabel" class="col-sm-2 col-form-label" for="maxRequestSize">Max Request Size</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="maxRequestSize" name="maxRequestSize" step="1" min="1000" placeholder="40000">
          </div>
        </div>

        <div class="form-group row">
          <label id="chunkingChunkSizeLabel" class="col-sm-2 col-form-label" for="chunkingChunkSize">Chunk Size</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="chunkingChunkSize" name="chunkingChunkSize" step="1" min="0"  placeholder="30000">
          </div>
        </div>


        <hr>
        <!-- Chat Memory -->

        <div class="form-group row">
          <label id="chatQueryRewritingEnabledLabel" class="col-sm-2 col-form-label">Enable Query Rewriting</label>
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

        <div class="form-group row">
          <label id="chatMemoryEnabledLabel" class="col-sm-2 col-form-label" for="chatMemoryEnabled">Enable Memory</label>
          <div class="col-sm-5">
            <input type="checkbox" id="chatMemoryEnabled" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <div class="form-group row">
          <label id="chatMemoryHistorySizeLabel" class="col-sm-2 col-form-label" for="chatMemoryHistorySize">History Size</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="chatMemoryHistorySize" min="0" step="1" placeholder="6">
          </div>
        </div>

        <hr>
        <!-- Retrieval -->

        <div class="form-group row">
          <label id="retrievalMethodLabel" class="col-sm-2 col-form-label" for="retrievalMethod">Retrieval Method</label>
          <div class="col-sm-5">
            <select id="retrievalMethod" class="form-control">
              <option value="bm25">BM25</option>
              <option value="vector">Vector Search</option>
              <option value="rrf">Hybrid (RRF)</option>
            </select>
          </div>
        </div>

        <!-- BM25 -->
        <div class="form-group row bm25Only">
          <label id="chunkingMaxFilesLabel" class="col-sm-2 col-form-label" for="chunkingMaxFiles">Max Files (BM25)</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="chunkingMaxFiles" name="chunkingMaxFiles" step="1" placeholder="3">
          </div>
        </div>

        <div class="form-group row bm25Only">
          <label id="ragOperatorLabel" class="col-sm-2 col-form-label" for="ragOperator">Search Operator</label>
          <div class="col-sm-5">
            <select id="ragOperator" class="form-control">
              <option value="OR">OR (recommended)</option>
              <option value="AND">AND</option>
            </select>
          </div>
        </div>

        <!-- Solr Vector Search -->
        <div class="form-group row vectorSearchOnly">
          <label id="solrEmbeddingsModelLabel" class="col-sm-2 col-form-label" for="solrEmbeddingsModel">Embeddings Model</label>
          <div class="col-sm-5">
            <input type="text" class="form-control" id="solrEmbeddingsModel" readonly placeholder="Vector Search must be enabled and configured to be used with RAG">
          </div>
        </div>

        <div class="form-group row vectorSearchOnly">
          <label id="solrVectorFieldLabel" class="col-sm-2 col-form-label" for="solrVectorField">Vector Field</label>
          <div class="col-sm-5">
            <input type="text" class="form-control" id="solrVectorField" readonly placeholder="Vector Search must be enabled and configured to be used with RAG">
          </div>
        </div>

        <!-- Common for Vector & Hybrid Search -->
        <div class="form-group row vectorSearchOnly rrfOnly">
          <label id="solrTopKLabel" class="col-sm-2 col-form-label" for="solrTopK">Solr topK</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="solrTopK" step="1" min="1" placeholder="10">
          </div>
        </div>

        <!-- Solr Hybrid Search -->
        <div class="form-group row rrfOnly">
          <label id="rrfTopKLabel" class="col-sm-2 col-form-label" for="rrfTopK">RRF topK</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="rrfTopK" step="1" min="1" placeholder="50">
          </div>
        </div>

        <div class="form-group row rrfOnly">
          <label id="rrfRankConstantLabel" class="col-sm-2 col-form-label" for="rrfRankConstant">RRF Rank Constant</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" id="rrfRankConstant" step="1" min="1" placeholder="60">
          </div>
        </div>


        <div class="form-group row">
          <label id="submitLabel" for="submit" class="col-sm-2 col-form-label"></label>
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