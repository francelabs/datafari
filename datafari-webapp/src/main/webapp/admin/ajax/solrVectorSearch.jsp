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
          <span id="documentation-solrVectorSearch"></span>
                Datafari allows you to embed your indexed content to enable semantic (vector) search.
                From this page, you can manage embeddings generation and monitor its progress.
          <a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/3920297985"
            target="_blank"> ...see more</a>
        </p>
      </div>

      <!-- Main form -->
      <form class="form-horizontal" id="solrVectorSearch-form" class="needs-validation" novalidate>

        <!-- Progression -->
        <div class="form-group row">
          <label id="svs-progressbarLabel" class="col-sm-2 col-form-label" for="svs-progressbar">Embeddings progress</label>
          <div class="col-sm-5">
            <div id="svs-progress-container">
              <progress id="svs-progressbar" value="0" max="100">
              </progress>
              <div style="margin-top:5px;">
                <span id="svs-vcount">0</span> / <span id="svs-total">0</span>
                (<span id="svs-percent">0%</span>)
              </div>
            </div>
          </div>
          <div class="col-sm-3">
            <button id="svs-refresh" class="btn btn-secondary" type="button">
              <i class="fas fa-sync-alt"></i> Refresh
            </button>
          </div>
        </div>

        <hr>

        <!-- Launch embeddings -->
        <div class="form-group row">
          <label class="col-sm-2 col-form-label" for="svs-start" id="svs-startLabel">Embeddings generation</label>
          <div class="col-sm-4">
            <button class="btn btn-success" id="svs-start" type="button">
              <i class="fas fa-play"></i> Start embeddings
            </button><span id="svs-job-status"></span>
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="svs-force">
              <label class="form-check-label" for="svs-force">
                Force embeddings of all documents (limited to the ones already chunked). This includes already vectorised documents.
              </label>
            </div>
          </div>
        </div>

      </form>
    </div>
  </div>
</body>
</html>