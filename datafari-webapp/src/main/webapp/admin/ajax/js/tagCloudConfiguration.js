//# sourceURL=/Datafari/admin/ajax/js/entityExtractionConfig.js

var timeouts = [];

var clearTimeouts = function() {
  for (var i = 0; i < timeouts.length; i++) {
    clearTimeout(timeouts[i]);
  }

  //quick reset of the timer array you just cleared
  timeouts = [];

  $('#main').unbind('DOMNodeRemoved');
}

$(document).ready(function(){
  $('#main').bind('DOMNodeRemoved', clearTimeouts);
  var simpleFeedbackMessageDiv = $("#simpleFeedbackMessage");
  var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
  let tagCloudResponseCodes= {
    'OK' : 0
  };

  setupLanguage();

  // suppose the feature is not active until we loaded the actual state
  $("#activateTagCloud").prop("checked", false);

  initValues();

  function setupLanguage(){
    $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
    $("#topbar1").text(window.i18n.msgStore['home']);
    $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineConfig']);
    $("#topbar3").text(window.i18n.msgStore['adminUI-tagCloudConf']);
    $("#title").text(window.i18n.msgStore['adminUI-tagCloudConf']);
    $("#activateTagCloudLabel").text(window.i18n.msgStore['adminUI-tagCloudActivate']);
    $("#simpleSaveButton").text(window.i18n.msgStore['save']);
    $("#simpleSaveButton").attr('data-loading-text', "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save']);
  }

  function initValues() {
    $.get("../SearchAdministrator/tagCloudConfiguration", function(data) {
      $("#activateTagCloud").prop("checked", data.isActivated?true:false);
    });
  }

  function showError(){
    let message= "Error";
    let danger = true;

    sourceError = source;
    simpleFeedbackMessageDiv.text(message).show();
    if (danger){
        simpleFeedbackMessageDiv.addClass("danger").prepend('<i class="fas fa-exclamation-triangle"></i> ');
    }else{
      simpleFeedbackMessageDiv.removeClass("danger");
    }
  }

  function clearStatus(element) {
    let parent = element.closest(".form-group");
    parent.removeClass("has-error");
    parent.removeClass("has-warning");
    parent.removeClass("has-success");
    parent.removeClass("has-feedback");
    let glyphElement = element.next();
    glyphElement.removeClass("glyphicon-remove");
    glyphElement.removeClass("glyphicon-warning-sign");
    glyphElement.removeClass("glyphicon-ok");
  };

  function setSuccess(element) {
    element.closest(".form-group").addClass("has-success");
    element.closest(".form-group").addClass("has-feedback");
    element.next().addClass("glyphicon-ok");
  };

  function setWarning(element) {
    element.closest(".form-group").addClass("has-warning");
    element.closest(".form-group").addClass("has-feedback");
    element.next().addClass("glyphicon-warning-sign");
  }

  function setError(element) {
    element.closest(".form-group").addClass("has-error");
    element.closest(".form-group").addClass("has-feedback");
    element.next().addClass("glyphicon-remove");
  };

  $('#tagCloudConfigurationForm').submit(function(e){
    e.preventDefault();
    if ($("#simpleSaveButton").hasClass('disabled')) {
      return;
    }
    $("#simpleSaveButton").button("loading");

    let isActivated = $("#activateTagCloud").is(":checked");
    
    $.post("../SearchAdministrator/tagCloudConfiguration",{
      isActivated : isActivated,
    },function(data){
      if (data.code == tagCloudResponseCodes['OK']){
        $("#simpleFeedbackMessage").html("<i class='fa fa-check'></i>" + window.i18n.msgStore['parameterSaved'])
                                   .addClass("success")
                                   .show()
                                   .removeClass("animated fadeOut");
        timeouts.push(setTimeout(function(){
          $("#simpleFeedbackMessage").addClass("animated fadeOut");
        },1500));
      }else{
        $("#simpleFeedbackMessage").html("<i class='fa fa-exclamation-triangle'></i>" + window.i18n.msgStore['adminUI-ErrorWhileSaving'])
                                   .addClass("danger")
                                   .show()
                                   .removeClass("animated fadeOut");
        timeouts.push(setTimeout(function(){
          $("#simpleFeedbackMessage").addClass("animated fadeOut");
        },1500));
      }
      $("#simpleSaveButton").button("reset");
    },"json").fail(function() {
      $("#simpleFeedbackMessage").html("<i class='fa fa-exclamation-triangle'></i>" + window.i18n.msgStore['adminUI-ErrorWhileSaving'])
                                 .addClass("danger")
                                 .show()
                                 .removeClass("animated fadeOut");
      timeouts.push(setTimeout(function(){
        $("#simpleFeedbackMessage").addClass("animated fadeOut");
      },1500));
      $("#simpleSaveButton").button("reset");
    });
    return false;
  });
});
