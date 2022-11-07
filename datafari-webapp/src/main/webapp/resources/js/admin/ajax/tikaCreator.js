//# sourceURL=/Datafari/resources/js/admin/ajax/tikaCreator.js

var timeouts = [];
var clearTimeouts = function() {
  for (var i = 0; i < timeouts.length; i++) {
    clearTimeout(timeouts[i]);
  }

  // quick reset of the timer array you just cleared
  timeouts = [];

  $('#main').unbind('DOMNodeRemoved');
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

function setOkStatus(element) {
  element.addClass("is-valid");
};

function setErrorStatus(element, errorMsg) {
  if (errorMsg !== null) {
    element.siblings(".invalid-feedback").html(errorMsg);
  }
  element.addClass("is-invalid");
}

$(document).ready(function() {

  // Set the breadcrumbs
  $("#topbar1").html(window.i18n.msgStore['home']);
  $("#topbar2").html(window.i18n.msgStore['adminUI-expertMenu']);
  $("#topbar3").html(window.i18n.msgStore['adminUI-tikaCreator']);

  // Set the i18n for page elements
  $("#box-title").html(window.i18n.msgStore['adminUI-tikaCreator']);
  $("#installDirLabel").html(window.i18n.msgStore['tikaInstallDirLabel']);
  $("#installDirWarnLabel").html(window.i18n.msgStore['tikaInstallDirWarnLabel']);
  $("#tikaHostLabel").html(window.i18n.msgStore['tikaHostLabel']);
  $("#tikaPortLabel").html(window.i18n.msgStore['tikaPortLabel']);
  $("#tikaTempDirLabel").html(window.i18n.msgStore['tikaTempDirLabel']);
  $("#tikaTypeLabel").html(window.i18n.msgStore['tikaTypeLabel']);
  $("#ocrStrategyLabel").html(window.i18n.msgStore['ocrStrategyLabel']);
  $("#externalTikaLabel").html(window.i18n.msgStore['externalTika']);
  $("#tikaCreateLabel").html(window.i18n.msgStore['tikaCreate']);
  $("#createTikaServer").html(window.i18n.msgStore['create']);
  
  // tooltips
  $("#installDir-tip").attr("title", window.i18n.msgStore['installDir-tip']);
  $("#tikaHost-tip").attr("title", window.i18n.msgStore['tikaHost-tip']);
  $("#tikaPort-tip").attr("title", window.i18n.msgStore['tikaPort-tip']);
  $("#tikaTempDir-tip").attr("title", window.i18n.msgStore['tikaTempDir-tip']);
  $("#tikaType-tip").attr("title", window.i18n.msgStore['tikaType-tip']);
  $("#ocrStrategy-tip").attr("title", window.i18n.msgStore['ocrStrategy-tip']);
  $("#externalTika-tip").attr("title", window.i18n.msgStore['externalTika-tip']);
  
  
  $("#installDir").change(function(e) {
    checkElm($("#installDir"));
  });
  $("#tikaHost").change(function(e) {
    checkElm($("#tikaHost"));
  });
  $("#tikaPort").change(function(e) {
    checkElm($("#tikaPort"));
  });
  $("#tikaTempDir").change(function(e) {
    checkElm($("#tikaTempDir"));
  });

  $("#tikaCreateForm").submit(function(e) {
    e.preventDefault();
    $("#tikaCreateForm").removeClass('was-validated');
    $("#tikaCreatorMessageSuccess").hide();
    $("#tikaCreatorMessageFailure").hide();
    var form = document.getElementById("tikaCreateForm");
    if (form.checkValidity() === false) {
      return false;
    } else {
      return createTikaServer();
    }
  });
});

function getTikaForm() {
  var tikaType = $("#tikaType").val();
  if (tikaType == "ocr") {
    $("#tikaOCRDiv").show();
  } else {
    $("#tikaOCRDiv").hide();
  }
}

function createTikaServer() {
  if ($("#createTikaServer").hasClass('disabled')) {
    return;
  }
  $("#createTikaServer").loading("loading");
  // Put the Data of the form into a global variable and serialize it
  formData = $("#tikaCreateForm").serialize();
  $.ajax({ 
    type : "POST",
    url : "./../SearchAdministrator/createTikaServer",
    data : formData,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if (data.code !== 0) {
        if(data.status !== null && data.status !== undefined) {
          $("#tikaCreatorMessageFailure").html(data.status);
        } else {
          $("#tikaCreatorMessageFailure").html("An unkown problem occurred while creating the Tika Server");
        }
        $("#tikaCreatorMessageFailure").show();
      } else {
        $("#tikaCreateForm").trigger("reset");
        $("#tikaOCRDiv").hide();
        var getUrl = window.location;
        $("#tikaCreatorMessageSuccess").html(
            "<i class='fa fa-check'></i>Tika Server created with success !");
        $("#tikaCreatorMessageSuccess").show();
        clearStatus($("#installDir"));
        clearStatus($("#tikaHost"));
        clearStatus($('#tikaPort'));
        clearStatus($('#tikaTempDir'));
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#addPromForm").html(jqXHR.responseText);
    },
    // capture the request before it was sent to server
    beforeSend : function(jqXHR, settings) {
      // disable the button until we get the response
      $('#tikaCreateForm').attr("disabled", true);
    },
    // this is called after the response or error functions are finsihed
    complete : function(jqXHR, textStatus) {
      // enable the button
      $("#createTikaServer").loading("reset");
    }
  });
}