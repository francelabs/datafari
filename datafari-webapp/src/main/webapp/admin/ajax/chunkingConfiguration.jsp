<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<script
  src="<c:url value="/resources/js/admin/ajax/chunking.js" />"
  type="text/javascript"></script>
<link rel="stylesheet" type="text/css"
  href="<c:url value="/resources/css/admin/solrVectorSearch.css" />" />
<meta charset="UTF-8">
<title>Chunking Configuration</title>
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
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3920297985"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="chunking-form" class="needs-validation" novalidate>

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