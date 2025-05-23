$(document).ready(function () {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-extraFunctionalities']);
  $("#topbar3").text(window.i18n.msgStore['solrVectorSearch-AdminUI']);
  $("#save-conf").text(window.i18n.msgStore['save']);
  $("#enableVectorSearchLabel").html(window.i18n.msgStore['solrVectorSearch-enableVectorSearchLabel']);
  $("#modelTemplateLabel").html(window.i18n.msgStore['solrVectorSearch-modelTemplateLabel']);
  $("#jsonModelLabel").html(window.i18n.msgStore['solrVectorSearch-jsonModelLabel']);
  $("#vectorFieldLabel").html(window.i18n.msgStore['solrVectorSearch-vectorFieldLabel']);
  $("#minChunkLengthLabel").html(window.i18n.msgStore['solrVectorSearch-minChunkLengthLabel']);
  $("#minAlphaNumRatioLabel").html(window.i18n.msgStore['solrVectorSearch-minAlphaNumRatioLabel']);

  $("#baseUrlLabel").html(window.i18n.msgStore['solrVectorSearch-baseUrlLabel']);
  $("#modelIdLabel").html(window.i18n.msgStore['solrVectorSearch-modelIdLabel']);
  $("#modelNameLabel").html(window.i18n.msgStore['solrVectorSearch-modelNameLabel']);
  $("#modelLabel").html(window.i18n.msgStore['solrVectorSearch-modelLabel']);
  $("#apiKeyLabel").html(window.i18n.msgStore['solrVectorSearch-apiKeyLabel']);
  $("#useThisModelLabel").html(window.i18n.msgStore['solrVectorSearch-useThisModelLabel']);
  $("#deleteModelLabel").html(window.i18n.msgStore['solrVectorSearch-deleteModelLabel']);
  $("#maxOverlapLabel").html(window.i18n.msgStore['solrVectorSearch-maxOverlapLabel']);
  $("#chunkSizeLabel").html(window.i18n.msgStore['solrVectorSearch-chunkSizeLabel']);
  $("#splitterLabel").html(window.i18n.msgStore['solrVectorSearch-splitterLabel']);


  $('#enableVectorSearch').bootstrapToggle();
  $('#useThisModel').bootstrapToggle();

  const defaultModelValues = {
    openai: {
      baseUrl: "https://api.openai.com/v1",
      modelName: "text-embedding-3-small",
      apiKey: ""
    },
    huggingface: {
      baseUrl: "https://huggingface.co",
      modelName: "sentence-transformers/all-MiniLM-L6-v2",
      apiKey: ""
    },
    mistral: {
      baseUrl: "https://api.mistral.ai/v1",
      modelName: "mistral-embed",
      apiKey: ""
    },
    cohere: {
      baseUrl: "https://api.cohere.ai/v1/",
      modelName: "embed-english-v3.0",
      apiKey: ""
    },
    aiagent: {
      baseUrl: "http://localhost:8888/",
      modelName: "all-MiniLM-L6-v2.Q8_0.gguf",
      apiKey: "xxx"
    }
  };

  // JSON templates with placeholders
  const templates = {
    openai: `{
  "class": "dev.langchain4j.model.openai.OpenAiEmbeddingModel",
  "name": "{model}",
  "params": {
    "baseUrl": "{baseUrl}",
    "apiKey": "{apiKey}",
    "modelName": "{modelName}",
    "timeout": 60,
    "logRequests": true,
    "logResponses": true,
    "maxRetries": 5
  }
}`,
    aiagent: `{
  "class": "dev.langchain4j.model.openai.OpenAiEmbeddingModel",
  "name": "{model}",
  "params": {
    "baseUrl": "{baseUrl}",
    "apiKey": "{apiKey}",
    "modelName": "{modelName}",
    "timeout": 60,
    "logRequests": true,
    "logResponses": true,
    "maxRetries": 5
  }
}`,
    huggingface: `{
  "class": "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel",
  "name": "{model}",
  "params": {
    "accessToken": "{apiKey}",
    "modelId": "{modelName}"
  }
}`,
    mistral: `{
  "class": "dev.langchain4j.model.mistralai.MistralAiEmbeddingModel",
  "name": "{model}",
  "params": {
    "baseUrl": "{baseUrl}",
    "apiKey": "{apiKey}",
    "modelName": "{modelName}",
    "timeout": 60,
    "logRequests": true,
    "logResponses": true,
    "maxRetries": 5
  }
}`,
    cohere: `{
  "class": "dev.langchain4j.model.cohere.CohereEmbeddingModel",
  "name": "{model}",
  "params": {
    "baseUrl": "{baseUrl}",
    "apiKey": "{apiKey}",
    "modelName": "{modelName}",
    "inputType": "search_document",
    "timeout": 60,
    "logRequests": true,
    "logResponses": true
  }
}`
  };

  const modelMappings = {
    openai: "dev.langchain4j.model.openai.OpenAiEmbeddingModel",
    huggingface: "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel",
    mistral: "dev.langchain4j.model.mistralai.MistralAiEmbeddingModel",
    cohere: "dev.langchain4j.model.cohere.CohereEmbeddingModel",
    aiagent: "dev.langchain4j.model.openai.OpenAiEmbeddingModel"
  };

  function selectActiveModelOrFallback(data) {
    const modelSelect = $('#model');
    const active = data.modelName;
    const exists = modelSelect.find(`option[value="${active}"]`).length > 0;

    if (active && exists) {
      modelSelect.val(active).trigger('change');
    } else {
      modelSelect.val("new").trigger('change');
    }
  }

  function loadFormData() {
    $.get("../rest/v2.0/management/solrvectorsearch", function (data) {
        // List of existing models
        const modelSelect = $('#model');
        modelSelect.empty();
        modelSelect.append($('<option>', { value: "new", text: "Add a new embeddings model" }));

        if (data.models) {
          data.models.forEach(model => {
            modelSelect.append($('<option>', {
              value: model.name,
              text: model.name
            }));
          });
        }

        // Check or uncheck "Enable Vector Search"
        $("#enableVectorSearch").prop("checked", data.enableVectorSearch === true).change();


        // Retrieving and updating vector fields list
        if (data.availableFields && Array.isArray(data.availableFields)) {
          const select = $('<select>', {
            class: 'form-control',
            name: 'vectorField',
            id: 'vectorField'
          });

          data.availableFields.forEach(function (field) {
            const option = $('<option>', {
              value: field,
              text: field
            });
            if (field === data.vectorField) {
              option.prop('selected', true);
            }
            select.append(option);
          });

          $('#vectorField-container').html(select);
        } else {
          $('#vectorField-container').html(
            $('<input>', {
              class: 'form-control',
              type: 'text',
              name: 'vectorField',
              id: 'vectorField',
              val: data.vectorField || '',
              required: true
            })
          );
        }

        // Set existing values
        $("#jsonModel").val(data.jsonModel);
        $("#modelTemplate").val(getTemplateKeyFromClass(data.modelTemplate)).trigger('change');
        $("#minChunkLength").val(data.minChunkLength);
        $("#minAlphaNumRatio").val(data.minAlphaNumRatio);
        $("#maxOverlap").val(data.maxoverlap || 0);
        $("#chunkSize").val(data.chunksize || 300);
        $("#splitter").val(data.splitter || "recursiveSplitter");

        // On model change
        $('#model').on('change', function () {
          const selectedModel = $(this).val();
          if (selectedModel === "new") {
            // New model
            $('#deleteModelContainer').hide();
            $('#modelId').val("default_model").prop("readonly", false);
            $('#modelName').val("");
            $('#baseUrl').val("");
            $('#apiKey').val("");
            $('#useThisModel').prop('checked', true).change();
            $('#modelTemplate').val("openai").trigger('change');
            updateJsonModel();
          } else {
            const selected = data.models.find(m => m.name === selectedModel);
            if (selected) {
              $('#deleteModelContainer').show();
              $('#modelTemplate').val(getTemplateKeyFromClass(selected.class)).trigger('change');
              $('#modelId').val(selected.name).prop("readonly", true);
              $('#modelName').val(selected.params.modelName || selected.params.modelId || "");
              $('#baseUrl').val(selected.params.baseUrl || "");
              $('#apiKey').val(selected.params.apiKey || selected.params.accessToken || "");
              $('#useThisModel').prop('checked', selected.name === data.modelName).change();
              updateJsonModel();
            }
          }
        });

        // Si un modèle par défaut est défini, on le sélectionne automatiquement
        selectActiveModelOrFallback(data);
      }, "json");
  }

  function getTemplateKeyFromClass(className) {
    if (className == null || className.includes("openai")) return "openai";
    if (className.includes("huggingface")) return "huggingface";
    if (className.includes("mistral")) return "mistral";
    if (className.includes("cohere")) return "cohere";
    return "openai"; // fallback
  }

  function setModelIdEditable(isEditable) {
    $('#modelId').prop('readonly', !isEditable);
  }

  function updateJsonModel() {
    const templateKey = $('#modelTemplate').val();
    if (!templateKey || !(templateKey in templates)) {
      console.warn("No valid template selected for modelTemplate:", templateKey);
      $('#jsonModel').val(""); // Vide le champ
      return;
    }
    const template = templates[templateKey];
    const className = modelMappings[templateKey];
    let json = templates[templateKey];
    const replacements = {
      "{model}": $('#modelId').val(),
      "{baseUrl}": $('#baseUrl').val(),
      "{apiKey}": $('#apiKey').val(),
      "{modelName}": $('#modelName').val()
    };

    for (const [key, val] of Object.entries(replacements)) {
      json = json.replaceAll(key, val || "");
    }

    $('#jsonModel').val(json);
  }

  // Auto-update JSON on blur
  $('#model, #modelId, #baseUrl, #apiKey, #modelName').on('blur change', updateJsonModel);

  $('#modelTemplate').on('change', function () {
    const selected = $(this).val();
    // Pre-fill fields with default template config
    const defaults = defaultModelValues[selected] || {};
    $('#baseUrl').val(defaults.baseUrl || "");
    $('#modelName').val(defaults.modelName || "");
    $('#apiKey').val(defaults.apiKey || "");

    updateJsonModel(); // Update textarea JSON
  });

  // Loading data from backend
  loadFormData();


  // Model deletion
  $('#deleteModel').on('click', function () {
    const modelName = $('#modelId').val();

    if (!confirm(`Are you sure you want to delete the model "${modelName}"?`)) {
      return;
    }

    // Call DELETE service
    $.ajax({
      url: `../rest/v2.0/management/solrvectorsearch?modelName=${encodeURIComponent(modelName)}`,
      type: 'DELETE',
      success: function (response) {
        $('#message')
          .removeClass('alert-danger')
          .addClass('alert-success')
          .text("Model deleted successfully")
          .show();
        loadFormData();
      },
      error: function () {
        $('#message')
          .removeClass('alert-success')
          .addClass('alert-danger')
          .text("Error deleting the model.")
          .show();
      }
    });
  });

  // Form validation
  $('#solrVectorSearch-form').on('submit', function (event) {
    event.preventDefault();

    if (!this.checkValidity()) {
      event.stopPropagation();
      $(this).addClass('was-validated');
      return;
    }

    // Construire la payload JSON
    const payload = {
      enableVectorSearch: $('#enableVectorSearch').is(':checked'),
      minChunkLength: parseInt($('#minChunkLength').val(), 10),
      minAlphaNumRatio: parseFloat($('#minAlphaNumRatio').val()),
      vectorField: $('#vectorField').val(),
      jsonModel: $('#jsonModel').val(),
      useThisModel: $('#useThisModel').is(':checked'),
      maxoverlap: parseInt($('#maxOverlap').val(), 10),
      chunksize: parseInt($('#chunkSize').val(), 10),
      splitter: $('#splitter').val()
    };

    // Loading spinner
    $('#save-conf').prop('disabled', true);
    $('#loadingIndicator').show();

    $.ajax({
        url: '../rest/v2.0/management/solrvectorsearch',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (response) {
          $('#save-conf').prop('disabled', false);
          $('#loadingIndicator').hide();
          $('#message')
            .removeClass('alert-danger')
            .addClass('alert-success')
            .text("Configuration saved successfully")
            .show();

          // Reload form data
          loadFormData();
        },
        error: function (xhr) {
          $('#save-conf').prop('disabled', false);
          $('#loadingIndicator').hide();
          $('#message')
            .removeClass('alert-success')
            .addClass('alert-danger')
            .text("An error occurred while saving configuration.")
            .show();
        }
    });
    setTimeout(() => {
      $('#message').fadeOut();
    }, 4000);

  });
});