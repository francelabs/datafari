//# sourceURL=/Datafari/resources/js/admin/ajax/synonyms.js

var previous = "";
$(document).ready(function() {
  // If the user refresh the page
  $(window).bind('beforeunload', function() {
    if (document.getElementById("language").value !== "") {
      cleanSem(document.getElementById("language").value);
    }
  });
  // If the user loads an other page
  $("a").click(function(e) {
    if (e.target.className === "ajax-link" || e.target.className === "ajax-link active-parent active") {
      if (document.getElementById("language").value !== "") {
        cleanSem(document.getElementById("language").value);
        $("#ajaxResponse").empty();
      }
    }
  });

  setupLanguage();

});

function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Synonyms'];
  document.getElementById("Modify").innerHTML = window.i18n.msgStore['selectLang'];
  document.getElementById("documentation-synonyms").innerHTML = window.i18n.msgStore['documentation-synonyms'];
  $("#synonymsBox").text(window.i18n.msgStore['adminUI-Synonyms']);
  $("#reload-warning").html(window.i18n.msgStore['adminUI-ReloadWarning']);
  $("#validate").html(window.i18n.msgStore['validModifications']);
  $("#validate").attr('data-loading-text', "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['validModifications']);
  $("#thWords").html(window.i18n.msgStore['words']);
  $("#thSynonyms").html(window.i18n.msgStore['synonyms']);
  $("#synonymsListLabel").html(window.i18n.msgStore['synonymsListLabel']);
  $('#validateModification')
      .html(
          window.i18n.msgStore['adminUI-ValidateModifications']
              + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='You first need to Save your changes before clicking here if you want your modifications to be taken into account'>i</button></span>");
}

function cleanTable() {
  $("#synonymsTableContent").empty();
}

function htmlWord(words) {
  var html = "";
  var arrWords = words.split(",");
  for (var i = 0; i < arrWords.length; i++) {
    var word = arrWords[i].trim();
    html += '<div class="inline_block"><div class="input-group word">' + '<span class="input-group-addon delete"><i class="fas fa-times"></i></span>' + '<input class="form-control ' + word
        + '" value="' + word + '" type="text" disabled/>' + '</div></div>';
  }
  return html
}

function setWordDeleteFunction() {
  $("#synonymsTableContent").find(".delete").click(function(e) {
    var element = $(e.target);
    while (!element.hasClass("inline_block")) {
      element = element.parent();
    }
    element.remove();
  });
}

function setFunctions() {
  $(".addWord").keypress(function(e) {
    if (e.which == 13) {
      var element = $(e.target);
      if (element.val() != "" && element.val().indexOf(",") == -1) {
        element.parent().append(htmlWord(element.val()));
        element.val('');
        setWordDeleteFunction();
      }
    }
  });

  setWordDeleteFunction();
}

function createAddWordDiv() {
  return "<div class='inline_block'><input class='addWord' type='text' placeholder='" + window.i18n.msgStore['synonymsPlaceholder'] + "'/></div>"
}

function getSynonymsList() {
  var synonymsList = {};
  $("#synonymsTableContent").find("tr").each(function() {
    var words = "";
    var synonyms = "";
    if ($(this).find("td").length == 3) {
      for (var cpt = 1; cpt < 3; cpt++) {
        $(this).find("td:nth-child(" + cpt + ")").find(".form-control").each(function() {
          if ($(this).val() != undefined && $(this).val() != "") {
            if (cpt == 1) {
              if (words != "") {
                words += " , ";
              }
              words += $(this).val();
            } else {
              if (synonyms != "") {
                synonyms += " , ";
              }
              synonyms += $(this).val();
            }
          }
        });
      }
      if (words != "" && synonyms != "") {
        synonymsList[words] = synonyms;
      } else {
        synonymsList[Error] = window.i18n.msgStore['synonymsError']
        return synonymsList;
      }
    }
  });
  return synonymsList;
}

function getFile() {
  cleanTable();

  // If the language has been changed while a file has been opened
  if (previous !== "") {
    // Clean the previous semaphore
    cleanSem(previous);
  }
  // clean the response area
  $("#ajaxResponse").empty();
  // get the language
  var language = document.getElementById("language").value;
  previous = language;
  // if a language has been selected
  if (language != "") {
    var dataString = "language=" + language;
    $.ajax({ // Ajax request to get the content
      url : './../admin/Synonyms',
      type : 'GET',
      data : dataString,
      success : function(data, textStatus, jqXHR) {
        // If the semaphore was already acquired
        if (data === "File already in use") {
          $("#synonymsDisplay").hide();
          $("#ajaxResponse").append("<div class=col-sm-4></div>");
          $("#ajaxResponse").append("<h3 class=col-sm-4>" + window.i18n.msgStore['usedFile'] + "</h3>");
          $("#ajaxResponse").append("<div class=col-sm-4></div>");
        }
        // If the servlet catched an exception
        else if (data.toString().indexOf("Error code : ") !== -1) {
          $("#synonymsDisplay").hide();
          $("#ajaxResponse").append("<div class=col-xs-3></div>");
          $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
          $("#ajaxResponse").append("<div class=col-xs-3></div>");
        }
        // Else print the content of the file in a textArea
        else {
          $("#legendDiv").html("<legend>" + window.i18n.msgStore['synonyms'] + "</legend>");
          $("#synonymsDisplay").show();
          for ( var words in data.synonymsList) {
            $("#synonymsTableContent").append(
                "<tr><td>" + createAddWordDiv() + htmlWord(words) + "</td><td>" + createAddWordDiv() + htmlWord(data.synonymsList[words])
                    + "</td><td class='btn-danger'><a><i class='far fa-trash-alt'></i></a></td></tr>");
            $("#synonymsTableContent tr:last td:last").click(function() {
              $(this).parent("tr").remove();
            });
          }
          setFunctions();

          // Add line to add new synonyms
          $("#synonymsTableContent").append(
              "<tr id='addLine'><td colspan=3><div class='row'><div class=\"col-sm-3 col-form-label\"><button class='col-sm btn btn-primary btn-label-left' name='addSynonyms' id='addSynonyms'>"
                  + window.i18n.msgStore['addSynonyms'] + "</button></div></div></td></tr>");
          $("#addSynonyms").click(function() {
            $("#synonymsTableContent").append("<tr><td>" + createAddWordDiv() + "</td><td>" + createAddWordDiv() + "</td><td class='btn-danger'><a><i class='far fa-trash-alt'></i></a></td></tr>");
            $("#synonymsTableContent tr:last td:last").click(function() {
              $(this).parent("tr").remove();
            });
            $("#synonymsTableContent").append($("#addLine"));
            setFunctions();
          });

          // Add validate button function
          $("#validate").click(function() {
            $("#validate").loading("loading");
            var synonymsList = getSynonymsList();
            if (synonymsList[Error] == undefined || synonymsList[Error] == "") {
              $.post('./Synonyms', {
                "synonymsList" : JSON.stringify(synonymsList),
                "language" : $("#language").val()
              }, function(data) {
                if (data.toString().indexOf("Error code : ") !== -1) {
                  $("#synonymsDisplay").hide();
                  $("#ajaxResponse").append("<div class=col-xs-3></div>");
                  $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
                  $("#ajaxResponse").append("<div class=col-xs-3></div>");
                } else {
                  $("#synonymsDisplay").show();
                  $("#ajaxResponse").empty();
                  $("#ajaxResponse").append(window.i18n.msgStore['modifDone']);
                }
                $("#validate").loading("reset");
              });
            } else {
              $("#ajaxResponse").html(synonymsList[Error]);
              $("#validate").loading("reset");
            }
          });
        }
      },
      error : function(jqXHR, textStatus, errorThrown) {
        console.log("Something really bad happened " + textStatus);
        $("#ajaxResponse").html(jqXHR.responseText);
      }
    });
  }
}

function cleanSem(lang) {
  var language = lang;
  if (language != "") { // If the user was editing a file
    var dataString = "language=" + language;
    $.ajax({ // Post request to release the semaphore
      url : './Synonyms',
      type : 'POST',
      data : dataString,
      async : false
    });
  }
}