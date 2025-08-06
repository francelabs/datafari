function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['aiMenu-AdminUI']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-chatLanguageModels-AdminUI']);
  $("#box-title").text(window.i18n.msgStore['adminUI-chatLanguageModels-AdminUI']);
  $("#documentation-chatLanguageModels").text(window.i18n.msgStore['adminUI-chatLanguageModels-doc']);

  // Active model selector
  $("#activeModelSelectLabel").text(window.i18n.msgStore['adminUI-chatLanguageModels-activeModelSelectLabel']);

  // Table headers and actions
  $("#definedModelsLabel").text(window.i18n.msgStore['adminUI-chatLanguageModels-definedModelsLabel']);
  $("#columnName").text(window.i18n.msgStore['adminUI-chatLanguageModels-columnName']);
  $("#columnInterfaceType").text(window.i18n.msgStore['adminUI-chatLanguageModels-columnInterfaceType']);
  $("#columnActions").text(window.i18n.msgStore['adminUI-chatLanguageModels-columnActions']);
  $("#addModelButton").text(window.i18n.msgStore['adminUI-chatLanguageModels-addModelButton']);

  // Form section
  $("#form-title").text(window.i18n.msgStore['adminUI-chatLanguageModels-formTitle']);
  $("#modelNameLabel").text(window.i18n.msgStore['adminUI-chatLanguageModels-modelNameLabel']);
  $("#interfaceTypeLabel").text(window.i18n.msgStore['adminUI-chatLanguageModels-interfaceTypeLabel']);
  $("#saveModelButton").text(window.i18n.msgStore['adminUI-chatLanguageModels-saveModelButton']);
  $("#cancelButton").text(window.i18n.msgStore['adminUI-chatLanguageModels-cancelButton']);
  $("#testModelButton").text(window.i18n.msgStore['adminUI-chatLanguageModels-testModelButton']);
}

let models = [];
let activeModel = null;
let editingIndex = null;

const API_URL = "/Datafari/rest/v2.0/management/chat-language-models";

const paramTemplates = {
  OpenAI: {
    baseUrl: "https://api.openai.com/v1/",
    apiKey: "",
    modelName: "gpt-4o-mini",
    temperature: 0,
    maxTokens: 200,
    timeout: 60,
    maxRetries: 3,
    logRequests: false,
    logResponses: false
  },
  AIAgent: {
    baseUrl: "http://localhost:8888/",
    apiKey: "xxx",
    modelName: "",
    temperature: 0,
    maxTokens: 200,
    timeout: 60,
    maxRetries: 3,
    logRequests: false,
    logResponses: false
  },
  AzureOpenAI: {
    endpoint: "",
    apiKey: "",
    deploymentName: "",
    serviceVersion: "2023-05-15",
    temperature: 0,
    maxTokens: 200,
    timeout: 60,
    maxRetries: 3,
    logRequestsAndResponses: false
  },
  HuggingFace: {
    accessToken: "",
    modelId: "",
    temperature: 0,
    maxTokens: 200,
    timeout: 60
  },
  GoogleAiGemini: {
    apiKey: "",
    modelName: "gemini-pro",
    temperature: 0,
    topP: 1.0,
    topK: 40,
    maxOutputTokens: 200,
    maxRetries: 1,
    timeout: 60,
    logRequestsAndResponses: false
  },
  Ollama: {
    baseUrl: "http://localhost:11434",
    modelName: "",
    timeout: 60,
    maxRetries: 3,
    logRequests: false,
    logResponses: false
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
        <td>${model.interfaceType}</td>
        <td>
          <button class="btn btn-sm btn-outline-primary" onclick="editModel(${i})" title="Edit">
            <i class="fas fa-pen"></i> Edit
          </button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteModel(${i})" title="Delete">
            <i class="fas fa-trash-alt"></i> Delete
          </button>
          <button class="btn btn-sm btn-outline-info" onclick="testModel(${i})" title="Test">
            <i class="fas fa-vial"></i> Test
          </button>
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

  $("#test-result").hide().text("");
  $("#form-title").text(`Edit model: ${model.name}`);
  $("#model-name").val(model.name);
  $("#interface-type").val(model.interfaceType);

  generateParamFields(model.interfaceType, model.params || {});
  $("#model-form-container").slideDown();
};

window.showAddModelForm = function () {
  editingIndex = null;
  $("#test-result").hide().text("");
  $("#form-title").text("Add new model");
  $("#model-name").val("");
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
  var interfaceType = $("#interface-type").val();
  if (interfaceType == "AIAgent") {
    interfaceType = "OpenAI"
  }
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

  const model = { name, interfaceType, params };

  if (editingIndex !== null) {
    models[editingIndex] = model;
  } else {
    models.push(model);
  }

  saveAll();
  cancelEdit();
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

// === Delete ===
window.deleteModel = function (index) {
  if (confirm("Are you sure you want to delete this model?")) {
    const deleted = models.splice(index, 1)[0];
    if (deleted.name === activeModel) {
      activeModel = models.length > 0 ? models[0].name : null;
    }
    saveAll();
  }
};

// === Save all ===
async function saveAll() {
  if (activeModel === null && models.length > 0) {
    activeModel = models[0].name;
  }
  const body = JSON.stringify({ activeModel, models });

  const response = await fetch(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body
  });

  if (response.ok) {
    renderModelTable();
    renderActiveModelSelect();
    showToast("✅ Saved successfully");
  } else {
    showToast("❌ Save failed", true);
  }
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