
(function () {
  "use strict";

  // Internationalization
  function initI18n() {
    $("#topbar1").text(window.i18n.msgStore['home']);
    $("#topbar2").text(window.i18n.msgStore['solrVectorSearch-AdminUI']);
    $("#topbar3").text(window.i18n.msgStore['adminUI-vectorSearchMenu-solrVectorSearch']);
    $("#svs-refresh").html('<i class="fas fa-sync-alt"></i> ' + window.i18n.msgStore['solrVectorSearch-refreshLabel']);
    $("#svs-start").html('<i class="fas fa-play"></i> ' + window.i18n.msgStore['solrVectorSearch-startEmbeddingsLabel']);
    $("#svs-startLabel").html(window.i18n.msgStore['solrVectorSearch-startLabel']);
    $("#svs-progressbarLabel").html(window.i18n.msgStore['solrVectorSearch-progressbarLabel']);
    $("#title").text(window.i18n.msgStore['adminUI-vectorSearchMenu-solrVectorSearch']);
  }

  // Config
  var API = "/Datafari/rest/v2.0/management/solr-vector-search";

  function setProgress(vecCount, total) {
    var p = (!total || total <= 0) ? 0 : Math.floor((vecCount / total) * 100);
    $("#svs-vcount").text(vecCount || 0);
    $("#svs-total").text(total || 0);
    $("#svs-percent").text(p + "%");

    var $pb = $("#svs-progressbar");
    $pb.attr("value", p);
    $pb.attr("max", 100);
    $pb.text(p + "%");
  }

  function refreshStatus() {
    console.log("refreshStatus");
    return $.getJSON(API + "?fn=status")
      .then(function (res) {
        setProgress(res.vectorizedDocs || 0, res.totalDocs || 0);
      })
      .fail(function (xhr) {
        alert("Failed to refresh status: " + (xhr.responseJSON && xhr.responseJSON.message || xhr.statusText));
      });
  }

  function startEmbeddings() {
    var force = $("#svs-force").is(":checked");

    return $.ajax({
      url: API + "?fn=startEmbeddings",
      method: "POST",
      contentType: "application/json",
      data: JSON.stringify({ force: force })
    })
      .done(function () {
        console.log("startEmbeddings", "done");
        $("#svs-job-status").text(window.i18n.msgStore['solrVectorSearch-jobStarted']);
      })
      .fail(function (xhr) {
        alert("Failed to start embeddings: " + (xhr.responseJSON && xhr.responseJSON.message || xhr.statusText));
      });
  }

  // ---------- Init ----------
  $(document).ready(function () {
    initI18n();

    // Bind events
    $("#svs-refresh").on("click", function (e) { e.preventDefault(); refreshStatus(); });
    $("#svs-start").on("click", function (e) { e.preventDefault(); startEmbeddings(); });

    refreshStatus();
  });
})();