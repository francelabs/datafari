function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-aiMenu']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-ragConf']);

//  $("#enableRagLabel").text(window.i18n.msgStore['ragConf-enableRagLabel']);
//  $("#enableAgenticLabel").text(window.i18n.msgStore['ragConf-enableAgenticLabel']);
//  $("#enableSummarizationLabel").text(window.i18n.msgStore['ragConf-enableSummarizationLabel']);
//  $("#enableSynthesisLabel").text(window.i18n.msgStore['ragConf-enableSynthesisLabel']);
  $("#ragTopKLabel").text(window.i18n.msgStore['ragConf-ragTopKLabel']);
  $("#rrfTopKLabel").text(window.i18n.msgStore['ragConf-rrfTopKLabel']);
  $("#rrfRankConstantLabel").text(window.i18n.msgStore['ragConf-rrfRankConstantLabel']);
  $("#solrEmbeddingsModelLabel").text(window.i18n.msgStore['ragConf-solrEmbeddingsModelLabel']);
  $("#ragOperatorLabel").text(window.i18n.msgStore['ragConf-ragOperatorLabel']);
  $("#chunkingMaxFilesLabel").text(window.i18n.msgStore['ragConf-chunkingMaxFilesLabel']);
  $("#retrievalMethodLabel").text(window.i18n.msgStore['ragConf-retrievalMethodLabel']);
  $("#chatMemoryHistorySizeLabel").text(window.i18n.msgStore['ragConf-chatMemoryHistorySizeLabel']);
  $("#chatMemoryEnabledLabel").text(window.i18n.msgStore['ragConf-chatMemoryEnabledLabel']);
  $("#chatQueryRewritingEnabledLabel").text(window.i18n.msgStore['ragConf-chatQueryRewritingEnabledLabel']);
  $("#chatQueryRewritingEnabledBM25Label").text(window.i18n.msgStore['ragConf-chatQueryRewritingEnabledBM25Label']);
  $("#chatQueryRewritingEnabledVectorLabel").text(window.i18n.msgStore['ragConf-chatQueryRewritingEnabledVectorLabel']);
  $("#chunkingChunkSizeLabel").text(window.i18n.msgStore['ragConf-chunkingChunkSizeLabel']);
  $("#maxRequestSizeLabel").text(window.i18n.msgStore['ragConf-maxRequestSizeLabel']);
  $("#chunkingStrategyLabel").text(window.i18n.msgStore['ragConf-chunkingStrategyLabel']);
  $("#enableLoopControlLabel").text(window.i18n.msgStore['ragConf-enableLoopControlLabel']);
}

function loadRagConfig() {
  $.get("../rest/v2.0/management/ragConfig", function (data) {
//    $('#enableRag').prop('checked', data.enableRag === true).change();
//    $('#enableAgentic').prop('checked', data.enableAgentic === true).change();
//    $('#enableSummarization').prop('checked', data.enableSummarization === true).change();
//    $('#enableSynthesis').prop('checked', data.enableSynthesis === true).change();

//    $('#apiEndpoint').val(data.apiEndpoint || '');
//    $('#apiToken').val(data.apiToken || '');
//    $('#llmService').val(data.llmService || 'openai');
//
//    $('#llmModel').val(data.llmModel || '');
//    $('#llmTemperature').val(data.llmTemperature || 0);
//    $('#llmMaxTokens').val(data.llmMaxTokens || 200);

    $('#chunkingStrategy').val(data.chunkingStrategy || 'refine');
    $('#maxRequestSize').val(data.maxRequestSize || 40000);

    $('#chunkingMaxFiles').val(data.chunkingMaxFiles || 3);
    $('#chunkingChunkSize').val(data.chunkingChunkSize || 3000);
    $('#ragOperator').val(data.ragOperator || 'OR');

    $('#chatQueryRewritingEnabledBM25').prop('checked', data.chatQueryRewritingEnabledBM25 === true).change();
    $('#chatQueryRewritingEnabledVector').prop('checked', data.chatQueryRewritingEnabledVector === true).change();
    $('#chatMemoryEnabled').prop('checked', data.chatMemoryEnabled === true).change();
    $('#enableLoopControl').prop('checked', data.enableLoopControl === true).change();
    $('#chatMemoryHistorySize').val(data.chatMemoryHistorySize || 6);

    $('#retrievalMethod').val(data.retrievalMethod || "bm25");
    updateRetrievalVisibility();
    $('#solrEmbeddingsModel').val(data.solrEmbeddingsModel || '');
    $('#solrVectorField').val(data.solrVectorField || '');
    $('#ragTopK').val(data.ragTopK || 10);
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

  if (!$('#ragConf-form')[0].checkValidity()) {
    $('#ragConf-form').addClass('was-validated');
    return;
  }

  $('#save-conf').prop('disabled', true);
  $('#loadingIndicator').show();

  const payload = {
//    "ai.enable.rag": $('#enableRag').is(':checked'),
//    "ai.enable.agentic": $('#enableAgentic').is(':checked'),
//    "ai.enable.summarization": $('#enableSummarization').is(':checked'),
//    "ai.enable.synthesis": $('#enableSynthesis').is(':checked'),
    "prompt.chunking.strategy": $('#chunkingStrategy').val(),
    "prompt.max.request.size": $('#maxRequestSize').val(),
    "chunking.maxFiles": $('#chunkingMaxFiles').val(),
    "chunking.chunk.size": $('#chunkingChunkSize').val(),
    "rag.operator": $('#ragOperator').val(),
    "chat.query.rewriting.enabled.bm25": $('#chatQueryRewritingEnabledBM25').is(':checked'),
    "chat.query.rewriting.enabled.vector": $('#chatQueryRewritingEnabledVector').is(':checked'),
    "chat.memory.enabled": $('#chatMemoryEnabled').is(':checked'),
    "chat.memory.history.size": $('#chatMemoryHistorySize').val(),
    "rag.topK": $('#ragTopK').val(),
    "rrf.topK": $('#rrfTopK').val(),
    "rrf.rank.constant": $('#rrfRankConstant').val(),
    "retrieval.method": $('#retrievalMethod').val(),
    "agentic.enable.loop.control": $('#enableLoopControl').is(':checked'),
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

//  $('#enableRag').bootstrapToggle();
//  $('#enableAgentic').bootstrapToggle();
//  $('#enableSummarization').bootstrapToggle();
//  $('#enableSynthesis').bootstrapToggle();
  $('#chatMemoryEnabled').bootstrapToggle();
  $('#chatQueryRewritingEnabledBM25').bootstrapToggle();
  $('#chatQueryRewritingEnabledVector').bootstrapToggle();
  $('#enableLoopControl').bootstrapToggle();
  $('#retrievalMethod').on('change', updateRetrievalVisibility);

  $('#ragConf-form').on('submit', submitRagConfig);
});