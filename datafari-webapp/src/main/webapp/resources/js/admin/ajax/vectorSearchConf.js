function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-aiMenu']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-vectorSearchConf']);

  $("#enableAcornLabel").text(window.i18n.msgStore['vectorSearchConf-enableAcornLabel']);
  $("#filteredSearchThresholdLabel").text(window.i18n.msgStore['vectorSearchConf-filteredSearchThresholdLabel']);
  $("#enableLadrLabel").text(window.i18n.msgStore['vectorSearchConf-enableLadrLabel']);
  $("#solrTopKLabel").text(window.i18n.msgStore['vectorSearchConf-solrTopKLabel']);
  $("#rrfTopKLabel").text(window.i18n.msgStore['vectorSearchConf-rrfTopKLabel']);
  $("#rrfRankConstantLabel").text(window.i18n.msgStore['vectorSearchConf-rrfRankConstantLabel']);

  $('#enableAcornTooltip')
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='ACORN is an algorithm designed to make hybrid searches consisting of a filter and a vector search more efficient. This approach tackles both the performance limitations of pre- and post- filtering. It modifies the construction of the HNSW graph and the search on it.'>i</button></span>");
  $('#filteredSearchThresholdTooltip')
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='If the percentage of documents that satisfies the filter is less than the threshold ACORN will be used. From 0 (never use ACORN) to 100 (always use ACORN)'>i</button></span>");
  $('#enableLadrTooltip')
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Use SeededKnnVectorQuery to initiate the entry points in the HNSW graph with a seedQuery, in order to improve the relevancy of the results.'>i</button></span>");

}

function loadRagConfig() {
  $.get("../rest/v2.0/management/ragConfig", function (data) {
    $('#enableLadr').prop('checked', data.enableLadr === true).change();
    $('#enableAcorn').prop('checked', data.enableAcorn === true).change();
    $('#filteredSearchThreshold').val(data.solrFilteredSearchThreshold || 60);

    $('#solrTopK').val(data.solrTopK || 10);
    $('#rrfTopK').val(data.rrfTopK || 50);
    $('#rrfRankConstant').val(data.rrfRankConstant || 60);

  }, "json");
}

function submitRagConfig(event) {
  event.preventDefault();

  if (!$('#vectorSearchConf-form')[0].checkValidity()) {
    $('#vectorSearchConf-form').addClass('was-validated');
    return;
  }

  $('#save-conf').prop('disabled', true);
  $('#loadingIndicator').show();

  const payload = {
    "solr.enable.ladr": $('#enableLadr').is(':checked'),
    "solr.enable.acorn": $('#enableAcorn').is(':checked'),
    "solr.filtered.search.threshold": $('#filteredSearchThreshold').val(),
    "solr.topK": $('#solrTopK').val(),
    "rrf.topK": $('#rrfTopK').val(),
    "rrf.rank.constant": $('#rrfRankConstant').val()
  };

  $.ajax({
    url: "../rest/v2.0/management/ragConfig",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload),
    success: function () {
      $('#message')
        .removeClass('alert-danger')
        .addClass('alert-success')
        .text("Configuration saved successfully")
        .show();
      setTimeout(() => $('#message').fadeOut(), 3000);
    },
    error: function () {
      $('#message')
        .removeClass('alert-success')
        .addClass('alert-danger')
        .text("An error occurred while saving configuration.")
        .show();
    },
    complete: function () {
      $('#save-conf').prop('disabled', false);
      $('#loadingIndicator').hide();
    }
  });
}

$(document).ready(function () {
  internationalize();
  loadRagConfig();

  $('#enableLadr').bootstrapToggle();
  $('#enableAcorn').bootstrapToggle();

  $('#vectorSearchConf-form').on('submit', submitRagConfig);
});