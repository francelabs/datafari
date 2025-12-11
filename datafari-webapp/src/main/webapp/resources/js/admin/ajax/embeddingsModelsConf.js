function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['solrVectorSearch-AdminUI']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-embeddingsModels-AdminUI']);
  $("#box-title").text(window.i18n.msgStore['adminUI-embeddingsModels-AdminUI']);
  $("#documentation-embeddingsModels").text(window.i18n.msgStore['adminUI-embeddingsModels-doc']);

  // Active model selector
  $("#activeModelSelectLabel").text(window.i18n.msgStore['adminUI-embeddingsModels-activeModelSelectLabel']);

  // Table headers and actions
  $("#definedModelsLabel").text(window.i18n.msgStore['adminUI-embeddingsModels-definedModelsLabel']);
  $("#columnName").text(window.i18n.msgStore['adminUI-embeddingsModels-columnName']);
  $("#columnInterfaceType").text(window.i18n.msgStore['adminUI-embeddingsModels-columnInterfaceType']);
  $("#columnActions").text(window.i18n.msgStore['adminUI-embeddingsModels-columnActions']);
  $("#addModelButton").text(window.i18n.msgStore['adminUI-embeddingsModels-addModelButton']);

  // Form section
  $("#form-title").text(window.i18n.msgStore['adminUI-embeddingsModels-formTitle']);
  $("#modelNameLabel").text(window.i18n.msgStore['adminUI-embeddingsModels-modelNameLabel']);
  $("#interfaceTypeLabel").text(window.i18n.msgStore['adminUI-embeddingsModels-interfaceTypeLabel']);
  $("#vectorFieldLabel").text(window.i18n.msgStore['adminUI-embeddingsModels-vectorFieldLabel']);
  $("#saveModelButton").text(window.i18n.msgStore['adminUI-embeddingsModels-saveModelButton']);
  $("#cancelButton").text(window.i18n.msgStore['adminUI-embeddingsModels-cancelButton']);
  $("#testModelButton").text(window.i18n.msgStore['adminUI-embeddingsModels-testModelButton']);
}

let models = [];
let activeModel = null;
let editingIndex = null;

const API_URL = "/Datafari/rest/v2.0/management/embeddings-models";
const interfaceTypeMap = {
  OpenAI: "dev.langchain4j.model.openai.OpenAiEmbeddingModel",
  AIAgent: "dev.langchain4j.model.openai.OpenAiEmbeddingModel",
  MistralAI: "dev.langchain4j.model.mistralai.MistralAiEmbeddingModel",
  HuggingFace: "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel",
  Cohere: "dev.langchain4j.model.cohere.CohereEmbeddingModel"
}
const interfaceTypeMapReverse = {
  "dev.langchain4j.model.openai.OpenAiEmbeddingModel": "OpenAI",
  "dev.langchain4j.model.mistralai.MistralAiEmbeddingModel": "MistralAI",
  "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel": "HuggingFace",
  "dev.langchain4j.model.cohere.CohereEmbeddingModel": "Cohere"
}
const paramTemplates = {
  OpenAI: {
    baseUrl: "https://api.openai.com/v1/",
    apiKey: "",
    modelName: "text-embedding-3-small",
    timeout: 60,
    maxRetries: 3,
    logRequests: false,
    logResponses: false
  },
  AIAgent: {
    baseUrl: "http://localhost:8888/",
    apiKey: "xxx",
    modelName: "",
    timeout: 60,
    maxRetries: 3,
    logRequests: false,
    logResponses: false
  },
  MistralAI: {
    baseUrl: "https://api.mistral.ai/v1/",
    apiKey: "",
    modelName: "",
    timeout: 60,
    maxRetries: 3,
    logRequests: false,
    logResponses: false
  },
  HuggingFace: {
    accessToken: "",
    modelId: ""
  },
  Cohere: {
    baseUrl: "https://api.cohere.ai/v1/",
    apiKey: "",
    modelName: "",
    inputType: "search_document",
    timeout: 60,
    logRequests: true,
    logResponses: true
  }
};

$(document).ready(async function () {
  internationalize();
  await loadModels();
  bindForm();
});

// === Load and render ===
async function loadModels() {
  const response = await fetch(API_URL);
  const data = await response.json();
  models = data.models || [];
  activeModel = data.activeModel;
  renderModelTable();
  renderActiveModelSelect();
}

function renderModelTable() {
  const $tbody = $("#models-table-body").empty();

  $.each(models, function (i, model) {
    const $tr = $(`
      <tr>
        <td>${model.name}</td>
        <td>${interfaceTypeMapReverse[model.class] || ""}</td>
        <td>${model.vectorField|| ""}</td>
        <td>
          <button class="btn btn-sm btn-outline-primary" onclick="editModel(${i})" title="Edit">
            <i class="fas fa-pen"></i> Edit
          </button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteModel(${i})" title="Delete">
            <i class="fas fa-trash-alt"></i> Delete
          </button>
          <!-- TODO : TEST BUTTON-->
          <!--button class="btn btn-sm btn-outline-info" onclick="testModel(${i})" title="Test">
            <i class="fas fa-vial"></i> Test
          </button-->
        </td>
        <td class="test-result text-left" id="test-result-${i}" style="max-width: 300px;"></td>
      </tr>
    `);
    $tbody.append($tr);
  });
}

function renderActiveModelSelect() {
  const $select = $("#activeModelSelect").empty();

  $.each(models, function (_, model) {
    const $option = $("<option>").val(model.name).text(model.name);
    if (model.name === activeModel) $option.prop("selected", true);
    $select.append($option);
  });

  $select.off("change").on("change", function () {
    activeModel = $(this).val();
    saveAll();
  });
}

// === Editing ===
window.editModel = function (index) {
  editingIndex = index;
  const model = models[index];
  const displayType = interfaceTypeMapReverse[model.class] || "";

  $("#test-result").hide().text("");
  $("#form-title").text(`Edit model: ${model.name}`);
  $("#model-name").val(model.name);
  $("#vectorField").val(model.vectorField);
  $("#interface-type").val(displayType);

  generateParamFields(displayType, model.params || {});
  $("#model-form-container").slideDown();
};

window.showAddModelForm = function () {
  editingIndex = null;
  $("#test-result").hide().text("");
  $("#form-title").text("Add new model");
  $("#model-name").val("");
  $("#vectorField").val("");
  $("#interface-type").val("");
  $("#params-fields").empty();
  $("#model-form-container").slideDown();
};

window.cancelEdit = function () {
  $("#model-form-container").slideUp();
};

// === Form logic ===
function bindForm() {
  $("#interface-type").on("change", function () {
    const type = $(this).val();
    generateParamFields(type, paramTemplates[type] || {});
  });
}

window.submitModelForm = function () {
  const name = $("#model-name").val();
  const vectorField = $("#vectorField").val();
  var interfaceType = interfaceTypeMap[$("#interface-type").val()];

  const params = {};

  $("#params-fields input").each(function () {
    const $input = $(this);
    const key = $input.attr("name");
    if ($input.attr("type") === "checkbox") {
      params[key] = $input.prop("checked");
    } else if (!isNaN($input.val()) && $input.val() !== "") {
      params[key] = Number($input.val());
    } else {
      params[key] = $input.val();
    }
  });

  const model = { name, vectorField, class: interfaceType, params };

  if (editingIndex !== null) {
    models[editingIndex] = model;
  } else {
    models.push(model);
  }

  saveAll();
};

/*
 * Generates a dynamic for using fields defined in template
 */
function generateParamFields(type, params) {
  const defaults = paramTemplates[type] || {};
  const $container = $("#params-fields").empty();

  $.each(defaults, function (key, defValue) {
    const value = params[key] !== undefined ? params[key] : defValue;
    const i18nKey = `adminUI-chatLanguageModels-param-${key}`;
    const labelText = window.i18n?.msgStore?.[i18nKey] || key;

    const $formGroup = $("<div>").addClass("form-group row");

    const $label = $("<label>")
      .addClass("col-sm-2 col-form-label")
      .attr("for", `param-${key}`)
      .text(labelText);

    const $inputWrapper = $("<div>").addClass("col-sm-5");
    let $input;

    if (typeof defValue === "boolean") {
      $input = $("<input>")
        .attr("type", "checkbox")
        .attr("name", key)
        .attr("id", `param-${key}`)
        .addClass("form-check-input")
        .prop("checked", value);

      // Wrap checkbox in a form-check div
      const $checkDiv = $("<div>").addClass("form-check");
      const $checkboxLabel = $("<label>")
        .addClass("form-check-label")
        .attr("for", `param-${key}`)
        .text(labelText);

      $checkDiv.append($input).append($checkboxLabel);
      $inputWrapper.append($checkDiv);
      $formGroup.append($("<div>").addClass("col-sm-2")); // empty label col
      $formGroup.append($inputWrapper);
    } else {
      $input = $("<input>")
        .attr("type", "text")
        .attr("name", key)
        .attr("id", `param-${key}`)
        .addClass("form-control")
        .val(value);

      $inputWrapper.append($input);
      $formGroup.append($label).append($inputWrapper);
    }

    $container.append($formGroup);
  });
}

function normalizeModel(m) {
  const clazz = m.class || "";
  return { ...m, class: clazz };
}

// === Delete ===
window.deleteModel = function (index) {
  if (confirm("Are you sure you want to delete this model?")) {
    const deleted = models.splice(index, 1)[0];
    if (deleted.name === activeModel) {
      activeModel = models.length > 0 ? models[0].name : null;
    }

    const $resultCell = $(`#test-result-${index}`).text("⏳ Deleting...");


    // Call DELETE service
    $.ajax({
      url: `../rest/v2.0/management/embeddings-models?modelName=${encodeURIComponent(deleted.name)}`,
      type: 'DELETE',
      success: function (response) {
        showToast("✅ Model deleted");

        const resp = response && typeof response === 'string' ? JSON.parse(response) : response;

        console.log("resp", resp);
        console.log("resp.models", resp.models);

//        models = resp.models || [];
        models = (resp.models || []).map(normalizeModel);
        activeModel = resp.activeModel;
        console.log("models", models);
        console.log("activeModel", activeModel);

        // Reload form data
        renderModelTable();
        renderActiveModelSelect();
      },
      error: function () {
        $resultCell.text("❌ Deletion failed...");
        showToast("❌ Deletion failed", true);
      }
    });
  }
};

// === Save all ===
async function saveAll() {

/*
    if (!this.checkValidity()) {
      event.stopPropagation();
      $(this).addClass('was-validated');
      return;
    }
*/
    // Build JSON payload
    if (activeModel === null && models.length > 0) {
        activeModel = models[0].name;
    }
    const payload = JSON.stringify({ activeModel, models });
    console.log(activeModel);
    console.log(payload);

    // Loading spinner
    $('#saveModelButton').prop('disabled', true);
    $('#loadingIndicator').show();

    $.ajax({
        url: API_URL,
        type: 'POST',
        contentType: 'application/json',
        data: payload,
        success: function (response) {

          const resp = response && typeof response === 'string' ? JSON.parse(response) : response;

          $('#saveModelButton').prop('disabled', false);
          $('#loadingIndicator').hide();
          showToast("✅ Saved successfully");

          console.log("resp", resp);
          console.log("resp.models", resp.models);

//          models = resp.models || [];
          models = (resp.models || []).map(normalizeModel);
          activeModel = resp.activeModel;
          console.log("models", models);
          console.log("activeModel", activeModel);

          // Reload form data
          renderModelTable();
          renderActiveModelSelect();
          cancelEdit();
        },
        error: function (xhr) {
          console.log("error", xhr);
          $('#saveModelButton').prop('disabled', false);
          $('#loadingIndicator').hide();
          showToast("❌ Save failed", true);
        }
    });
}

// === Model test ===
window.testModel = async function (index) {
  const model = models[index];
  const $resultCell = $(`#test-result-${index}`).text("⏳ Testing...");

  try {
    const res = await fetch(`${API_URL}?test=true&model=${encodeURIComponent(model.name)}`);
    const txt = await res.text();
    $resultCell.text(`🤖 ${txt}`);
  } catch (e) {
    $resultCell.text(`❌ Failed ! ${e.message}`);
  }
};

// === Toast
function showToast(msg, isError = false) {
  if ($("#toast-message").length === 0) {
    $("body").append('<div id="toast-message"></div>');
  }

  const $toast = $("#toast-message");
  $toast
    .text(msg)
    .css({
      backgroundColor: isError ? "#c0392b" : "#27ae60",
      color: "#fff",
      padding: "10px 20px",
      borderRadius: "5px",
      position: "fixed",
      bottom: "20px",
      right: "20px",
      display: "none",
      zIndex: 1000
    })
    .fadeIn(200)
    .delay(2000)
    .fadeOut(600);
}