function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-aiMenu']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-assistantConf']);

  $("#enableAssistantLabel").text(window.i18n.msgStore['assistantConf-enableAssistantLabel']);
  $("#enableRagLabel").text(window.i18n.msgStore['assistantConf-enableRagLabel']);
  $("#enableAgenticLabel").text(window.i18n.msgStore['assistantConf-enableAgenticLabel']);
  $("#enableSummarizationLabel").text(window.i18n.msgStore['assistantConf-enableSummarizationLabel']);
  $("#enableSynthesisLabel").text(window.i18n.msgStore['assistantConf-enableSynthesisLabel']);
  $("#assistantRetrievalMethodLabel").text(window.i18n.msgStore['assistantConf-assistantRetrievalMethodLabel']);
  $("#enableConversationStorageLabel").text(window.i18n.msgStore['assistantConf-enableConversationStorageLabel']);
}

function loadAssistantConfig() {
  $.get("../rest/v2.0/management/ragConfig", function (data) {
    $('#enableAssistant').prop('checked', data.enableAssistant === true).change();
    $('#enableRag').prop('checked', data.enableRag === true).change();
    $('#enableAgentic').prop('checked', data.enableAgentic === true).change();
    $('#enableSummarization').prop('checked', data.enableSummarization === true).change();
    $('#enableSynthesis').prop('checked', data.enableSynthesis === true).change();
    $('#enableConversationStorage').prop('checked', data.enableConversationStorage === true).change();

    $('#assistantRetrievalMethod').val(data.assistantRetrievalMethod || "bm25");

  }, "json");
}

function submitAssistantConfig(event) {
  event.preventDefault();

  if (!$('#assistantConf-form')[0].checkValidity()) {
    $('#assistantConf-form').addClass('was-validated');
    return;
  }

  $('#save-conf').prop('disabled', true);
  $('#loadingIndicator').show();

  const payload = {
    "assistant.enable.assistant": $('#enableAssistant').is(':checked'),
    "ai.enable.rag": $('#enableRag').is(':checked'),
    "ai.enable.agentic": $('#enableAgentic').is(':checked'),
    "ai.enable.summarization": $('#enableSummarization').is(':checked'),
    "ai.enable.synthesis": $('#enableSynthesis').is(':checked'),
    "assistant.enable.conversation.storage": $('#enableConversationStorage').is(':checked'),
    "assistant.retrieval.method": $('#assistantRetrievalMethod').val(),
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
  loadAssistantConfig();

  $('#enableAssistant').bootstrapToggle();
  $('#enableRag').bootstrapToggle();
  $('#enableAgentic').bootstrapToggle();
  $('#enableSummarization').bootstrapToggle();
  $('#enableSynthesis').bootstrapToggle();
  $('#enableConversationStorage').bootstrapToggle();

  $('#assistantConf-form').on('submit', submitAssistantConfig);
});

