// Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax//mcfSimplified.js

var timeouts = [];
var mcfUrl = "@GET-MCF-IP@";
var returnData = undefined;
var clearTimeouts = function() {
  for (var i = 0; i < timeouts.length; i++) {
    clearTimeout(timeouts[i]);
  }

  // quick reset of the timer array you just cleared
  timeouts = [];

  $('#main').unbind('DOMNodeRemoved');
}

$(document)
    .ready(
        function() {
          $('#main').bind('DOMNodeRemoved', clearTimeouts);
          $('backupDir-label').text(window.i18n.msgStore["adminUI-MCFBackupRestore-backupDir-label"]);
          $('.MCFSave')
              .html(
                  window.i18n.msgStore['adminUI-MCFSave']
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
          document.getElementById("createWebJob").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-createWebJob'];
          document.getElementById("createFilerJob").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-createFilerJob'];

          document.getElementById("webJobTitle").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-webjobFormEdit'];
          document.getElementById("webAddLegend").innerHTML = window.i18n.msgStore['param'];
          document.getElementById("webSeedsLabel").innerHTML = window.i18n.msgStore['seeds'];
          document.getElementById("webEmailLabel").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-email'];
          document.getElementById("webSourcenameLabel").innerHTML = window.i18n.msgStore['sourcename'];
          document.getElementById("webReponameLabel").innerHTML = window.i18n.msgStore['reponame'];
          $("#seeds").attr("placeholder", window.i18n.msgStore['seeds']);
          $("#email").attr("placeholder", window.i18n.msgStore['adminUI-MCFSimplified-email']);
          $("#exclusions").attr("placeholder", window.i18n.msgStore['exclusions']);
          $("#newWebConfig").html(window.i18n.msgStore['confirm']);
          $("#newWebConfig").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);

          document.getElementById("filerJobTitle").innerHTML = window.i18n.msgStore['adminUI-MCFSimplified-filerjobFormEdit'];
          document.getElementById("filerAddLegend").innerHTML = window.i18n.msgStore['param'];
          document.getElementById("serverLabel").innerHTML = window.i18n.msgStore['server'];
          document.getElementById("userLabel").innerHTML = window.i18n.msgStore['filerUser'];
          document.getElementById("passwordLabel").innerHTML = window.i18n.msgStore['adminUI-Password'];
          document.getElementById("pathsLabel").innerHTML = window.i18n.msgStore['paths'];
          document.getElementById("filerSourcenameLabel").innerHTML = window.i18n.msgStore['sourcename'];
          document.getElementById("filerReponameLabel").innerHTML = window.i18n.msgStore['reponame'];
          document.getElementById("securityLabel").innerHTML = window.i18n.msgStore['security'];
          $("#startJobLabel").html(window.i18n.msgStore['startJob']);
          $("#startJobWebLabel").html(window.i18n.msgStore['startJob']);
          $("#server").attr("placeholder", window.i18n.msgStore['server']);
          $("#password").attr("placeholder", window.i18n.msgStore['adminUI-Password']);
          $("#user").attr("placeholder", window.i18n.msgStore['filerUser']);
          $("#paths").attr("placeholder", window.i18n.msgStore['paths']);
          $("#filerSourcename").attr("placeholder", window.i18n.msgStore['sourcename']);
          $("#filerReponame").attr("placeholder", window.i18n.msgStore['reponame']);
          $("#newFilerConfig").html(window.i18n.msgStore['confirm']);
          $("#newFilerConfig").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);

          // Set the tooltips
          $("#server-tip").attr("title", window.i18n.msgStore['server-tip']);
          $("#password-tip").attr("title", window.i18n.msgStore['password-tip']);
          $("#user-tip").attr("title", window.i18n.msgStore['user-tip']);
          $("#paths-tip").attr("title", window.i18n.msgStore['paths-tip']);
          $("#filerSourcename-tip").attr("title", window.i18n.msgStore['sourcename-tip']);
          $("#filerReponame-tip").attr("title", window.i18n.msgStore['reponame-tip']);
          $("#security-tip").attr("title", window.i18n.msgStore['security-tip']);

          $("#seeds-tip").attr("title", window.i18n.msgStore['seeds-tip']);
          $("#email-tip").attr("title", window.i18n.msgStore['email-tip']);
          $("#exclusions-tip").attr("title", window.i18n.msgStore['exclusions-tip']);
          $("#webSourcename-tip").attr("title", window.i18n.msgStore['sourcename-tip']);
          $("#webReponame-tip").attr("title", window.i18n.msgStore['reponame-tip']);

          $(".asteriskLabel").html(window.i18n.msgStore['mandatoryField']);

          $('[data-toggle="tooltip"]').tooltip();

          $("#server").change(function(e) {
            checkElm($("#server"));
          });

          $("#user").change(function(e) {
            checkElm($("#user"));
          });

          $("#password").change(function(e) {
            checkElm($("#password"));
          });

          $("#paths").change(function(e) {
            checkElm($("#paths"));
          });

          $("#seeds").change(function(e) {
            checkSeeds($("#seeds"));
          });

          $("#email").change(function(e) {
            checkEmail($("#email"));
          });

          $("#webSourcename").change(function(e) {
            checkElm($("#webSourcename"));
          });
          
          $("#webReponame").change(function(e) {
            checkRepoName($("#webReponame"));
          });

          $("#filerReponame").change(function(e) {
            checkRepoName($("#filerReponame"));
          });

          $("#addWeb").submit(function(e) {
            e.preventDefault();
            $("#addWeb").removeClass('was-validated');
            $("#addWebMessageSuccess").hide();
            $("#addWebMessageFailure").hide();
            $("#addWebCheckMessageFailure").hide();
            if (checkSeeds($("#seeds")) && checkEmail($("#email")) && checkElm($("#webSourcename")) && checkRepoName($("#webReponame"))) {
              return addWebConnector();
            } else {
              return false;
            }
          });

          $("#addFiler").submit(function(e) {
            e.preventDefault();
            $("#addFiler").removeClass('was-validated');
            $("#addFilerMessageSuccess").hide();
            $("#addFilerMessageFailure").hide();
            $("#addFilerCheckMessageFailure").hide();
            var form = document.getElementById("addFiler");
            if (form.checkValidity() === false || !checkRepoName($("#filerReponame"))) {
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

// ***** Inputs verification and status management ***** //
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

function checkRepoName(element) {
  clearStatus(element);
  let htmlElement = document.getElementById(element.attr('id'));
  if (!$(element).val()) {
    setErrorStatus(element, "Please provide a repository name");
    return false;
  } else {
    let repoName = element.val();
    let repoPattern = /^\w+$/;
    let repoRegex = new RegExp(repoPattern);
    if (repoName.match(repoRegex)) {
      setOkStatus(element);
      htmlElement.setCustomValidity("");
      htmlElement.reportValidity();
      return true;
    } else {
      htmlElement.setCustomValidity("Repository name invalid");
      htmlElement.reportValidity();
      setErrorStatus(element, "Repository name can only contain alphanumerical charaters and underscores (\"_\")");
      return false;
    }
  }
}

function checkSeeds(element) {
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, "Please provide at least one seed URL");
    return false;
  } else {
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
      setOkStatus(element);
      return true;
    } else {
      setErrorStatus(element, "Invalid URL pattern");
      return false;
    }
  }
}

function checkEmail(element) {
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, "Please provide an email");
    return false;
  } else {
    let email = element.val();
    let emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/i;
    let emailRegex = new RegExp(emailPattern);
    if (email.match(emailRegex)) {
      setOkStatus(element);
      return true;
    } else {
      setErrorStatus(element, "Invalid email pattern");
      return false;
    }
  }
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
  $("#newFilerConfig").loading("loading");
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
        $("#addFilerMessageFailure").show();
      } else {
        $("#addFiler").trigger("reset");
        var jobStarted = "";
        if (document.getElementById('startJob').checked) {
          jobStarted = " and started";
        }
        var getUrl = window.location;
        $("#addFilerMessageSuccess").html(
            "<i class='fa fa-check'></i>Job " + data.job_id + " created" + jobStarted
                + " ! Based on your configuration, it may not crawl immediately.\n Check the status in the <a target='_blank' href='" + mcfUrl + "'>Datafari connectors status page</a>");
        $("#addFilerMessageSuccess").show();
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
      $("#newFilerConfig").loading("reset");
    }
  });
}

function addWebConnector() {
  if ($("#newWebConfig").hasClass('disabled')) {
    return;
  }
  $("#newWebConfig").loading("loading");
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
        $("#addWebMessageFailure").show();
      } else {
        $("#addWeb").trigger("reset");
        var jobStarted = "";
        if (document.getElementById('startJobWeb').checked) {
          jobStarted = " and started";
        }
        var getUrl = window.location;
        $("#addWebMessageSuccess").html(
            "<i class='fa fa-check'></i>Job " + data.job_id + " created" + jobStarted
                + " ! Based on your configuration, it may not crawl immediately.\n Check the status in the <a target='_blank' href='" + mcfUrl + "'>Datafari connectors status page</a>");
        $("#addWebMessageSuccess").show();
        timeouts.push(setTimeout(function() {
          clearStatus($("#seeds"));
          clearStatus($("#email"));
          clearStatus($('#webSourcename'));
          clearStatus($('#webReponame'));
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
      $("#newWebConfig").loading("reset");
      ;
    }
  });
}
