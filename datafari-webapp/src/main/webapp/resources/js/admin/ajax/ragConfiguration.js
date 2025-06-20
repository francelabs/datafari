function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-extraFunctionalities']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-ragConf']);

  $("#enableRagLabel").text(window.i18n.msgStore['ragConf-enableRagLabel']);
  $("#enableSummarizationLabel").text(window.i18n.msgStore['ragConf-enableSummarizationLabel']);
  $("#solrTopKLabel").text(window.i18n.msgStore['ragConf-solrTopKLabel']);
  $("#rrfTopKLabel").text(window.i18n.msgStore['ragConf-rrfTopKLabel']);
  $("#rrfRankConstantLabel").text(window.i18n.msgStore['ragConf-rrfRankConstantLabel']);
  $("#solrEmbeddingsModelLabel").text(window.i18n.msgStore['ragConf-solrEmbeddingsModelLabel']);
  $("#ragOperatorLabel").text(window.i18n.msgStore['ragConf-ragOperatorLabel']);
  $("#chunkingMaxFilesLabel").text(window.i18n.msgStore['ragConf-chunkingMaxFilesLabel']);
  $("#retrievalMethodLabel").text(window.i18n.msgStore['ragConf-retrievalMethodLabel']);
  $("#chatMemoryHistorySizeLabel").text(window.i18n.msgStore['ragConf-chatMemoryHistorySizeLabel']);
  $("#chatMemoryEnabledLabel").text(window.i18n.msgStore['ragConf-chatMemoryEnabledLabel']);
  $("#chatQueryRewritingEnabledLabel").text(window.i18n.msgStore['ragConf-chatQueryRewritingEnabledLabel']);
  $("#chunkingChunkSizeLabel").text(window.i18n.msgStore['ragConf-chunkingChunkSizeLabel']);
  $("#maxRequestSizeLabel").text(window.i18n.msgStore['ragConf-maxRequestSizeLabel']);
  $("#chunkingStrategyLabel").text(window.i18n.msgStore['ragConf-chunkingStrategyLabel']);
  $("#llmMaxTokensLabel").text(window.i18n.msgStore['ragConf-llmMaxTokensLabel']);
  $("#llmTemperatureLabel").text(window.i18n.msgStore['ragConf-llmTemperatureLabel']);
  $("#llmModelLabel").text(window.i18n.msgStore['ragConf-llmModelLabel']);
  $("#llmServiceLabel").text(window.i18n.msgStore['ragConf-llmServiceLabel']);
  $("#apiTokenLabel").text(window.i18n.msgStore['ragConf-apiTokenLabel']);
  $("#apiEndpointLabel").text(window.i18n.msgStore['ragConf-apiEndpointLabel']);

}

function loadRagConfig() {
  $.get("../rest/v2.0/management/ragConfig", function (data) {
    $('#enableRag').prop('checked', data.enableRag === true).change();
    $('#enableSummarization').prop('checked', data.enableSummarization === true).change();

    $('#apiEndpoint').val(data.apiEndpoint || '');
    $('#apiToken').val(data.apiToken || '');
    $('#llmService').val(data.llmService || 'openai');

    $('#llmModel').val(data.llmModel || '');
    $('#llmTemperature').val(data.llmTemperature || 0);
    $('#llmMaxTokens').val(data.llmMaxTokens || 200);

    $('#chunkingStrategy').val(data.chunkingStrategy || 'refine');
    $('#maxRequestSize').val(data.maxRequestSize || 40000);

    $('#chunkingMaxFiles').val(data.chunkingMaxFiles || 3);
    $('#chunkingChunkSize').val(data.chunkingChunkSize || 3000);
    $('#ragOperator').val(data.ragOperator || 'OR');

    $('#chatQueryRewritingEnabled').prop('checked', data.chatQueryRewritingEnabled === true).change();
    $('#chatMemoryEnabled').prop('checked', data.chatMemoryEnabled === true).change();
    $('#chatMemoryHistorySize').val(data.chatMemoryHistorySize || 6);

    $('#retrievalMethod').val(data.retrievalMethod || "bm25");
    updateRetrievalVisibility();
    $('#solrEmbeddingsModel').val(data.solrEmbeddingsModel || '');
    $('#solrVectorField').val(data.solrVectorField || '');
    $('#solrTopK').val(data.solrTopK || 10);
    $('#rrfTopK').val(data.rrfTopK || 50);
    $('#rrfRankConstant').val(data.rrfRankConstant || 60);

  }, "json");
}


function updateRetrievalVisibility() {
  const value = $('#retrievalMethod').val();

  if (value === "vector") {
    $('.bm25Only').hide();
    $('.rrfOnly').hide();
    $('.vectorSearchOnly').show();
  } else if (value === "rrf") {
    $('.bm25Only').hide();
    $('.vectorSearchOnly').hide();
    $('.rrfOnly').show();
  } else {
    $('.rrfOnly').hide();
    $('.vectorSearchOnly').hide();
    $('.bm25Only').show();
  }
}

function submitRagConfig(event) {
  event.preventDefault();

  if (!$('#ragCong-form')[0].checkValidity()) {
    $('#ragCong-form').addClass('was-validated');
    return;
  }

  $('#save-conf').prop('disabled', true);
  $('#loadingIndicator').show();

  const payload = {
    "ai.enable.rag": $('#enableRag').is(':checked'),
    "ai.enable.summarization": $('#enableSummarization').is(':checked'),
    "ai.api.endpoint": $('#apiEndpoint').val(),
    "ai.api.token": $('#apiToken').val(),
    "ai.llm.service": $('#llmService').val(),
    "llm.model": $('#llmModel').val(),
    "llm.temperature": $('#llmTemperature').val(),
    "llm.maxTokens": $('#llmMaxTokens').val(),
    "prompt.chunking.strategy": $('#chunkingStrategy').val(),
    "prompt.max.request.size": $('#maxRequestSize').val(),
    "chunking.maxFiles": $('#chunkingMaxFiles').val(),
    "chunking.chunk.size": $('#chunkingChunkSize').val(),
    "rag.operator": $('#ragOperator').val(),
    "chat.query.rewriting.enabled": $('#chatQueryRewritingEnabled').is(':checked'),
    "chat.memory.enabled": $('#chatMemoryEnabled').is(':checked'),
    "chat.memory.history.size": $('#chatMemoryHistorySize').val(),
    "solr.topK": $('#solrTopK').val(),
    "rrf.topK": $('#rrfTopK').val(),
    "rrf.rank.constant": $('#rrfRankConstant').val(),
    "retrieval.method": $('#retrievalMethod').val(),
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

  $('#enableRag').bootstrapToggle();
  $('#enableSummarization').bootstrapToggle();
  $('#chatMemoryEnabled').bootstrapToggle();
  $('#chatQueryRewritingEnabled').bootstrapToggle();
  $('#retrievalMethod').on('change', updateRetrievalVisibility);

  $('#ragCong-form').on('submit', submitRagConfig);
});