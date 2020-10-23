// Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax//relevancySetupFile.js


$(document).ready(function() {

  $('relevancySetupFile-label').text(window.i18n.msgStore["adminUI-RelevancyFile-label"]);

  // Set the breadcrumbs
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-RelevancySetupFile'];

  // Set the i18n for page elements
  document.getElementById("box-title").innerHTML = window.i18n.msgStore['adminUI-RelevancySetupFile'];
  document.getElementById("documentation-relevancyfile").innerHTML = window.i18n.msgStore['documentation-relevancyfile'];
  document.getElementById("relevancySetupFile-label").innerHTML = window.i18n.msgStore['relevancySetupFile-label'];
  document.getElementById("goldenQueriesSetupFile-label").innerHTML = window.i18n.msgStore['goldenQueriesSetupFile-label'] 
  + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='This is the path to the golden queries file. It must be readable and writable by the user running the datafari process (user named datafari by default)'>i</button></span>";
  document.getElementById("doSave-btn").innerHTML = window.i18n.msgStore['save'];
  document.getElementById("doSaveRelevancy-btn").innerHTML = window.i18n.msgStore['save'];
  document.getElementById("fixed_parameters_set_legend").innerHTML = window.i18n.msgStore['parametersLegendFixed'];
  document.getElementById("parameters_set_legend").innerHTML = window.i18n.msgStore['parametersLegend'];
  $('#relevancySetupFile-label').append("<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='This is the path to the parameters file. It must be readable and writable by the user running the datafari process (user named datafari by default)'>i</button></span>");
  $('.relevancySave').html(window.i18n.msgStore['adminUI-DepartmentSearchConfConfirm1']);

  $.get("./../admin/relevancySetupFile", function(data) {
    $("#relevancySetupFile-input").val(data.relevancySetupFilePath);
    $("#goldenQueriesSetupFile-input").val(data.goldenQueriesSetupFilePath);
  }, "json");

  refreshParams();

});
let params;
let currentEditing;

function refreshParams() {
  $.get("./../admin/relevancySetupFile?action=getRelevancySetup", function(data) {
      params = data;
      refreshDisplay();
    });
  }

function refreshDisplay() {
  let parametersSection = $('#parameters_set');
  parametersSection.html("");
  parametersSection.append("\
  <legend id=\"parameters_set_legend\">" + window.i18n.msgStore['parametersLegend'] + "</legend>\
  ");
  params['parameters'].forEach(function(parameter, index) {
    parametersSection.append("\
    <div id=\"param"+parameter['id']+"\">\
      <div class=\"row\">\
          <label for=\"param_name_" + parameter['id'] + "\" class=\"pl-3 col-form-label param_name_label\">" + window.i18n.msgStore['name'] + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"param_name_" + parameter['id'] + "\" class=\"form-control\" value=\"" + parameter['name'] + "\" readonly/>\
          </div>\
          <label for=\"param_type_" + parameter['id'] + "\" class=\"pl-3 col-form-label param_type_label\">" + window.i18n.msgStore['parameterType'] + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"param_type_" + parameter['id'] + "\" class=\"form-control\" value=\"" + parameter['type'] + "\" readonly/>\
          </div>\
          <label for=\"param_min_" + parameter['id'] + "\" class=\"pl-3 col-form-label param_min_label\">" + window.i18n.msgStore['min'] + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"param_min_" + parameter['id'] + "\" class=\"form-control\" value=\"" + parameter['start'] + "\" readonly/>\
          </div>\
          <label for=\"param_max_" + parameter['id'] + "\" class=\"pl-3 col-form-label param_max_label\">" + window.i18n.msgStore['max'] + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"param_max_" + parameter['id'] + "\" class=\"form-control\" value=\"" + parameter['end'] + "\" readonly/>\
          </div>\
          <div class=\"col-sm-2 col-form-label\">\
            <div class=\"row\">\
              <button class=\"btn\" onclick=\"return editClick(false," + parameter['id'] + ")\"><i class=\"fas fa-pencil-alt\"></i></button>\
              <button class=\"btn\" onclick=\"return deleteParam(false," + parameter['id'] + ")\"><i class=\"far fa-trash-alt\"></i> </span></button>\
            </div>\
          </div>\
      </div>\
    </div>\
    ");
  });
  parametersSection.append("\
    <div class=\"row justify-content-end\">\
      <div class=\"col-sm-1\">\
        <button class=\"btn\" onclick=\"return newParam(false)\"><i class=\"fas fa-plus\"></i><span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Click on the plus sign to create a new parameter'>i</button></span></button>\
      </div>\
    </div>\
    ");

  let fixedParametersSection = $('#fixed_params_set');
  fixedParametersSection.html("");
  fixedParametersSection.append("\
  <legend id=\"fixed_parameters_set_legend\">" + window.i18n.msgStore['parametersLegendFixed'] + "</legend>\
  ");
  params['fixed_params'].forEach(function(fixedParam, index) {
    fixedParametersSection.append("\
    <div id=\"fixed"+fixedParam['id']+"\">\
      <div class=\"row\">\
          <label for=\"fixed_param_name_" + fixedParam['id'] + "\" class=\"pl-3 col-form-label param_name_label\">" + window.i18n.msgStore['name'] 
          + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='The name section must correspond to a field of the index on which you want to set a parameter (to optimize it between two values in this case)'>i</button></span>" + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"fixed_param_name_" + fixedParam['id'] + "\" class=\"form-control\" value=\"" + fixedParam['name'] + "\" readonly/>\
          </div>\
          <label for=\"fixed_param_type_" + fixedParam['id'] + "\" class=\"pl-3 col-form-label param_type_label\">" + window.i18n.msgStore['parameterType']
          + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='The type section should be \"qf\" or \"pf\" depending on the type of solr parameter you want to optimize'>i</button></span>" + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"fixed_param_type_" + fixedParam['id'] + "\" class=\"form-control\" value=\"" + fixedParam['type'] + "\" readonly/>\
          </div>\
          <label for=\"fixed_param_value_" + fixedParam['id'] + "\" class=\"pl-3 col-form-label param_min_label\">" + window.i18n.msgStore['value']
          + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Must be of type INTEGER'>i</button></span>" + "</label>\
          <div class=\"col-sm-2\">\
            <input type=\"text\" id=\"fixed_param_value_" + fixedParam['id'] + "\" class=\"form-control\" value=\"" + fixedParam['value'] + "\" readonly/>\
          </div>\
          <div class=\"col-sm-2 col-form-label\">\
            <div class=\"row\">\
              <button class=\"btn\" onclick=\"return editClick(true," + fixedParam['id'] + ")\"><i class=\"fas fa-pencil-alt\"></i></button>\
              <button class=\"btn\" onclick=\"return deleteParam(true," + fixedParam['id'] + ")\"><i class=\"far fa-trash-alt\"></i> </span></button>\
            </div>\
          </div>\
      </div>\
    </div>\
    ");
  });
  fixedParametersSection.append("\
    <div class=\"row justify-content-end\">\
      <div class=\"col-sm-1\">\
        <button class=\"btn\" onclick=\"return newParam(true)\"><i class=\"fas fa-plus\"></i><span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Click on the plus sign to create a new parameter'>i</button></span></button>\
      </div>\
    </div>\
    ");
  prepareCurrentlyEdited()
}

function newParam(fixed) {
  let newId = undefined;
  if (fixed) {
    let allIds = [];
    params['fixed_params'].forEach(function(element) {
      allIds.push(element.id);
    });
    if (allIds.length > 0) {
      newId = Math.max(...allIds);
      newId = newId || newId == 0 ? newId+1 : 0;
    } else {
      newId = 0;
    }
    params['fixed_params'].push({'id':newId, 'name':undefined, 'type':undefined, 'value':undefined});
  } else {
    let allIds = [];
    params['parameters'].forEach(function(element) {
      allIds.push(element.id);
    });
    if (allIds.length > 0) {
      newId = Math.max(...allIds);
      newId = newId || newId == 0 ? newId+1 : 0;
    } else {
      newId = 0;
    }
    params['parameters'].push({'id':newId, 'name':undefined, 'type':undefined, 'start':undefined, 'end':undefined})
  }
  refreshDisplay();
  editClick(fixed, newId);
  return false;
}

function deleteParam(fixed, id) {
  if (currentEditing) {
    endEditing();
  }
  let array = [];
  function deleteFilter(param) {
    return param['id'] !== id;
  }
  if (fixed) {
    params['fixed_params'] = params['fixed_params'].filter(deleteFilter);

  } else {
    params['parameters'] = params['parameters'].filter(deleteFilter);
  }
  sendNewValues();
  return false;
}

function editClick(fixed, id) {
  if (currentEditing) {
    let returnAfterEnd = (currentEditing.fixed == fixed && currentEditing.id === id);
    endEditing();
    if (returnAfterEnd) {
      return false;
    }
  }
  currentEditing = {
    'fixed': fixed,
    'id': id
  };
  prepareCurrentlyEdited();
  return false;
}

function prepareCurrentlyEdited() {
  if (!currentEditing) {
    return false;
  }
  let element;
  if (currentEditing.fixed) {
    element = $("#fixed" + currentEditing.id);
  } else {
    element = $("#param" + currentEditing.id);
  }
  element.find("input").prop("readonly", false);
  let span = element.find(".fas.fa-pencil-alt");
  span.removeClass("fas fa-pencil-alt");
  span.addClass("fas fa-check");
  return false;
}

function endEditing() {
  let id = currentEditing.id;
  let element;
  function search(elm) {
    return elm.id === id;
  }
  if (currentEditing.fixed) {
    element = $("#fixed" + id);
    let param = params['fixed_params'].find(search);
    param.name = $("#fixed_param_name_" + id).val();
    param.type = $("#fixed_param_type_" + id).val();
    param.value = parseInt($("#fixed_param_value_" + id).val(),10);
  } else {
    element = $("#param" + id);
    let param = params['parameters'].find(search);
    param.name = $("#param_name_" + id).val();
    param.type = $("#param_type_" + id).val();
    param.start = parseInt($("#param_min_" + id).val(),10);
    param.end = parseInt($("#param_max_" + id).val(),10);
  }
  element.find("input").prop("readonly", true);
  let span = element.find(".fas.fa-check");
  span.removeClass("fas fa-check");
  span.addClass("fas fa-pencil-alt");
  sendNewValues();
  currentEditing = null;
}

function sendNewValues() {
  $.ajax("./../admin/relevancySetupFile?action=saveconfig", {
    data : {params: JSON.stringify(params)},
    type : 'POST',
    success : function() {
      window.setTimeout(refreshParams, 100);
    }
  });
}

function doSave() {

  // Reset message
  $("doSaveReturnStatus-label").html('');

  var data = "action=save";

  if ($('#relevancySetupFile-input').val() && $('#goldenQueriesSetupFile-input').val()) {
    data = data + "&relevancySetupFile=" + $('#relevancySetupFile-input').val();
    data = data + "&goldenQueriesSetupFile=" + $('#goldenQueriesSetupFile-input').val();
  }

  $.ajax({ // Ajax request to the doPost of the MCF Backup Restore servlet
    type : "POST",
    url : "./../admin/relevancySetupFile",
    data : data,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if(data.toString().indexOf("Error code : ")!==-1){
        $("#doSaveReturnStatus-label").text(window.i18n.msgStore["adminUI-RelevancySetupFile-saveError"]).switchClass('relevancySetupFileOK','relevancySetupFileError', 100);

      } else {
        $("#doSaveReturnStatus-label").text(window.i18n.msgStore["adminUI-RelevancySetupFile-saveOK"]).switchClass('relevancySetupFileError','relevancySetupFileOK', 100);
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#doSaveReturnStatus-label").html(jqXHR.responseText).addClass('relevancySetupFileError');
    },
    // capture the request before it was sent to server
    beforeSend : function(jqXHR, settings) {
      // disable the buttons until we get the response
      $('#doSave-btn').prop("disabled", true);
    },
    // this is called after the response or error functions are finished
    complete : function(jqXHR, textStatus) {
      // enable the buttons
      $('#doSave-btn').prop("disabled", false);
    }
  });
}

function doSaveRelevancySetup() {
  sendNewValues();
}
