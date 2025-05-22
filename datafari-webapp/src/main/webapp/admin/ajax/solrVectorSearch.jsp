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


        <!-- Select an existing model or create a new one -->
        <div class="form-group row">
            <label id="modelLabel" class="col-sm-2 col-form-label" for="model"></label>
            <div class="col-sm-5">
                <select id="model" class="form-control">
                    <option value="new">Add a new embeddings model</option>
                </select>
            </div>
		</div>

        <!-- Model JSON (readonly) -->
        <div class="form-group row">
            <label id="jsonModelLabel" class="col-sm-2 col-form-label" for="jsonModel"></label>
            <div class="col-sm-5">
                <textarea id="jsonModel" name="jsonModel" class="form-control" rows="13" readonly></textarea>
            </div>
		</div>

        <!-- Delete model -->
        <div id="deleteModelContainer" class="form-group row"  style="display:none;">
            <label class="col-sm-2 col-form-label"></label>
		    <button id="deleteModel" class="btn btn-danger" type="button" style="margin-left:10px;">
              <i class="fas fa-trash"></i> <span id="deleteModelLabel">Delete this model</span>
            </button>
		</div>

		<div class="box" id="modelConfgurationBox">
            <div class="box-header">
                <div class="box-name">
                    <i class="fas fa-table"></i>
                    <span id="filerJobTitle">Model creation/edition</span> <!-- TODO : localize -->
                </div>
            </div>

            <div id="addBox" class="box-content">

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

                <!-- Name (identifier) of the model JSON configuration -->
                <div class="form-group row">
                        <label id="modelIdLabel" class="col-sm-2 col-form-label" for="modelId"></label>
                        <div class="col-sm-5">
                            <input type="text" id="modelId" name="modelId" class="form-control" value="default_model" required />
                        </div>
                </div>

                <!-- Actual model to use from the external service -->
                <div class="form-group row">
                        <label id="modelNameLabel" class="col-sm-2 col-form-label" for="modelName"></label>
                        <div class="col-sm-5">
                            <input type="text" id="modelName" name="modelName" class="form-control" required />
                        </div>
                </div>

                <!-- base Url -->
                <div class="form-group row">
                        <label id="baseUrlLabel" class="col-sm-2 col-form-label" for="baseUrl"></label>
                        <div class="col-sm-5">
                            <input type="text" id="baseUrl" name="baseUrl" class="form-control" required />
                        </div>
                </div>

                <!-- API key -->
                <div class="form-group row">
                        <label id="apiKeyLabel" class="col-sm-2 col-form-label" for="apiKey"></label>
                        <div class="col-sm-5">
                            <input type="text" id="apiKey" name="apiKey" class="form-control" required />
                        </div>
                </div>

                <!-- Use this model -->
                <div class="form-group row">
                        <label id="useThisModelLabel" class="col-sm-2 col-form-label" for="useThisModel"></label>
                        <div class="col-sm-5">
                            <input type="checkbox" id="useThisModel" name="useThisModel" data-height="36" data-toggle="toggle" data-onstyle="success" checked>
                        </div>
                </div>

            </div>
        </div>

		<hr>

        <!-- Vector field -->
        <div class="form-group row">
          <label id="vectorFieldLabel"
            class="col-sm-2 col-form-label" for="vectorField"></label>
          <div class="col-sm-5" id="vectorField-container">
            <input class="form-control" type="text" name="vectorField" id="vectorField" required />
          </div>
        </div>

        <hr>

        <!-- Chunking options : chunk size -->
        <div class="form-group row">
          <label id="chunkSizeLabel" for="chunkSize" class="col-sm-2 col-form-label">Chunk Size (tokens)</label>
          <div class="col-sm-5">
            <input class="form-control" type="number" min="1" name="chunkSize" id="chunkSize" />
          </div>
        </div>

        <!-- Chunking options : chunk size -->
        <div class="form-group row">
          <label id="maxOverlapLabel"  for="maxOverlap" class="col-sm-2 col-form-label">Max Overlap (tokens)</label>
          <div class="col-sm-5">
            <input class="form-control" type="number" min="0" name="maxOverlap" id="maxOverlap" />
          </div>
        </div>

        <!-- Chunking options : chunk size -->
        <div class="form-group row">
          <label id="splitterLabel" for="splitter" class="col-sm-2 col-form-label">Chunking Method</label>
          <div class="col-sm-5">
            <select class="form-control" name="splitter" id="splitter">
              <option value="recursiveSplitter">Recursive Splitter (recommended)</option>
              <option value="splitterByParagraph">Splitter by Paragraph</option>
              <option value="splitterBySentence">Splitter by Sentence</option>
              <option value="splitterByLine">Splitter by Line</option>
              <option value="splitterByCharacter">Splitter by Character</option>
            </select>
          </div>
        </div>

        <hr>

        <!-- Filter minChunkLength -->
        <div class="form-group row">
          <label id="minChunkLengthLabel" for="minChunkLength" class="col-sm-2 col-form-label">Min Chunk Length</label>
          <div class="col-sm-5">
            <input class="form-control" type="number" min="0" name="minChunkLength" id="minChunkLength" value="1" required />
          </div>
        </div>

        <!-- Filter minAlphaNumRatio -->
        <div class="form-group row">
          <label id="minAlphaNumRatioLabel" for="minAlphaNumRatio" class="col-sm-2 col-form-label">Min AlphaNum Ratio</label>
          <div class="col-sm-5">
            <input class="form-control" type="number" step="0.01" min="0" max="1" name="minAlphaNumRatio" id="minAlphaNumRatio" value="0.00" required />
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