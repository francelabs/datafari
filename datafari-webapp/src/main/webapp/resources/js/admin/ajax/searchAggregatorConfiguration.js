//# sourceURL=/Datafari/resources/js/admin/ajax/searchAggregatorConfiguration.js
var externalDatafaris = null;
var defaultDatafari = null;
var regexURL = /https?:\/\/.+/;

function internationalize() {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineConfig']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-SearchAggregatorConf']);
  $("#renew_search_aggregator_secret").text(window.i18n.msgStore['renew']);
  $("#renew_search_aggregator_secret").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['renew']);
  $("#title").text(window.i18n.msgStore['adminUI-SearchAggregatorConf']);
  $("#documentation-search-aggregator-configuration").text(window.i18n.msgStore['documentation-search-aggregator-configuration']);
  $("#searchAggregatorActivationLabel")
      .html(
          window.i18n.msgStore['searchAggregatorActivationLabel']
              + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='enable if you want to combine results of several Datafari instances, disable otherwise'>i</button></span>");
  $("#searchAggregatorClientConfig").html(window.i18n.msgStore['searchAggregatorClientConfig']);
  $("#searchAggregatorClientConfig")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"This section concerns the configuration of the search aggregator client (used when this Datafari instance is requested by another Datafari instance that is configured to aggregate results and has this Datafari instance configured as an external Datafari configuration).\">i</button></span>");
  $("#searchAggregatorRenewPasswordLabel")
      .html(
          window.i18n.msgStore['searchAggregatorRenewPasswordLabel']
              + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Renew the password of the search-aggregator API client (this client is used by external Datafari instances that are configured to aggregate results with this Datafari instance)'>i</button></span>");

  $("#searchAggregatorServerConfig").html(window.i18n.msgStore['searchAggregatorServerConfig']);
  $("#searchAggregatorServerConfig")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"This section concerns the configuration of the search aggregator server (when this Datafari instance is configured to aggregate results of external Datafari instances.\">i</button></span>");

  $("#searchAggregatorTimeouts").html(window.i18n.msgStore['searchAggregatorTimeouts']);
  $("#searchAggregatorTimeouts")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"In this section you can configure the timeouts used by the search aggregator.\">i</button></span>");
  $("#timeoutPerRequestLabel").html(window.i18n.msgStore['timeoutPerRequestLabel']);
  $("#timeoutPerRequestLabel").append(
      "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"Timeout used for each request to an external Datafari.\">i</button></span>");
  $("#globalTimeoutLabel").html(window.i18n.msgStore['globalTimeoutLabel']);
  $("#globalTimeoutLabel")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"Timeout after which any requests that still run are canceled and an aggregated response is built upon the available responses (requests that ended before this timeout) .\">i</button></span>");
  $("#submitTimeouts").text(window.i18n.msgStore['save']);
  $("#submitTimeouts").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save']);
  $("#saveTimeoutsLabel").html(window.i18n.msgStore['save']);
  $("#saveTimeoutsLabel").append("<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"Save the timeouts.\">i</button></span>");

  $("#EXDatafaris").html(window.i18n.msgStore['EXDatafaris']);
  $('#EXDatafaris')
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='To aggregate results from external Datafari, you need to configure them. You can add as many external Datafaris as you want by selecting \"Add new external Datafari\" in the dropdown list. To modify an existing external Datafari configuration, just select it in the dropdown list. You will be allowed to adjust every parameter BUT the name'>i</button></span>");
  $("#label-select-ex-datafari").html(window.i18n.msgStore['label-select-ex-datafari']);
  $("#label-new-ex-datafari").html(window.i18n.msgStore['label-new-ex-datafari']);
  $("#ExDatafariParameters").html(window.i18n.msgStore['ExDatafariParameters']);

  $("#datafariNameLabel")
      .html(
          window.i18n.msgStore['datafariNameLabel']
              + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Name of the Datafari to request (serves as id so must be unique, hostname is highly recommended)'>i</button></span>");
  $("#searchAPIUrlLabel").html(
      window.i18n.msgStore['searchAPIUrlLabel']
          + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='URL of the search API of the external Datafari'>i</button></span>");
  $("#tokenRequestUrlLabel")
      .html(
          window.i18n.msgStore['tokenRequestUrlLabel']
              + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='URL of the token API that delivers valid access tokens that work with the search API'>i</button></span>");
  $("#searchAggregatorSecretLabel").html(
      window.i18n.msgStore['searchAggregatorSecretLabel']
          + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Password of the 'search-aggregator' API client'>i</button></span>");
  $("#datafariActivationLabel")
      .html(
          window.i18n.msgStore['datafariActivationLabel']
              + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Enable or disable requests and results aggregation of this external Datafari instance'>i</button></span>");

  $("#submit").text(window.i18n.msgStore['save']);
  $("#submit").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save']);
  $("#saveLabel").html(window.i18n.msgStore['save']);
  $("#saveLabel")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"Datafari will check the provided configuration, if the API can't be requested with the search-aggregator user, the conf will not be saved and an error will be displayed. If everything is fine, the configuration is saved.\">i</button></span>");


  $("#defaultEXDatafaris").html(window.i18n.msgStore['searchAggregatorDefaultLabel'] + 
      "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"" + window.i18n.msgStore['searchAggregatorDefaultLabelTooltip'] + "\">i</button></span>");
  $("#current-default-label").html(window.i18n.msgStore['searchAggregatorCurrentDefaultLabel'])
  $("#submit-default").text(window.i18n.msgStore['save']);
  $("#submit-default").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save']);
  $("#saveDefaultLabel").html(window.i18n.msgStore['save']);
  $("#saveDefaultLabel")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"" + window.i18n.msgStore['searchAggregatorDefaultSaveLabelTooltip'] + "\">i</button></span>");
  $("#label-no-default").html(window.i18n.msgStore['none']);
  $("#label-select-default-ex-datafari").html(window.i18n.msgStore['searchAggregatorDefaultSelectLabel'])

  $("#deleteLabel").html(window.i18n.msgStore['delete']);
  $("#deleteLabel")
      .append(
          "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=\"Delete this external Datafari from the search aggregator configuration. Thus, it will not be requested anymore\">i</button></span>");
  $("#delete").html(window.i18n.msgStore['delete']);
  $("#delete").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['delete']);

}

function deleteConf() {
  $("#delete").loading("loading");
  $("#message").empty().hide();
  var datafariName = $("#datafariName").val();
  var index = $("#select-ex-datafari").val();
  $.post("../SearchAdministrator/searchAggregatorConfig", {
    datafariName : datafariName,
    action : "delete"
  }, function(data) {
    if (data != undefined && data.code != undefined) {
      if (data.code == 0) {
        $("#message").html('<i class="fas fa-check"></i> External Datafari ' + datafariName + ' deleted !').addClass("alert-success").removeClass("alert-danger").show();
        deleteLocalExDatafariElm(index);
      } else {
        $("#message").html('<i class="fas fa-times"></i> ' + data.status).addClass("alert-danger").removeClass("alert-success").show();
      }
    } else {
      $("#message").html('<i class="fas fa-times"></i> ' + window.i18n.msgStore['error']).addClass("alert-danger").removeClass("alert-success").show();
    }
    $("#delete").loading("reset");
  }, "json");
}

function setDefault() {
  $("#submit-default").loading("loading");
  $("#message").empty().hide();
  var index = $("#select-default-ex-datafari").val();
  var datafariName = "";
  if (index != -1) {
    datafariName = externalDatafaris[index].label
  }
  $.post("../SearchAdministrator/searchAggregatorConfig", {
    datafariName : datafariName,
    action : "setdefault"
  }, function(data) {
    if (data != undefined && data.code != undefined) {
      if (data.code == 0) {
        $("#current-default-value").html(datafariName ? datafariName : window.i18n.msgStore['none']);
        $("#message").html('<i class="fas fa-check"></i>' + (datafariName ? datafariName : window.i18n.msgStore['none']) + ' ' + window.i18n.msgStore['searchAggregatorDefaultSuccessMessage']).addClass("alert-success").removeClass("alert-danger").show();
      } else {
        $("#message").html('<i class="fas fa-times"></i> ' + data.status).addClass("alert-danger").removeClass("alert-success").show();
      }
    } else {
      $("#message").html('<i class="fas fa-times"></i> ' + window.i18n.msgStore['error']).addClass("alert-danger").removeClass("alert-success").show();
    }
    $("#submit-default").loading("reset");
  }, "json");
}

function deleteLocalExDatafariElm(index) {
  externalDatafaris.splice(index, 1);
  updateExDatafarisSelect();
}

function updateExDatafarisSelect(selectIndex) {
  $("#select-ex-datafari option:gt(1)").remove();
  $("#select-default-ex-datafari option:gt(1)").remove();
  var defaultLabel = window.i18n.msgStore['none'];
  for (var i = 0; i < externalDatafaris.length; i++) {
    $("#select-ex-datafari").append("<option value='" + i + "'>" + externalDatafaris[i].label + "</option>");
    if (externalDatafaris[i].enabled) {
      if (defaultDatafari == externalDatafaris[i].label) {
        $("#select-default-ex-datafari").append("<option selected value='" + i + "'>" + externalDatafaris[i].label + "</option>");
        defaultLabel = defaultDatafari;
      } else {
        $("#select-default-ex-datafari").append("<option value='" + i + "'>" + externalDatafaris[i].label + "</option>");
      }
    }
  }
  if (selectIndex !== undefined) {
    $('#select-ex-datafari').val(selectIndex).change();
  } else {
    $('#select-ex-datafari').val("new").change();
  }
  $("#current-default-value").html(defaultLabel);
}

function updateLocalList(index, datafariName, search_api_url, token_request_url, search_aggregator_secret, enabled) {
  if (index == "new") {
    index = externalDatafaris.length;
  }
  if (index == 0) {
    externalDatafaris[index] = {};
  }
  externalDatafaris[index].label = datafariName;
  externalDatafaris[index].search_api_url = search_api_url;
  externalDatafaris[index].token_request_url = token_request_url;
  externalDatafaris[index].search_aggregator_secret = search_aggregator_secret;
  externalDatafaris[index].enabled = enabled;
  return index;
}

function checkElm(element) {
  clearStatus(element);
  if (!element.val()) {
    setErrorStatus(element, null);
    element.addClass("is-invalid");
    return false;
  } else {
    setOkStatus(element);
    return true;
  }
}

function clearStatus(element) {
  element.removeClass("is-valid");
  element.removeClass("is-invalid");
  let parent = element.closest(".form-group");
  parent.removeClass("was-validated");
};

function setOkStatus(element, validMsg) {
  element.addClass("is-valid");
  if (validMsg !== undefined && validMsg !== null) {
    element.siblings(".valid-feedback").html(validMsg);
  }
};

function setErrorStatus(element, errorMsg) {
  if (errorMsg !== null && errorMsg !== undefined) {
    element.siblings(".invalid-feedback").html(errorMsg);
  }
  element.addClass("is-invalid");
}

function clearAllStatus() {
  clearStatus($("#datafariName"));
  clearStatus($("#search_api_url"));
  clearStatus($("#token_request_url"));
  clearStatus($("#search_aggregator_secret"));
}

function setOnchangeEvents() {
  $("#datafariName").change(function() {
    checkElm($("#datafariName"));
  });

  $("#search_api_url").change(function() {
    checkURL($("#search_api_url"));
  });

  $("#token_request_url").change(function() {
    if (checkURL($("#token_request_url")) && checkElm($("#search_aggregator_secret"))) {
      checkSASecret();
    }
  });

  $("#search_aggregator_secret").change(function() {
    if (checkElm($("#search_aggregator_secret")) && checkURL($("#token_request_url"))) {
      checkSASecret();
    }
  });
}

function checkURL(element) {
  if (checkElm(element)) {
    var elmURL = element.val();
    if (regexURL.test(elmURL)) {
      setOkStatus(element);
      return true;
    } else {
      setErrorStatus(element, window.i18n.msgStore['bad-url-format']);
      return false;
    }
  } else {
    return false;
  }
}

function checkSASecret() {
  var tokenRequestURL = $("#token_request_url").val();
  var secretElm = $("#search_aggregator_secret");
  clearStatus(secretElm);
  if (!secretElm.val()) {

  } else {

  }
}

$(document).ready(
    function() {

      internationalize();

      setOnchangeEvents();

      // Init toggle buttons
      $('#search_aggregator_activation').bootstrapToggle();
      $('#ex_datafari_activation').bootstrapToggle();

      // Init delete button
      $("#delete").click(function() {
        deleteConf();
      });

      // init search_aggregator_activation
      $.get("../SearchAdministrator/searchAggregatorConfig", function(data) {
        if (data.code == 0) {
          var activated = data.activated;
          var timeoutPerRequest = data.timeoutPerRequest;
          var globalTimeout = data.globalTimeout;
          var renewAvailable = data.renew_available;

          $("#timeoutPerRequest").val(timeoutPerRequest);
          $("#globalTimeout").val(globalTimeout);

          if (activated == true) {
            $("#search_aggregator_activation").bootstrapToggle('on', true);
          } else {
            $("#search_aggregator_activation").bootstrapToggle('off', true);
          }

          if (renewAvailable == false) {
            $("#renew_search_aggregator_secret").prop('disabled', true);
            $("#search-aggregator-secret-message").html(
                "The search-aggregator client is not managed by Datafari but by an Identity Provider ! Use the IDP to manage the search-aggregator secret password !");
            $("#search-aggregator-secret-message").addClass("alert-success");
            $("#search-aggregator-secret-message").removeClass("alert-danger");
            $("#search-aggregator-secret-message").show();
          } else {
            $("#renew_search_aggregator_secret").prop('disabled', false);
          }

          externalDatafaris = data.external_datafaris;
          defaultDatafari = data.default_datafari;
          var defaultLabel = window.i18n.msgStore['none'];
          for (var i = 0; i < externalDatafaris.length; i++) {
            $("#select-ex-datafari").append("<option value='" + i + "'>" + externalDatafaris[i].label + "</option>");
            if (externalDatafaris[i].enabled) {
              if (defaultDatafari == externalDatafaris[i].label) {
                $("#select-default-ex-datafari").append("<option selected value='" + i + "'>" + externalDatafaris[i].label + "</option>");
                defaultLabel = defaultDatafari;
              } else {
                $("#select-default-ex-datafari").append("<option value='" + i + "'>" + externalDatafaris[i].label + "</option>");
              }
            }
          }

          $("#current-default-value").html(defaultLabel);


          $("#select-ex-datafari").change(function() {
            $("#message").fadeOut(5000);
            clearAllStatus();
            if ($(this).val() !== "") {
              $("#fs-parameters").show();
            } else {
              $("#fs-parameters").hide();
            }
            $("#submit").prop("disabled", false);
            var index = $("#select-ex-datafari").val();
            if (index != "new") {
              $(".delete-group").show();
              $("#datafariName").prop("disabled", true);
              $("#datafariName").val(externalDatafaris[index].label);
              $("#search_api_url").val(externalDatafaris[index].search_api_url);
              $("#token_request_url").val(externalDatafaris[index].token_request_url);
              $("#search_aggregator_secret").val(externalDatafaris[index].search_aggregator_secret);
              if (externalDatafaris[index].enabled == true) {
                $("#ex_datafari_activation").bootstrapToggle('on', true);
              } else {
                $("#ex_datafari_activation").bootstrapToggle('off', true);
              }

            } else {
              $("#ex_datafari_activation").bootstrapToggle('off', true);
              $("#datafariName").prop("disabled", false);
              $("#datafariName").val("");
              $("#search_api_url").val("");
              $("#token_request_url").val("");
              $("#search_aggregator_secret").val("");
              $(".delete-group").hide();
            }
          });

        }
      }, "json");

      $("#timeout-form").submit(function(e) {
        e.preventDefault();
        $("#timeouts-message").empty().hide();
        $("#submitTimeouts").loading("loading");
        var timeoutPerRequest = $("#timeoutPerRequest").val();
        var globalTimeout = $("#globalTimeout").val();
        var action = "timeouts";
        $.post("../SearchAdministrator/searchAggregatorConfig", {
          timeoutPerRequest : timeoutPerRequest,
          globalTimeout : globalTimeout,
          action : action
        }, function(data) {
          if (data != undefined && data.code != undefined) {
            if (data.code == 0) {
              $("#timeouts-message").html('<i class="fas fa-check"></i> ' + window.i18n.msgStore['saved']).addClass("alert-success").removeClass("alert-danger").show();
            } else {
              $("#timeouts-message").html('<i class="fas fa-times"></i> ' + data.status).addClass("alert-danger").removeClass("alert-success").show();
            }
          } else {
            $("#timeouts-message").html('<i class="fas fa-times"></i> ' + window.i18n.msgStore['error']).addClass("alert-danger").removeClass("alert-success").show();
          }
          $("#submitTimeouts").loading("reset");
        }, "json");
      });

      $("#external-datafaris-form").submit(function(e) {
        e.preventDefault();
        $("#message").empty().hide();
        if (checkElm($("#datafariName")) && checkURL($("#search_api_url")) && checkURL($("#token_request_url")) && checkElm($("#search_aggregator_secret"))) {
          $("#submit").loading("loading");
          var action = "modify";
          var index = $("#select-ex-datafari").val();
          var datafariName = $("#datafariName").val();
          var search_api_url = $("#search_api_url").val();
          var token_request_url = $("#token_request_url").val();
          var search_aggregator_secret = $("#search_aggregator_secret").val();
          if (index == "new") {
            action = "new";
          }
          var enabled = true;
          if (!$("#ex_datafari_activation").is(':checked')) {
            enabled = false;
          }
          $.post("../SearchAdministrator/searchAggregatorConfig", {
            datafariName : datafariName,
            search_api_url : search_api_url,
            token_request_url : token_request_url,
            search_aggregator_secret : search_aggregator_secret,
            enabled : enabled,
            action : action
          }, function(data) {
            if (data != undefined && data.code != undefined) {
              if (data.code == 0) {
                $("#message").html('<i class="fas fa-check"></i> ' + window.i18n.msgStore['saved']).addClass("alert-success").removeClass("alert-danger").show();
                var newIndex = updateLocalList(index, datafariName, search_api_url, token_request_url, search_aggregator_secret, enabled);
                updateExDatafarisSelect(newIndex);
              } else {
                $("#message").html('<i class="fas fa-times"></i> ' + data.status).addClass("alert-danger").removeClass("alert-success").show();
              }
            } else {
              $("#message").html('<i class="fas fa-times"></i> ' + window.i18n.msgStore['error']).addClass("alert-danger").removeClass("alert-success").show();
            }
            $("#submit").loading("reset");
          }, "json");
        } else {
          return false;
        }
      });

      $("#search_aggregator_activation").change(function click_handler(e) {
        e.preventDefault();
        $("#search-aggregator-message").hide();
        var element = $(this);

        var enable = "true";
        if (!element.is(':checked')) {
          enable = "false";
        }

        $.post("../SearchAdministrator/searchAggregatorConfig", {
          action : "activate",
          activated : enable
        }, function(data) {
          if (data.code == undefined || data.code != 0) {
            $("#search-aggregator-message").html('<i class="fas fa-times"></i> ' + window.i18n.msgStore['error']);
            $("#search-aggregator-message").removeClass("alert-success");
            $("#search-aggregator-message").addClass("alert-danger");
            $("#search-aggregator-message").show();
          }
        }, "json");

      });

      $("#renew_search_aggregator_secret").click(function() {
        $("#search-aggregator-secret-message, #search-aggregator-secret-message2").hide();
        $("#renew_search_aggregator_secret").loading("loading");
        $.post("../SearchAdministrator/searchAggregatorConfig", {
          action : "renew"
        }, function(data) {
          $("#renew_search_aggregator_secret").loading("reset");
          if (data.code == 0) {
            var password = data.password;
            $("#search-aggregator-secret-message").html("New password: " + password);
            $("#search-aggregator-secret-message2").html("This password will not be shown anymore so save it somewhere safe or you will need to renew it again !");
            $("#search-aggregator-secret-message, #search-aggregator-secret-message2").addClass("alert-success");
            $("#search-aggregator-secret-message, #search-aggregator-secret-message2").removeClass("alert-danger");
            $("#search-aggregator-secret-message, #search-aggregator-secret-message2").show();
          } else {
            $("#search-aggregator-secret-message").html("Error: " + data.status);
            $("#search-aggregator-secret-message").removeClass("alert-success");
            $("#search-aggregator-secret-message").addClass("alert-danger");
            $("#search-aggregator-secret-message").show();
          }
        }, "json");
      });

      $("#default-datafari-form").submit(function(e) {
        e.preventDefault();
        setDefault();
      });
    });