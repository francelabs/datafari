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
          $("#topbar1").html(window.i18n.msgStore['home']);
          $("#topbar2").html(window.i18n.msgStore['adminUI-Connectors']);
          $("#topbar3").html(window.i18n.msgStore['adminUI-Connectors-MCFSimplified']);
          $("#topbar2").html(window.i18n.msgStore['adminUI-Connectors']);
          // Set the i18n for page elements
          $("#box-title").html(window.i18n.msgStore['adminUI-Connectors-MCFSimplified']);
          $("#documentation-mcfsimplified").html(window.i18n.msgStore['documentation-mcfsimplified']);

          $("#mcfsimplified-title-label").html(window.i18n.msgStore['adminUI-MCFSimplified-title']);
          $("#createWebJob").html(window.i18n.msgStore['adminUI-MCFSimplified-createWebJob']);
          $("#createFilerJob").html(window.i18n.msgStore['adminUI-MCFSimplified-createFilerJob']);
          $("#createDbJob").html(window.i18n.msgStore['adminUI-MCFSimplified-dbJobFormEdit']);

          $("#dbJobTitle").html(window.i18n.msgStore['adminUI-MCFSimplified-dbJobFormEdit']);
          $("#dbAddLegend").html(window.i18n.msgStore['param']);
          $("#dbTypeLabel").html(window.i18n.msgStore['dbType']);
          $("#dbHostLabel").html(window.i18n.msgStore['dbHost']);
          $("#dbNameLabel").html(window.i18n.msgStore['dbName']);
          $("#dbConnStrLabel").html(window.i18n.msgStore['dbConnStr']);
          $("#dbUsernameLabel").html(window.i18n.msgStore['filerUser']);
          $("#dbPasswordLabel").html(window.i18n.msgStore['adminUI-Password']);
          $("#dbSeedingLabel").html(window.i18n.msgStore['dbSeeding']);
          $("#dbVersionLabel").html(window.i18n.msgStore['dbVersion']);
          $("#dbAccessTokenLabel").html(window.i18n.msgStore['dbAccessToken']);
          $("#dbDataLabel").html(window.i18n.msgStore['dbData']);
          $("#dbSecurityLabel").html(window.i18n.msgStore['security']);
          $("#dbDuplicatesDetectionLabel").html(window.i18n.msgStore['duplicatesDetection']);
          $("#dbSourcenameLabel").html(window.i18n.msgStore['sourcename']);
          $("#dbReponameLabel").html(window.i18n.msgStore['reponame']);
          $("#dbSourcename").attr("placeholder", window.i18n.msgStore['sourcename']);
          $("#dbReponame").attr("placeholder", window.i18n.msgStore['reponame']);
          $("#dbStartJobLabel").html(window.i18n.msgStore['startJob']);
          $("#dbSeeding").html("SELECT idfield AS $(IDCOLUMN) FROM documenttable WHERE modifydatefield > $(STARTTIME) AND modifydatefield <= $(ENDTIME)");
          $("#dbVersion").html("SELECT idfield AS $(IDCOLUMN), versionfield AS $(VERSIONCOLUMN) FROM documenttable WHERE idfield IN $(IDLIST)");
          $("#dbAccessToken").html("SELECT docidfield AS $(IDCOLUMN), aclfield AS $(TOKENCOLUMN) FROM acltable WHERE docidfield IN $(IDLIST)");
          $("#dbData").html("SELECT idfield AS $(IDCOLUMN), urlfield AS $(URLCOLUMN), datafield AS $(DATACOLUMN) FROM documenttable WHERE idfield IN $(IDLIST)");
          $("#dbSourcename-tip").attr("title", window.i18n.msgStore['sourcename-tip']);
          $("#dbReponame-tip").attr("title", window.i18n.msgStore['reponame-tip']);
          $("#dbSecurity-tip").attr("title", window.i18n.msgStore['dbSecurity-tip']);
          $("#dbType-tip").attr("title", window.i18n.msgStore['dbType-tip']);
          $("#dbHost-tip").attr("title", window.i18n.msgStore['dbHost-tip']);
          $("#dbName-tip").attr("title", window.i18n.msgStore['dbName-tip']);
          $("#dbConnStr-tip").attr("title", window.i18n.msgStore['dbConnStr-tip']);
          $("#dbUsername-tip").attr("title", window.i18n.msgStore['dbUsername-tip']);
          $("#dbPassword-tip").attr("title", window.i18n.msgStore['password-tip']);
          $("#dbSeeding-tip").attr("title", window.i18n.msgStore['dbSeeding-tip']);
          $("#dbVersion-tip").attr("title", window.i18n.msgStore['dbVersion-tip']);
          $("#dbAccessToken-tip").attr("title", window.i18n.msgStore['dbAccessToken-tip']);
          $("#dbData-tip").attr("title", window.i18n.msgStore['dbData-tip']);
          $("#dbDuplicatesDetection-tip").attr("title", window.i18n.msgStore['duplicatesDetection-tip']);

          $("#webJobTitle").html(window.i18n.msgStore['adminUI-MCFSimplified-webjobFormEdit']);
          $("#webAddLegend").html(window.i18n.msgStore['param']);
          $("#webSeedsLabel").html(window.i18n.msgStore['seeds']);
          $("#webEmailLabel").html(window.i18n.msgStore['adminUI-MCFSimplified-email']);
          $("#webSourcenameLabel").html(window.i18n.msgStore['sourcename']);
          $("#webReponameLabel").html(window.i18n.msgStore['reponame']);
          $("#webDuplicatesDetectionLabel").html(window.i18n.msgStore['duplicatesDetection']);
          $("#seeds").attr("placeholder", window.i18n.msgStore['seeds']);
          $("#email").attr("placeholder", window.i18n.msgStore['adminUI-MCFSimplified-email']);
          $("#exclusions").attr("placeholder", window.i18n.msgStore['exclusions']);
          $("#newWebConfig").html(window.i18n.msgStore['confirm']);
          $("#newWebConfig").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);

          $("#filerJobTitle").html(window.i18n.msgStore['adminUI-MCFSimplified-filerjobFormEdit']);
          $("#filerAddLegend").html(window.i18n.msgStore['param']);
          $("#serverLabel").html(window.i18n.msgStore['server']);
          $("#userLabel").html(window.i18n.msgStore['filerUser']);
          $("#passwordLabel").html(window.i18n.msgStore['adminUI-Password']);
          $("#pathsLabel").html(window.i18n.msgStore['paths']);
          $("#filerSourcenameLabel").html(window.i18n.msgStore['sourcename']);
          $("#filerReponameLabel").html(window.i18n.msgStore['reponame']);
          $("#securityLabel").html(window.i18n.msgStore['security']);
          $("#duplicatesDetectionLabel").html(window.i18n.msgStore['duplicatesDetection']);
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
          $("#duplicatesDetection-tip").attr("title", window.i18n.msgStore['duplicatesDetection-tip']);

          $("#seeds-tip").attr("title", window.i18n.msgStore['seeds-tip']);
          $("#email-tip").attr("title", window.i18n.msgStore['email-tip']);
          $("#exclusions-tip").attr("title", window.i18n.msgStore['exclusions-tip']);
          $("#webSourcename-tip").attr("title", window.i18n.msgStore['sourcename-tip']);
          $("#webReponame-tip").attr("title", window.i18n.msgStore['reponame-tip']);
          $("#webDuplicatesDetection-tip").attr("title", window.i18n.msgStore['duplicatesDetection-tip']);
          
          // OCR filer
          $("#filerCreateOCRLabel").html(window.i18n.msgStore['createOCR']);
          $("#filerTikaOCRHostLabel").html(window.i18n.msgStore['tikaOCRHost']);
          $("#filerTikaOCRPortLabel").html(window.i18n.msgStore['tikaOCRPort']);
          $("#filerTikaOCRNameLabel").html(window.i18n.msgStore['tikaOCRName']);
          $("#filerCreateOCR-tip").attr("title", window.i18n.msgStore['createOCR-tip']);
          $("#filerTikaOCRHost-tip").attr("title", window.i18n.msgStore['tikaOCRHost-tip']);
          $("#filerTikaOCRPort-tip").attr("title", window.i18n.msgStore['tikaOCRPort-tip']);
          $("#filerTikaOCRName-tip").attr("title", window.i18n.msgStore['tikaOCRName-tip']);
          
          // OCR web
          $("#webCreateOCRLabel").html(window.i18n.msgStore['createOCR']);
          $("#webTikaOCRHostLabel").html(window.i18n.msgStore['tikaOCRHost']);
          $("#webTikaOCRPortLabel").html(window.i18n.msgStore['tikaOCRPort']);
          $("#webTikaOCRNameLabel").html(window.i18n.msgStore['tikaOCRName']);
          $("#webCreateOCR-tip").attr("title", window.i18n.msgStore['createOCR-tip']);
          $("#webTikaOCRHost-tip").attr("title", window.i18n.msgStore['tikaOCRHost-tip']);
          $("#webTikaOCRPort-tip").attr("title", window.i18n.msgStore['tikaOCRPort-tip']);
          $("#webTikaOCRName-tip").attr("title", window.i18n.msgStore['tikaOCRName-tip']);
          
          // OCR db
          $("#dbCreateOCRLabel").html(window.i18n.msgStore['createOCR']);
          $("#dbTikaOCRHostLabel").html(window.i18n.msgStore['tikaOCRHost']);
          $("#dbTikaOCRPortLabel").html(window.i18n.msgStore['tikaOCRPort']);
          $("#dbTikaOCRNameLabel").html(window.i18n.msgStore['tikaOCRName']);
          $("#dbCreateOCR-tip").attr("title", window.i18n.msgStore['createOCR-tip']);
          $("#dbTikaOCRHost-tip").attr("title", window.i18n.msgStore['tikaOCRHost-tip']);
          $("#dbTikaOCRPort-tip").attr("title", window.i18n.msgStore['tikaOCRPort-tip']);
          $("#dbTikaOCRName-tip").attr("title", window.i18n.msgStore['tikaOCRName-tip']);

          $(".asteriskLabel").html(window.i18n.msgStore['mandatoryField']);

          $('[data-toggle="tooltip"]').tooltip();

          // Filer change functions
          $("#filerCreateOCR").change(function(e) {
            if($('#filerCreateOCR').is(':checked')) {
              $("#filerOCR").show();
            } else {
              $("#filerOCR").hide();
            }
          });
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

          
          // Web change functions
          $("#webCreateOCR").change(function(e) {
            if($('#webCreateOCR').is(':checked')) {
              $("#webOCR").show();
            } else {
              $("#webOCR").hide();
            }
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
          
          
          // DB change functions
          $("#dbCreateOCR").change(function(e) {
            if($('#dbCreateOCR').is(':checked')) {
              $("#dbOCR").show();
            } else {
              $("#dbOCR").hide();
            }
          });
          $("#dbHost").change(function(e) {
            checkElm($("#dbHost"));
          });
          $("#dbName").change(function(e) {
            checkElm($("#dbName"));
          });
          $("#dbUsername").change(function(e) {
            checkElm($("#dbUsername"));
          });
          $("#dbPassword").change(function(e) {
            checkElm($("#dbPassword"));
          });
          $("#dbSeeding").change(function(e) {
            checkSeedingQuery($("#dbSeeding"));
          });
          $("#dbVersion").change(function(e) {
            checkVersionQuery($("#dbVersion"));
          });
          $("#dbData").change(function(e) {
            checkDataQuery($("#dbData"));
          });
          $("#dbAccessToken").change(function(e) {
            checkAccessTokenQuery($("#dbAccessToken"));
          });
          $("#dbSourcename").change(function(e) {
            checkElm($("#dbSourcename"));
          });
          $("#dbReponame").change(function(e) {
            checkRepoName($("#dbReponame"));
          });
          
          $("#addDb").submit(function(e) {
            e.preventDefault();
            $("#addDb").removeClass('was-validated');
            $("#addDbMessageSuccess").hide();
            $("#addDbMessageFailure").hide();
            $("#addDbCheckMessageFailure").hide();
            var form = document.getElementById("addDb");
            if (form.checkValidity() === false || !checkRepoName($("#dbReponame")) || !checkSeedingQuery($("#dbSeeding"))  || !checkVersionQuery($("#dbVersion"))  || !checkDataQuery($("#dbData"))  || !checkAccessTokenQuery($("#dbAccessToken")) || !checkOCR($("#dbCreateOCR"), $("#dbTikaOCRHost"), $("#dbTikaOCRPort"), $("#dbTikaOCRName"))) {
              return false;
            } else {
              return addDbConnector();
            }
          });

          $("#addWeb").submit(function(e) {
            e.preventDefault();
            $("#addWeb").removeClass('was-validated');
            $("#addWebMessageSuccess").hide();
            $("#addWebMessageFailure").hide();
            $("#addWebCheckMessageFailure").hide();
            if (checkSeeds($("#seeds")) && checkEmail($("#email")) && checkElm($("#webSourcename")) && checkRepoName($("#webReponame")) && checkOCR($("#webCreateOCR"), $("#webTikaOCRHost"), $("#webTikaOCRPort"), $("#webTikaOCRName"))) {
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
            if (form.checkValidity() === false || !checkRepoName($("#filerReponame")) || !checkOCR($("#filerCreateOCR"), $("#filerTikaOCRHost"), $("#filerTikaOCRPort"), $("#filerTikaOCRName"))) {
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

function checkOCR(checkboxEl, tikaHostEl, tikaPortEl, tikaNameEl) {
  clearStatus(tikaHostEl);
  clearStatus(tikaPortEl);
  clearStatus(tikaNameEl);
  if (checkboxEl.is(':checked')) {
    if (!tikaHostEl.val()) {
      setErrorStatus(tikaHostEl, null);
      tikaHostEl.addClass("is-invalid");
      return false;
    } else {
      setOkStatus(tikaHostEl);
    }
    if (!tikaPortEl.val()) {
      setErrorStatus(tikaPortEl, null);
      tikaPortEl.addClass("is-invalid");
      return false;
    } else if (isNaN(tikaPortEl.val())) {
      setErrorStatus(tikaPortEl, "Please set a number");
      tikaPortEl.addClass("is-invalid");
      return false;
    } else {
      setOkStatus(tikaPortEl);
    }
    if (!tikaNameEl.val()) {
      setErrorStatus(tikaNameEl, null);
      tikaNameEl.addClass("is-invalid");
      return false;
    } else {
      setOkStatus(tikaNameEl);
    }

    return true;
  } else {
    return true;
  }
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

function checkSeedingQuery(element) {
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, "Please provide a seeding query");
    return false;
  } else {
    let sq = element.val();
    if (sq.indexOf("$(IDCOLUMN)") == -1) {
      setErrorStatus(element, "The seeding query must return $(IDCOLUMN) in the result");
      return false;
    } else {
      setOkStatus(element);
      return true;
    }
  }
}

function checkVersionQuery(element) {
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, "Please provide a version query");
    return false;
  } else {
    let sq = element.val();
    if (sq.indexOf("$(IDCOLUMN)") == -1) {
      setErrorStatus(element, "The version query must return $(IDCOLUMN) in the result");
      return false;
    } else if (sq.indexOf("$(VERSIONCOLUMN)") == -1) {
      setErrorStatus(element, "The version query must return $(VERSIONCOLUMN) in the result");
      return false;
    } else if (sq.indexOf("$(IDLIST)") == -1) {
      setErrorStatus(element, "The version query must return $(IDLIST) in the result");
      return false;
    } else {
      setOkStatus(element);
      return true;
    }
  }
}

function checkDataQuery(element) {
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, "Please provide a data query");
    return false;
  } else {
    let sq = element.val();
    if (sq.indexOf("$(IDCOLUMN)") == -1) {
      setErrorStatus(element, "The data query must return $(IDCOLUMN) in the result");
      return false;
    } else if (sq.indexOf("$(URLCOLUMN)") == -1) {
      setErrorStatus(element, "The data query must return $(URLCOLUMN) in the result");
      return false;
    } else if (sq.indexOf("$(DATACOLUMN)") == -1) {
      setErrorStatus(element, "The data query must return $(DATACOLUMN) in the result");
      return false;
    } else if (sq.indexOf("$(IDLIST)") == -1) {
      setErrorStatus(element, "The data query must return $(IDLIST) in the result");
      return false;
    } else {
      setOkStatus(element);
      return true;
    }
  }
}

function checkAccessTokenQuery(element) {
  clearStatus(element);
  if ($('#dbSecurity').prop('checked') && !$(element).val()) {
    setErrorStatus(element, "Please provide an access token query");
    return false;
  } else {
    if($(element).val()) {
      let sq = element.val();
      if (sq.indexOf("$(IDCOLUMN)") == -1) {
        setErrorStatus(element, "The data query must return $(IDCOLUMN) in the result");
        return false;
      } else if (sq.indexOf("$(TOKENCOLUMN)") == -1) {
        setErrorStatus(element, "The access token query must return $(TOKENCOLUMN) in the result");
        return false;
      } else if (sq.indexOf("$(IDLIST)") == -1) {
        setErrorStatus(element, "The access token query must return $(IDLIST) in the result");
        return false;
      } else {
        setOkStatus(element);
        return true;
      }
    } else {
      setOkStatus(element);
      return true;
    }
  }
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
  $('#dbJobDiv').hide();
  $("#addFilerMessageSuccess").hide();
  $("#addFilerMessageFailure").hide();
  $("#addFilerCheckMessageFailure").hide();
  $("#addDbMessageSuccess").hide();
  $("#addDbMessageFailure").hide();
  $("#addDbCheckMessageFailure").hide();
  $("#addWebMessageSuccess").hide();
  $("#addWebMessageFailure").hide();
  $("#addWebCheckMessageFailure").hide();
  
  // get the language
  if (jobType == "webjob") {
    webJobForm();
  } else if (jobType == "filerjob") {
    filerJobForm();
  } else if (jobType == "dbjob") {
    dbJobForm();
  }

}

function webJobForm() {
  $('#webJobDiv').show();
}

function filerJobForm() {
  $('#filerJobDiv').show();
}

function dbJobForm() {
  $('#dbJobDiv').show();
}

function addDbConnector() {
  if ($("#newDbConfig").hasClass('disabled')) {
    return;
  }
  $("#newDbConfig").loading("loading");
  $('#addDb').attr("disabled", true);
  // Put the Data of the form into a global variable and serialize it
  formData = $("#addDb").serialize();
  $.ajax({ // Ajax Request to the doGet of Admin to check if there is already a
    // promoLink with this keyword
    type : "POST",
    url : "./../admin/MCFUISimplified/Db",
    data : formData,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      data = JSON.parse(data);
      if (data['code'] !== 0) {
        if(data['status'] !== null && data['status'] !== undefined) {
          $("#addDbMessageFailure").html(data['status']);
        } else {
          $("#addDbMessageFailure").html("An unkown problem occurred while saving the configuration");
        }
        $("#addDbMessageFailure").show();
      } else {
        $("#addDb").trigger("reset");
        $("#dbOCR").hide();
        var jobStarted = "";
        if (document.getElementById('dbStartJob').checked) {
          jobStarted = " and started";
        }
        var getUrl = window.location;
        $("#addDbMessageSuccess").html(
            "<i class='fa fa-check'></i>Job " + data.job_id + " created" + jobStarted
                + " ! Based on your configuration, it may not crawl immediately.\n Check the status in the <a target='_blank' href='" + mcfUrl + "'>Datafari connectors status page</a>");
        $("#addDbMessageSuccess").show();
        timeouts.push(setTimeout(function() {
          clearStatus($("#dbHost"));
          clearStatus($("#dbName"));
          clearStatus($('#dbConnStr'));
          clearStatus($('#dbUsername'));
          clearStatus($("#dbPassword"));
          clearStatus($("#dbSourcename"));
          clearStatus($("#dbReponame"));
          clearStatus($("#dbTikaOCRHost"));
          clearStatus($("#dbTikaOCRPort"));
          clearStatus($("#dbTikaOCRName"));
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
      $('#addDb').attr("disabled", true);
    },
    // this is called after the response or error functions are finsihed
    complete : function(jqXHR, textStatus) {
      // enable the button
      $("#newDbConfig").loading("reset");
    }
  });
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
        if(data['status'] !== null && data['status'] !== undefined) {
          $("#addFilerMessageFailure").html(data['status']);
        } else {
          $("#addFilerMessageFailure").html("An unkown problem occurred while saving the configuration");
        }
        $("#addFilerMessageFailure").show();
      } else {
        $("#addFiler").trigger("reset");
        $("#filerOCR").hide();
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
          clearStatus($("#filerReponame"));
          clearStatus($("#filerSourcename"));
          clearStatus($("#filerTikaOCRHost"));
          clearStatus($("#filerTikaOCRPort"));
          clearStatus($("#filerTikaOCRName"));
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
        if(data['status'] !== null && data['status'] !== undefined) {
          $("#addWebMessageFailure").html(data['status']);
        } else {
          $("#addWebMessageFailure").html("An unkown problem occurred while saving the configuration");
        }
        $("#addWebMessageFailure").show();
      } else {
        $("#addWeb").trigger("reset");
        $("#webOCR").hide();
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
          clearStatus($("#webTikaOCRHost"));
          clearStatus($("#webTikaOCRPort"));
          clearStatus($("#webTikaOCRName"));
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
