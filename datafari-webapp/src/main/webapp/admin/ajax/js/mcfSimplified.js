// Very important to allow debugging !
//# sourceURL=/Datafari/admin/ajax/js/mcfSimplified.js

var timeouts = [];
var returnData = undefined;
var clearTimeouts = function() {
  for (var i = 0; i < timeouts.length; i++) {
    clearTimeout(timeouts[i]);
  }

  // quick reset of the timer array you just cleared
  timeouts = [];

  $('#main').unbind('DOMNodeRemoved');
}

$(document).ready(function() {
  $('#main').bind('DOMNodeRemoved', clearTimeouts);
  $('backupDir-label').text(window.i18n.msgStore["adminUI-MCFBackupRestore-backupDir-label"]);
  $('.MCFSave').html(window.i18n.msgStore['adminUI-MCFSave']
    + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Be careful, if you do not check the “Start the job once created” checkbox, the job will be saved and it will present in the crawlers admin interface, but it will not start automatically'>i</button></span>");

  // Set the breadcrumbs
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Connectors'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Connectors-MCFSimplified'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Connectors'];
  // Set the i18n for page elements
  document.getElementById("box-title").innerHTML = window.i18n.msgStore['adminUI-Connectors-MCFSimplified'];
  document.getElementById("documentation-mcfsimplified").innerHTML = window.i18n.msgStore['documentation-mcfsimplified'];

  document.getElementById("mcfsimplified-title-label").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-title'];
  // document.getElementById("backupDir-label-default").innerHTML =
  // window.i18n.msgStore['adminUI-MCFBackupRestore-backupDir-label-default'];
  // document.getElementById("web-btn").innerHTML =
  // window.i18n.msgStore['adminUI-MCFSimplified-web-btn'];
  // document.getElementById("filer-btn").innerHTML =
  // window.i18n.msgStore['adminUI-MCFSimplified-filer-btn'];
  document.getElementById("createWebJob").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-createWebJob'];
  document.getElementById("createFilerJob").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-createFilerJob'];

  document.getElementById("webJobTitle").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-webjobFormEdit'];
  document.getElementById("webAddLegend").innerHTML = window.i18n.msgStore['param'];
  document.getElementById("webSeedsLabel").innerHTML = window.i18n.msgStore['seeds'];
  document.getElementById("webEmailLabel").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-email'];
  document.getElementById("webExclusionLabel").innerHTML = window.i18n.msgStore['exclusions'];
  document.getElementById("webSourcenameLabel").innerHTML = window.i18n.msgStore['sourcename'];
  $("#seeds").attr("placeholder", window.i18n.msgStore['seeds']);
  $("#email").attr("placeholder", window.i18n.msgStore['adminUI-MCFSimplified-email']);
  $("#exclusions").attr("placeholder", window.i18n.msgStore['exclusions']);
  $("#newWebConfig").innerHTML = window.i18n.msgStore['confirm'];
  $("#newWebConfig").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);

  document.getElementById("filerJobTitle").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-filerjobFormEdit'];
  document.getElementById("filerAddLegend").innerHTML = window.i18n.msgStore['param'];
  document.getElementById("serverLabel").innerHTML = window.i18n.msgStore['server'];
  document.getElementById("userLabel").innerHTML = window.i18n.msgStore['filerUser'];
  document.getElementById("passwordLabel").innerHTML = window.i18n.msgStore['adminUI-Password'];
  document.getElementById("pathsLabel").innerHTML = window.i18n.msgStore['paths'];
  document.getElementById("filerSourcenameLabel").innerHTML = window.i18n.msgStore['sourcename'];
  document.getElementById("securityLabel").innerHTML = window.i18n.msgStore['security'];
  $("#startJobLabel").html(window.i18n.msgStore['startJob']);
  $("#startJobWebLabel").html(window.i18n.msgStore['startJob']);
  $("#server").attr("placeholder", window.i18n.msgStore['server']);
  $("#password").attr("placeholder", window.i18n.msgStore['adminUI-Password']);
  $("#user").attr("placeholder", window.i18n.msgStore['filerUser']);
  $("#paths").attr("placeholder", window.i18n.msgStore['paths']);
  $("#filerSourcename").attr("placeholder", window.i18n.msgStore['sourcename']);
  ("#newFilerConfig").innerHTML = window.i18n.msgStore['confirm'];
  $("#newFilerConfig").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);

  // Set the tooltips
  $("#server-tip").attr("title", window.i18n.msgStore['server-tip']);
  $("#password-tip").attr("title", window.i18n.msgStore['password-tip']);
  $("#user-tip").attr("title", window.i18n.msgStore['user-tip']);
  $("#paths-tip").attr("title", window.i18n.msgStore['paths-tip']);
  $("#filerSourcename-tip").attr("title", window.i18n.msgStore['sourcename-tip']);
  $("#security-tip").attr("title", window.i18n.msgStore['security-tip']);

  $("#seeds-tip").attr("title", window.i18n.msgStore['seeds-tip']);
  $("#email-tip").attr("title", window.i18n.msgStore['email-tip']);
  $("#exclusions-tip").attr("title", window.i18n.msgStore['exclusions-tip']);
  $("#webSourcename-tip").attr("title", window.i18n.msgStore['sourcename-tip']);

  $(".asteriskLabel").html(window.i18n.msgStore['mandatoryField']);

  $('[data-toggle="tooltip"]').tooltip();

  $("#server").change(function(e) {
    checkServer($("#server"));
  });

  $("#user").change(function(e) {
    checkUser($("#user"));
  });

  $("#password").change(function(e) {
    checkPassword($("#password"));
  });

  $("#paths").change(function(e) {
    checkPaths($("#paths"));
  });

  $("#seeds").change(function(e) {
    checkSeeds($("#seeds"));
  });

  $("#email").change(function(e) {
    checkemail($("#email"));
  });

  $("#exclusions").change(function(e) {
    checkExclusions($("#exclusions"));
  });

  $("#addWeb").submit(function(e) {
    e.preventDefault();
    if (!checkSeeds($("#seeds")) && checkemail($("#email")) && checkExclusions($("#exclusions")) && checkSourcename($("#webSourcename"))) {
      return false;
    } else {
      return addWebConnector();
    }
  });

  $("#addFiler").submit(function(e) {
    e.preventDefault();
    $("#addFilerCheckMessageFailure").hide();
    if (!checkServer($("#server")) && checkUser($("#user")) && checkPassword($("#password")) && checkPaths($("#paths")) && checkSourcename($("#filerSourcename"))) {
      return false;
    } else {
      $.get("./../admin/CheckMCFConfiguration", {
        configuration : "filer"
      }, function(data) {
        if (data.code === 0) {
          if (data.registered === true) {
            return addFilerConnector();
          } else {
            $("#addFilerCheckMessageFailure").html(window.i18n.msgStore['jcifs-not-installed']);
            $("#addFilerCheckMessageFailure").show();
          }

        } else {
          $("#addFilerCheckMessageFailure").html("Unable to parse the connectors.xml file : " + data.status);
          $("#addFilerCheckMessageFailure").show();
        }
      });
    }
  });

});
// ***** Inputs verification and status management ***** //
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

function checkSourcename(element) {
  clearStatus(element);
  setSuccess(element);
  return true;
}

function checkSeeds(element) {
  clearStatus(element);
  let lines = element.val().split('\n');
  // Very loose verification, only checks that it really starts with http or
  // https
  // let URLPattern = /^https?:\/\/.*/;
  // More restrictive pattern matching
  let URLPattern = /^((?:https?:\/\/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))/i;
  let URLRegex = new RegExp(URLPattern);
  let valid = true;
  lines.forEach(function(line) {
    valid = valid && line.match(URLRegex);
  });
  if (valid) {
    setSuccess(element);
    return true;
  } else {
    setError(element);
    return false;
  }
}

function checkemail(element) {
  clearStatus(element);
  let email = element.val();
  let emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/i;
  let emailRegex = new RegExp(emailPattern);
  if (email.match(emailRegex)) {
    setSuccess(element);
    return true;
  } else {
    setError(element);
    return false;
  }
}

function checkExclusions(element) {
  clearStatus(element);
  setSuccess(element);
  return true;
}

function checkServer(element) {
  clearStatus(element);
  setSuccess(element);
  return true;
}

function checkUser(element) {
  clearStatus(element);
  setSuccess(element);
  return true;
}

function checkPassword(element) {
  clearStatus(element);
  setSuccess(element);
  return true;
}

function checkPaths(element) {
  clearStatus(element);
  setSuccess(element);
  return true;
}

function getJobForm() {
  var jobType = document.getElementById("jobType").value
  // clean the response area
  $("#ajaxResponse").empty();
  $('#webJobDiv').hide();
  $('#filerJobDiv').hide();
  $("#addFilerMessageSuccess").hide();
  $("#addWebMessageSuccess").hide();
  // get the language
  if (jobType == "webjob") {
    webJobForm();
  } else if (jobType == "filerjob") {
    filerJobForm();
  }

}

function webJobForm() {
  $('#webJobDiv').show();
}

function filerJobForm() {
  $('#filerJobDiv').show();
}

function addFilerConnector() {
  if ($("#newFilerConfig").hasClass('disabled')) {
    return;
  }
  $("#newFilerConfig").button("loading");
  $('#addFiler').attr("disabled", true);
  // Put the Data of the form into a global variable and serialize it
  formData = $("#addFiler").serialize();
  $.ajax({ // Ajax Request to the doGet of Admin to check if there is already a
    // promoLink with this keyword
    type : "POST",
    url : "./../admin/MCFUISimplified/Filer",
    data : formData,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      data = JSON.parse(data);
      if (data['code'] !== 0) {
        $("#addFilerMessageFailure").show().removeClass("animated fadeOut");
        timeouts.push(setTimeout(function() {
          $("#addFilerMessageFailure").addClass("animated fadeOut");
        }, 1500));
      } else {
        $("#addFiler").trigger("reset");
        var jobStarted = "";
        if (document.getElementById('startJob').checked) {
          jobStarted = " and started";
        }
        var getUrl = window.location;
        var mcfUrl = "/datafari-mcf-crawler-ui/index.jsp?p=showjobstatus.jsp";
        $("#addFilerMessageSuccess").html(
            "<i class='fa fa-check'></i>Job " + data.job_id + " created" + jobStarted
                + " ! Based on your configuration, it may not crawl immediately.\n Check the status in the <a target='_blank' href='" + mcfUrl + "'>Datafari connectors status page</a>");
        $("#addFilerMessageSuccess").show().removeClass("animated fadeOut");
        timeouts.push(setTimeout(function() {
          clearStatus($("#server"));
          clearStatus($("#user"));
          clearStatus($('#password'));
          clearStatus($('#paths'));
        }, 1500));
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#addPromForm").html(jqXHR.responseText);
    },
    // capture the request before it was sent to server
    beforeSend : function(jqXHR, settings) {
      // disable the button until we get the response
      $('#addFiler').attr("disabled", true);
    },
    // this is called after the response or error functions are finsihed
    complete : function(jqXHR, textStatus) {
      // enable the button
      $("#newFilerConfig").button("reset");
    }
  });
}

function addWebConnector() {
  if ($("#newWebConfig").hasClass('disabled')) {
    return;
  }
  $("#newWebConfig").button("loading");
  $('#addWeb').attr("disabled", true);
  // Put the Data of the form into a global variable and serialize it
  formData = $("#addWeb").serialize();
  $.ajax({ // Ajax Request to the doGet of Admin to check if there is already a
    // promoLink with this keyword
    type : "POST",
    url : "./../admin/MCFUISimplified/Web",
    data : formData,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      data = JSON.parse(data);
      if (data['code'] !== 0) {
        $("#addWebMessageFailure").show().removeClass("animated fadeOut");
        timeouts.push(setTimeout(function() {
          $("#addWebMessageFailure").addClass("animated fadeOut");
        }, 1500));
      } else {
        $("#addWeb").trigger("reset");
        var jobStarted = "";
        if (document.getElementById('startJobWeb').checked) {
          jobStarted = " and started";
        }
        var getUrl = window.location;
        var mcfUrl = "/datafari-mcf-crawler-ui/index.jsp?p=showjobstatus.jsp";
        $("#addWebMessageSuccess").html(
            "<i class='fa fa-check'></i>Job " + data.job_id + " created" + jobStarted
                + " ! Based on your configuration, it may not crawl immediately.\n Check the status in the <a target='_blank' href='" + mcfUrl + "'>Datafari connectors status page</a>");
        $("#addWebMessageSuccess").show().removeClass("animated fadeOut");
        timeouts.push(setTimeout(function() {
          clearStatus($("#seeds"));
          clearStatus($("#email"));
          clearStatus($('#exclusions'));
        }, 1500));
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#addPromForm").html(jqXHR.responseText);
    },
    // capture the request before it was sent to server
    beforeSend : function(jqXHR, settings) {
      // disable the button until we get the response
      $('#addWeb').attr("disabled", true);
    },
    // this is called after the response or error functions are finsihed
    complete : function(jqXHR, textStatus) {
      // enable the button
      $("#newWebConfig").button("reset");
      ;
    }
  });
}
