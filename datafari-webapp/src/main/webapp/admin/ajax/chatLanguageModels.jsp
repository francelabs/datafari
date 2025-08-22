<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>

<head>
<script src="<c:url value="/resources/js/admin/ajax/chatLanguageModelsConf.js" />" type="text/javascript"></script>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/ragConfiguration.css" />" />
<meta charset="UTF-8">
<title>Chat Language Models Configuration</title>
<Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />" />
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

  <div class="col-sm-12"></div>
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

    <div id="thBox" class="box-content">
      <div class="documentation-style-no-margin">
        <p class="documentation-preview">
          <span id="documentation-chatLanguageModels">Datafari Enterprise v6.2 provides AI powered features</span><a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3931832326"
            target="_blank"> ...see more</a>
        </p>
      </div>

      <form class="form-horizontal needs-validation" id="chatLanguageModels-form" novalidate onsubmit="return false">

        <!-- Select Active Model -->
        <div class="form-group row" id="model-selector">
          <label id="activeModelSelectLabel" class="col-sm-2 col-form-label" for="activeModelSelect"></label>
          <div class="col-sm-5">
            <select id="activeModelSelect" class="form-control"></select>
          </div>
        </div>

        <hr>

        <!-- Models Table -->
        <div class="form-group row" id="models-table-container">
          <div class="col-sm-12">
            <h4 id="definedModelsLabel"></h4>
            <table class="table table-striped" id="models-table">
              <thead>
                <tr>
                  <th class="text-left" id="columnName"></th>
                  <th class="text-left" id="columnInterfaceType"></th>
                  <th class="text-left" id="columnActions"></th>
                  <th class="text-left" id="columnTestResult"></th>
                </tr>
              </thead>
              <tbody id="models-table-body"></tbody>
            </table>
            <button type="button" class="btn btn-success" onclick="showAddModelForm()" id="addModelButton"></button>
          </div>
        </div>

        <hr>

        <!-- Model Form -->
        <div class="form-group row" id="model-form-container" style="display:none;">
          <div class="col-sm-12">
            <h4 id="form-title">Model form</h4>
              <div class="form-group row">
                <label for="model-name" class="col-sm-2 col-form-label" id="modelNameLabel"></label>
                <div class="col-sm-5">
                  <input type="text" id="model-name" class="form-control" required />
                </div>
              </div>
              <div class="form-group row">
                <label for="interface-type" class="col-sm-2 col-form-label" id="interfaceTypeLabel"></label>
                <div class="col-sm-5">
                  <select id="interface-type" class="form-control">
                    <option value="">-- Select --</option>
                    <option value="OpenAI">OpenAI</option>
                    <option value="AIAgent">Datafari AI Agent</option>
                    <option value="AzureOpenAI">Azure OpenAI (not tested)</option>
                    <option value="HuggingFace">Hugging Face (not tested)</option>
                    <option value="GoogleAiGemini">Google AI Gemini (not tested)</option>
                    <option value="Ollama">Ollama (not tested)</option>
                  </select>
                </div>
              </div>

              <div id="params-fields"></div>

              <div class="form-group row">
                <div class="col-sm-5 offset-sm-2">
                  <button type="button" class="btn btn-primary" id="saveModelButton" onclick="submitModelForm()"></button>
                  <button type="button" class="btn btn-secondary" onclick="cancelEdit()" id="cancelButton"></button>
                </div>
              </div>
          </div>
        </div>

        <div id="toast-message" class="alert alert-success" style="display:none;"></div>

      </form>
    </div>
  </div>
</body>
</html>
