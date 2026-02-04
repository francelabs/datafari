<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<script
  src="<c:url value="/resources/js/admin/ajax/assistantConfiguration.js" />"
  type="text/javascript"></script>
<link rel="stylesheet" type="text/css"
  href="<c:url value="/resources/css/admin/ragConfiguration.css" />" />
<meta charset="UTF-8">
<title>Datafari Assistant configuration</title>
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
          <span id="documentation-assistantConf"></span>Datafari Enterprise v7.0 provides a new, optional, Datafari Assistant features<a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3931832326"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="assistantConf-form" class="needs-validation" novalidate>


        <!-- Global AI Features -->

        <!-- Enable Assistant -->
        <div class="form-group row">
          <label id="enableAssistantLabel" class="col-sm-4 col-form-label" for="enableAssistant">Enable Datafari Assistant</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableAssistant" name="enableAssistant" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Enable RAG -->
        <div class="form-group row">
          <label id="enableRagLabel" class="col-sm-4 col-form-label" for="enableRag">Enable RAG service</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableRag" name="enableRag" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Enable agentic -->
        <div class="form-group row">
          <label id="enableAgenticLabel" class="col-sm-4 col-form-label" for="enableAgentic">Enable Agentic service</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableAgentic" name="enableAgentic" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Enable summarization -->
        <div class="form-group row">
          <label id="enableSummarizationLabel" class="col-sm-4 col-form-label" for="enableSummarization">Enable summarization service</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableSummarization" name="enableSummarization" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <!-- Enable conversation storage -->
        <div class="form-group row">
          <label id="enableConversationStorageLabel" class="col-sm-4 col-form-label" for="enableConversationStorage">Enable conversation storage</label>
          <div class="col-sm-5">
            <input type="checkbox" id="enableConversationStorage" name="enableConversationStorage" data-toggle="toggle" data-onstyle="success">
          </div>
        </div>

        <hr>
        <!-- Retrieval -->

        <div class="form-group row">
          <label id="assistantRetrievalMethodLabel" class="col-sm-2 col-form-label" for="assistantRetrievalMethod">Retrieval Method for search via assistant</label>
          <div class="col-sm-5">
            <select id="assistantRetrievalMethod" class="form-control">
              <option value="bm25">BM25</option>
              <option value="vector">Vector Search</option>
              <option value="rrf">Hybrid (RRF)</option>
            </select>
          </div>
        </div>

        <hr>

        <div class="form-group row">
          <label id="submitLabel" for="submit" class="col-sm-4 col-form-label"></label>
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