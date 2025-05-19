<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<script
  src="<c:url value="/resources/js/admin/ajax/solrVectorSearch.js" />"
  type="text/javascript"></script>
<link rel="stylesheet" type="text/css"
  href="<c:url value="/resources/css/admin/solrVectorSearch.css" />" />
<meta charset="UTF-8">
<title>Solr Vector Search</title>
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
          <span id="documentation-solrVectorSearch"></span>Datafari Enterprise v6.2 provides an AI powered "Vector Search" feature<a
            class="documentation-link"
            href="solrVectorSearch"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="solrVectorSearch-form" class="needs-validation" novalidate>

        <!-- Enable Vector Search -->
        <div class="form-group row">
            <label id="enableVectorSearchLabel" class="col-sm-2 col-form-label" for="enableVectorSearch">Enable Vector Search</label>
            <div class="col-sm-5">
                <input type="checkbox" id="enableVectorSearch" name="enableVectorSearch" data-height="36" data-toggle="toggle" data-onstyle="success">
            </div>
		</div>


        <!-- Type of model/template ? -->
        <div class="form-group row">
            <label id="modelTemplateLabel" class="col-sm-2 col-form-label" for="modelTemplate"></label>
            <div class="col-sm-5">
                <select id="modelTemplate" class="form-control">
                    <option value="aiagent">Datafari AI Agent</option>
                    <option value="openai">OpenAI</option>
                    <option value="huggingface">Hugging Face</option>
                    <option value="mistral">Mistral</option>
                    <option value="cohere">Cohere</option>
                </select>
            </div>
		</div>

        <!-- Model (JSON textarea ?) -->
        <div class="form-group row">
            <label id="jsonModelLabel" class="col-sm-2 col-form-label" for="jsonModel"></label>
            <div class="col-sm-5">
                <textarea id="jsonModel" name="jsonModel" class="form-control" rows="13"></textarea>
            </div>
		</div>

        <!-- Vector field -->
        <div class="form-group row">
          <label id="vectorFieldLabel"
            class="col-sm-2 col-form-label" for="vectorField"></label>
          <div class="col-sm-5" id="vectorField-container">
            <input class="form-control" type="text" name="vectorField" id="vectorField" required />
          </div>
        </div>

        <!-- Filter minchunklength -->
        <div class="form-group row">
          <label for="minChunkLength" class="col-sm-2 col-form-label">Min Chunk Length</label>
          <div class="col-sm-5">
            <input class="form-control" type="number" min="1" name="minChunkLength" id="minChunkLength" required />
          </div>
        </div>

        <!-- Filter minalphanumratio -->
        <div class="form-group row">
          <label for="minAlphaNumRatio" class="col-sm-2 col-form-label">Min AlphaNum Ratio</label>
          <div class="col-sm-5">
            <input class="form-control" type="number" step="0.01" min="0" max="1" name="minAlphaNumRatio" id="minAlphaNumRatio" required />
          </div>
        </div>


        <div class="form-group row">
          <div class="col-sm-2">
            <button type="submit" class="btn btn-primary" value="save" id="save-conf">Save</button>
          </div>
        </div>
        <div class="col feedback-message alert" id="message"></div>


      </form>
    </div>
  </div>
</body>
</html>