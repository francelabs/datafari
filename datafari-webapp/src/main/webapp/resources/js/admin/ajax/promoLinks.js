//# sourceURL=/Datafari/resources/js/admin/ajax/promoLink.js

var d;
var formData;
var oldKey = null;
var languages = new Array();

$.ajaxSetup({
  async : false
});
$.getJSON("../GetAvailableLanguages", function(data) {
  $.each(data.availableLanguageList, function(i, language) {
    languages.push(language);
  });
});
$.ajaxSetup({
  async : true
});

$(document).ready(function() {
  addProm();
  getTab();

  setupLanguage();
  // Remove the div of selection, useful on second load of date pickers
  $("a").click(function() {
    if (document.getElementById("ui-datepicker-div") !== null) {
      var element = document.getElementById("ui-datepicker-div");
      element.parentNode.removeChild(element);
    }
  });
});

function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineConfig']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-PromoLinks']);
  $('#promoLinksBox').text(window.i18n.msgStore['adminUI-PromoLinks']);
  document.getElementById("legendAdd").innerHTML = window.i18n.msgStore['promoLinkAdd'];
  document.getElementById("documentation-promolinks").innerHTML = window.i18n.msgStore['documentation-promolinks'];
  $('#legendAdd').append(
      "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='" + window.i18n.msgStore['promolink-default-explanation']
          + "'>i</button></span>");
  document.getElementById("searchBar").placeholder = window.i18n.msgStore['promoLinkSearch'];
  document.getElementById("contextMenu").innerHTML = window.i18n.msgStore['Results'];
}

function getTab() {
  // Clean both responses area
  $("#ajaxResponseBis").empty();
  $("#ajaxResponse").empty();

  // get the search field value
  var keyword = $("input#searchBar").val();
  dataString = "keyword=" + keyword;

  $
      .ajax({ // Ajax request to the doGet of the Admin servlet
        type : "GET",
        url : "./../admin/PromoLink",
        data : dataString,
        // if received a response from the server
        success : function(data, textStatus, jqXHR) {
          if (data.toString().indexOf("Error code : ") !== -1) {
            $("#ajaxResponse").append("<div class=col-xs-3></div>");
            $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
            $("#ajaxResponse").append("<div class=col-xs-3></div>");
            $('#newPromoLink').attr("disabled", true);
          } else {
            // Put the response into a global variable
            d = data;
            // If there is result then print the head of the table
            var numb = data.response.docs.length;
            if (numb != 0) {
              var i = 0;
              $("#ajaxResponse")
                  .append(
                      "<div class=\"col-xs-12\"><div class=\"box\"><div class=\"box-header\"><div class=\"box-name\"><i class=\"fa fa-table\"></i><span>"
                          + window.i18n.msgStore['promoLinkList']
                          + "</span></div><div class=\"box-icons\"><a class=\"collapse-link\"><i class=\"fa fa-chevron-up\"></i></a><a class=\"expand-link\"><i class=\"fa fa-expand\"></i></a><a class=\"close-link\"><i class=\"fa fa-times\"></i></a></div><div class=\"no-move\"></div></div><div id=\"boxCon\" class=\"box-content no-padding\">");
              $("#boxCon").append("<table id=\"table\" class=\"table table-striped table-bordered table-hover table-heading no-border-bottom\">");
              $("#table").append(
                  "<thead><tr><th>#</th><th>" + window.i18n.msgStore['keyword'] + "</th><th>" + window.i18n.msgStore['title'] + " </th><th>" + window.i18n.msgStore['content'] + " </th><th>"
                      + window.i18n.msgStore['dateStart'] + "</th><th>" + window.i18n.msgStore['dateEnd'] + "</th><th></th></tr></thead><tbody>");
              // for each result add a line to the table
              while (i < numb) {
                var key = data.response.docs[i].keyword;
                $("#table")
                    .append(
                        "<tr id=\""
                            + i
                            + "\"><th>"
                            + i
                            + "</th><th><a href=\"javascript: edit("
                            + i
                            + ")\">"
                            + key
                            + "</a></th><th>"
                            + data.response.docs[i].title
                            + "</th><th>"
                            + data.response.docs[i].content
                            + "</th><th>"
                            + formatDate(data.response.docs[i].dateBeginning)
                            + "</th><th>"
                            + formatDate(data.response.docs[i].dateEnd)
                            + "</th><th class=\"btn-danger text-center\"style=\"background-color : #d9534f; position : relative;\"><a href=\"javascript: remove("
                            + i
                            + ")\" style=\"color: #FFFFFF; position: absolute;top: 50%;left: 50%; text-decoration: inherit; -ms-transform: translate(-50%,-50%); -webkit-transform: translate(-50%,-50%); transform: translate(-50%,-50%);\"><i class=\"far fa-trash-alt\" ></i></a></th></tr>");
                i++;
              }
              $("#table").append("</tbody></table>");
              $("#ajaxResponse").append("</div></div></div></div>");
            }
            // If there is no result
            else {
              $("#ajaxResponse").html("<div><b>" + window.i18n.msgStore['nopromoLink'] + "</b></div>");
            }
          }
        },
        // If there was no response from the server
        error : function(jqXHR, textStatus, errorThrown) {
          console.log("Something really bad happened " + textStatus);
          $("#ajaxResponse").html(jqXHR.responseText);
        },
        // capture the request before it was sent to server
        beforeSend : function(jqXHR, settings) {
          // disable the button until we get the response
          $('#myButton').attr("disabled", true);
        },
        // this is called after the response or error functions are finsihed
        complete : function(jqXHR, textStatus) {
          // enable the button
          $('#myButton').attr("disabled", false);
        }

      });
}
function addProm() {
  // print the add promoLink form
  $("#addPromForm").empty();
  $("#addPromFormBis").empty();
  $("#addPromForm").append("<div id=\"addBox\" class=\"box-content\">");
  $("#addBox").append("<form id=\"add\" class=\"form-horizontal\" role=\"form\">");
  // $("#add").append("<span class='fa fa-info-circle default-explanation'><p class='force-font'>" +
  // window.i18n.msgStore['promolink-default-explanation'] + "</p></span>");
  $("#add").append("<fieldset id=\"fieldContent\">");
  $("#fieldContent").append("<legend>" + window.i18n.msgStore['param']);
  $("#fieldContent").append("<div class=\"row\" id=\"div1\">");
  $("#div1").append("<div class=\"col-sm-3\"><span class=\"fa fa-asterisk \" style=\"color : red\"></span><label class=\"col-form-label\">" + window.i18n.msgStore['keyword'] + "</label></div>");
  $("#div1").append("<input required type=\"text\" id=\"keyword\" name=\"keyword\" placeholder=" + window.i18n.msgStore['keyword'] + " class='col-sm-3 form-control'>");
  $("#fieldContent").append("</div>");

  // add title
  $("#fieldContent").append("<div class=\"row\" id=\"div11\">");
  $("#div11").append(
      "<div class=\"col-sm-3\"><span class=\"fa fa-asterisk \" style=\"color : red\"></span><label class=\"col-form-label default-label\">" + window.i18n.msgStore['title'] + " ("
          + window.i18n.msgStore['promolink_default'] + ")</label></div>");
  $("#div11").append("<input required type=\"text\" id=\"title\" name=\"title\" placeholder=" + window.i18n.msgStore['title'] + " class='col-sm-3 form-control' ><div class='col-sm-2'></div>");
  $("#fieldContent").append("</div><div>");

  // add title per language
  $.each(languages, function(index, value) {
    $("#fieldContent").append("<div class=\"row\" id=\"div11" + value + "\">");
    $("#div11" + value + "").append("<div class=\"col-sm-3\"><label class=\"col-form-label\">" + '(' + value + ') ' + window.i18n.msgStore['title'] + "</label></div>");
    $("#div11" + value + "").append("<input type=\"text\" id=\"title_" + value + "\" name=\"title_" + value + "\" placeholder=" + "(" + value + ")" + " class='col-sm-3 form-control'>");
    $("#fieldContent").append("</div>");
  });

  // add content
  $("#fieldContent").append("</div><div class=\"row\" id=\"div2\">");
  $("#div2").append(
      "<div class=\"col-sm-3\"><span class=\"fa fa-asterisk \" style=\"color : red\"></span><label class=\"col-form-label default-label\">" + window.i18n.msgStore['content'] + " ("
          + window.i18n.msgStore['promolink_default'] + ")</label></div>");
  $("#div2").append("<textarea required type=\"text\" id=\"contentPromoLink\" name=\"content\" placeholder=" + window.i18n.msgStore['content'] + " class=\"col-sm-8 form-control\"></textarea>");
  $("#fieldContent").append("</div>");

  // add content per language
  $.each(languages, function(index, value) {
    $("#fieldContent").append("<div class=\"row\" id=\"div2" + value + "\">");
    $("#div2" + value + "").append("<div class=\"col-sm-3\"><label class=\"col-form-label\">" + '(' + value + ') ' + window.i18n.msgStore['content'] + "</label></div>");
    $("#div2" + value + "").append("<textarea type=\"text\" id=\"content_" + value + "\" name=\"content_" + value + "\" placeholder=" + "(" + value + ") class=\"col-sm-8 form-control\"></textarea>");
    $("#fieldContent").append("</div>");
  });

  $("#add").append("</fieldset>");
  $("#add").append("<fieldset id=\"fieldDate\">")
  $("#fieldDate").append("<legend id=\"legendDate\">" + window.i18n.msgStore['dates'] + "</legend>");
  $("#fieldDate").append(
      "<div class=\"row\" id=\"div3\"><label class=\"col-sm-3 col-form-label\">" + window.i18n.msgStore['dateStart']
          + "</label><input type=\"text\" id=\"dateB\" name=\"dateB\" class=\"col-sm-3 form-control\" placeholder=\"Date\"></div>");
  $("#dateB").datepicker({
    setDate : new Date()
  });
  $("#fieldDate").append("<div class=\"row\" id=\"div31\">");
  $("#div31").append("<label class=\"col-sm-3 col-form-label\">" + window.i18n.msgStore['dateEnd'] + "</label>");
  $("#div31").append("<input type=\"text\" id=\"dateE\" name=\"dateE\" class=\"col-sm-3 form-control\" placeholder=\"Date\">");
  $("#dateE").datepicker({
    setDate : new Date()
  });
  $("#div3").append("</div>");
  $("#add").append("</fieldset>");
  $("#add").append("<div class=\"row\" id=\"div4\">");
  $("#div4").append("<div class=\"col-sm-3\"><label id=\"promoLinkConfirm\" class=\"col-form-label\">Click to validate and activate your promolink creation or changes</label></div>");
  $("#div4").append(
      "<div class=\"col-form-label\"><button type=\"Submit\" id=\"newPromoLink\" name=\"addProm\" class=\"btn btn-primary btn-label-left\">" + window.i18n.msgStore['confirm']
          + "</button></div><div class=\"col-sm-6\"></div><label class=\"col-form-label\"><i class=\"fa fa-asterisk\" style=\"color : red\"></i> " + window.i18n.msgStore['mandatoryField']
          + "</label>");
  $("#newPromoLink").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);
  $("#add").append("</div>");
  $("#addBox").append("</form>");
  $("#addPromForm").append("</div></div></div></div>");
  // Stops the submit request
  $("#add").submit(function(e) {
    e.preventDefault();
  });
  // redefines the submit request
  $("#add").submit(function(e) {
    $("#newPromoLink").loading("loading");
    // If it's an add
    if (oldKey == null) {
      verifAdd();
    }// If it's an edit with a modified keyword
    else if (oldKey != document.getElementById("keyword").value) {
      verifAdd();
    }
  });
}
function verifAdd() { // Used to check if ther's already a promoLink with the specified keyword
  // Put the Data of the form into a global variable and serialize it
  var kwrd = $("#keyword").val();
  var kwrdRegex = /[^a-zA-Z0-9\s]/g;
  if (kwrdRegex.test(kwrd)) {
    // Clean the seconde part of the response area
    $("#addPromFormBis").empty();
    // Print the error message
    $("#addPromFormBis").append("<h3 style='color:red;'><i class='fa fa-exclamation-triangle'></i> " + window.i18n.msgStore['errorSpecChars'] + "</h3>");
    $("#addPromFormBis").append("<br /><br />");
    $("#keyword").focus();
  } else {
    formData = $("#add").serialize();
    $.ajax({ // Ajax Request to the doGet of Admin to check if there is already a promoLink with this keyword
      type : "GET",
      url : "./../admin/PromoLink",
      data : formData,
      // if received a response from the server
      success : function(data, textStatus, jqXHR) {
        if (data.toString().indexOf("Error code : ") !== -1) {
          $("#ajaxResponse").append("<div class=col-xs-3></div>");
          $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
          $("#ajaxResponse").append("<div class=col-xs-3></div>");
          $("#newPromoLink").loading("reset");
          $('#newPromoLink').attr("disabled", true);
        } else if (data.response.docs.length > 0 && existsKeyword(data.response.docs, $("#keyword").val())) {// if there is already a
          // promoLink ask for confirm and
          // print the attributes of this
          // promoLink
          $("#addPromFormBis").empty();
          $("#addPromFormBis").append("<div class=\"col-sm-3\"></div>");
          $("#addPromFormBis").append("<h3>" + window.i18n.msgStore['promoLinkAlready'] + "</h3><div class=\"col-sm-3\"></div><br />");
          $("#addPromFormBis").append("<div class=\"col-sm-3\"></div>");
          $("#addPromFormBis").append("<h3>" + window.i18n.msgStore['title'] + data.response.docs[0].title + ", " + window.i18n.msgStore['content'] + data.response.docs[0].content + " </h3>");
          $("#addPromFormBis").append("<div class=\"col-sm-3\"></div></br><div class=\"col-sm-3\"></div>");
          if (data.response.docs[0].dateBeginning != undefined) {
            $("#addPromFormBis").append("<h3>" + window.i18n.msgStore['dateStart'] + data.response.docs[0].dateBeginning + ", </h3>");
            $("#addPromFormBis").append("<div class=\"col-sm-3\"></div><br />");
          }
          if (data.response.docs[0].dateEnd != undefined) {
            $("#addPromFormBis").append("<div class=\"col-sm-3\"></div>");
            $("#addPromFormBis").append("<h3>" + window.i18n.msgStore['dateEnd'] + data.response.docs[0].dateEnd + " </h3>");
          }
          $("#addPromFormBis").append("<div class=\"col-sm-3\"></div>");
          $("#addPromFormBis").append("<br />");
          $("#addPromFormBis").append("<div class=\"col-sm-3\"></div>");
          $("#addPromFormBis").append(
              "<a href=\"javascript: add()\" style=\"color: inherit; text-decoration: inherit;\"><button class=\"btn btn-primary btn-label-left col-sm-1\" id=\"confirm\">"
                  + window.i18n.msgStore['yes'] + "</button></a>");
          $("#addPromFormBis").append("<div class=\"col-sm-3\"></div>");
          $("#addPromFormBis").append(
              "<a href=\"javascript: cancel()\" style=\"color: inherit; text-decoration: inherit;\"><button class=\"btn btn-primary btn-label-left col-sm-1\" id=\"confirm\">"
                  + window.i18n.msgStore['no'] + "</button></a><br />	");
          $("#addPromFormBis").append("<br /><br />");
        }
        // If there are no results then just add the promoLink
        else {
          add();
        }
      },
      error : function(jqXHR, textStatus, errorThrown) {
        console.log("Something really bad happened " + textStatus);
        $("#addPromForm").html(jqXHR.responseText);
      },
      // capture the request before it was sent to server
      beforeSend : function(jqXHR, settings) {
        // disable the button until we get the response
        $('#add').attr("disabled", true);
      },
      // this is called after the response or error functions are finsihed
      complete : function(jqXHR, textStatus) {
        // enable the button
        $('#add').attr("disabled", false);
      }
    });
  }
}

function existsKeyword(docs, keyword) {
  for (var i = 0; i < docs.length; i++) {
    if (docs[i].keyword == keyword) {
      return true;
    }
  }
  return false;
}

function cancel() {
  oldKey = null;
  $("#addPromFormBis").empty();
}
function add() {
  // Clean an eventual confirm button
  $("#addPromFormBis").empty();
  // Get the data of the add form
  var data = formData;
  // If it's an edit with a modified keyword
  if (oldKey != null) {
    data = "oldKey=" + oldKey + "&" + data;
  }
  // Get the Dates
  var dateB = data.substring(data.toString().indexOf("dateB") + 6, data.toString().indexOf("dateE") - 1);
  var dateE = data.substring(data.toString().indexOf("dateE") + 6, data.length);
  // If there are both dates
  if (dateB != "" && dateE != "") {
    // Format them and compare them
    var dateOne = new Date();
    dateOne.setFullYear(dateB.substring(10, 14), dateB.substring(0, 2), dateB.substring(5, 7));
    var dateTwo = new Date();
    dateTwo.setFullYear(dateE.substring(10, 14), dateE.substring(0, 2), dateE.substring(5, 7));
    // If the ending date is posterior to the starting date or if it's the same day
    if (isGreater(dateTwo, dateOne)) {
      $.ajax({ // Ajax request to the doPost of Admin to add the promoLink
        type : "POST",
        url : "./../admin/PromoLink",
        data : data,
        // if received a response from the server
        success : function(data, textStatus, jqXHR) {
          if (data.toString().indexOf("Error code : ") !== -1) {
            $("#ajaxResponse").append("<div class=col-xs-3></div>");
            $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
            $("#ajaxResponse").append("<div class=col-xs-3></div>");
            $('#newPromoLink').attr("disabled", true);
          } else {
            // Clean the reponse Area and the global variable
            $("#addPromForm").empty();
            $("#addPromFormBis").empty();
            document.getElementById('searchBar').value = "";
            formData = null;
            getTab();
            addProm();
            oldKey = null;
          }
        },
        error : function(jqXHR, textStatus, errorThrown) {
          console.log("Something really bad happened " + textStatus);
          $("#addPromForm").html(jqXHR.responseText);
        },
        // capture the request before it was sent to server
        beforeSend : function(jqXHR, settings) {
          // disable the button until we get the response
          $('#add').attr("disabled", true);
        },
        // this is called after the response or error functions are finsihed
        complete : function(jqXHR, textStatus) {
          $("#newPromoLink").loading("reset");
          // enable the button
          $('#add').attr("disabled", false);
        }
      });
    }
    // If the starting date is posterior to the ending date
    else {
      $("#newPromoLink").loading("reset");
      // Clean the seconde part of the response area
      $("#addPromFormBis").empty();
      // Print the error message
      $("#addPromFormBis").append("<div class=col-xs-4></div>");
      $("#addPromFormBis").append("<h3 class=col-xs-4>" + window.i18n.msgStore['errorDate'] + "</h3>");
      $("#addPromFormBis").append("<div class=col-xs-4></div>");
    }
  }
  // If there is a date or none
  else {
    $.ajax({ // Ajax request to the doPost of Admin to add the promoLink
      type : "POST",
      url : "./../admin/PromoLink",
      data : data,
      // if received a response from the server
      success : function(data, textStatus, jqXHR) {
        if (data.toString().indexOf("Error code : ") !== -1) {
          $("#ajaxResponse").append("<div class=col-xs-3></div>");
          $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
          $("#ajaxResponse").append("<div class=col-xs-3></div>");
          $('#newPromoLink').attr("disabled", true);
        } else {
          // Clean the reponse Area and the global variable
          $("#addPromFormBis").empty();
          document.getElementById('searchBar').value = "";
          formData = null;
          getTab();
          addProm();
          oldKey = null;
        }
      },
      error : function(jqXHR, textStatus, errorThrown) {
        console.log("Something really bad happened " + textStatus);
        $("#addPromForm").html(jqXHR.responseText);
      },
      beforeSend : function(jqXHR, settings) {
        // disable the button until we get the response
        $('#add').attr("disabled", true);
      },
      // this is called after the response or error functions are finsihed
      complete : function(jqXHR, textStatus) {
        $("#newPromoLink").loading("reset");
        // enable the button
        $('#add').attr("disabled", false);
      }
    });
  }
}
function edit(i) {
  // Gets the original keyword
  oldKey = d.response.docs[i].keyword;
  // Put the values in the fields
  document.getElementById("keyword").value = d.response.docs[i].keyword;
  document.getElementById("title").value = d.response.docs[i].title;
  document.getElementById("contentPromoLink").value = d.response.docs[i].content;

  $.each(languages, function(index, language) {
    if (d.response.docs[i]["title_" + language] != undefined) {
      document.getElementById("title_" + language).value = d.response.docs[i]["title_" + language];
    }
    if (d.response.docs[i]["content_" + language] != undefined) {
      document.getElementById("content_" + language).value = d.response.docs[i]["content_" + language];
    }
  });

  if (d.response.docs[i].dateBeginning != undefined) {
    document.getElementById("dateB").value = formatDate(d.response.docs[i].dateBeginning);
  } else {
    document.getElementById("dateB").value = "";
  }
  if (d.response.docs[i].dateEnd != undefined) {
    document.getElementById("dateE").value = formatDate(d.response.docs[i].dateEnd);
  } else {
    document.getElementById("dateE").value = "";
  }
  $("#add").submit(function(e) {
    e.preventDefault();
  });
  // Redefines the submit request
  $("#add").submit(function(e) {
    // If it's an edit and the keyword has not been changed
    if (oldKey != null && oldKey == document.getElementById("keyword").value) {
      // Get the edit form's data and serialize it
      var data = $("#add").serialize();
      // Get the dates
      var dateB = data.substring(data.indexOf("dateB") + 6, data.indexOf("dateE") - 1);
      var dateE = data.substring(data.indexOf("dateE") + 6, data.length);
      // If there are both dates
      if (dateB != "" && dateE != "") {
        // Format the dates and compare them
        var dateOne = new Date();
        dateOne.setFullYear(dateB.substring(10, 14), dateB.substring(0, 2), dateB.substring(5, 7));
        var dateTwo = new Date();
        dateTwo.setFullYear(dateE.substring(10, 14), dateE.substring(0, 2), dateE.substring(5, 7));
        // If the ending date is posterior to the starting date or if it's the same day
        if (isGreater(dateTwo, dateOne)) {
          // Add the oldKey to the serialized data and serialize it (if the keyword has not been changed the oldKey will be the same as the
          // current one)
          var datastring = "oldKey=" + oldKey + "&" + $("#add").serialize();
          $.ajax({ // Ajax request to the doPost of Admin to edit the promoLink
            type : "POST",
            url : "./../admin/PromoLink",
            data : datastring,
            // if received a response from the server
            success : function(data, textStatus, jqXHR) {
              if (data.toString().indexOf("Error code : ") !== -1) {
                $("#ajaxResponse").append("<div class=col-xs-3></div>");
                $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
                $("#ajaxResponse").append("<div class=col-xs-3></div>");
                $('#newPromoLink').attr("disabled", true);
              } else {
                // Clean the response area and print all the promoLinks
                $("#ajaxResponse").empty();
                $("#addPromForm").empty();
                $("#addPromFormBis").empty();
                document.getElementById('searchBar').value = "";
                getTab();
                addProm();
                oldKey = null;
              }
            },
            error : function(jqXHR, textStatus, errorThrown) {
              console.log("Something really bad happened " + textStatus);
              $("#ajaxResponse").html(jqXHR.responseText);
            },
            // capture the request before it was sent to server
            beforeSend : function(jqXHR, settings) {
              // disable the button until we get the response
              $('#add').attr("disabled", true);
            },
            // this is called after the response or error functions are finsihed
            complete : function(jqXHR, textStatus) {
              // enable the button
              $('#add').attr("disabled", false);
            }
          });
        }
        // If the starting date is posterior to the ending date
        else {
          // Clean the second response area
          $("#addPromFormBis").empty();
          // Print error message
          $("#addPromFormBis").append("<div class=col-xs-4></div>");
          $("#addPromFormBis").append("<h3 class=col-xs-4>" + window.i18n.msgStore['errorDate'] + "</h3>");
          $("#addPromFormBis").append("<div class=col-xs-4></div>");
        }
      }
      // If there is one date or none
      else {
        // Add the oldKey to the serialized data and serialize it (if the keyword has not been changed the oldKey will be the same as the
        // current one)
        var datastring = "oldKey=" + oldKey + "&" + $("#add").serialize();
        $.ajax({ // Ajax request to the doPost of Admin to edit the promoLink
          type : "POST",
          url : "./../admin/PromoLink",
          data : datastring,
          // if received a response from the server
          success : function(data, textStatus, jqXHR) {
            if (data.toString().indexOf("Error code : ") !== -1) {
              $("#ajaxResponse").append("<div class=col-xs-3></div>");
              $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
              $("#ajaxResponse").append("<div class=col-xs-3></div>");
              $('#newPromoLink').attr("disabled", true);
            } else {
              // Clean the response area and print all the promoLinks
              $("#ajaxResponse").empty();
              $("#addPromForm").empty();
              $("#addPromFormBis").empty();
              document.getElementById('searchBar').value = "";
              getTab();
              addProm();
              oldKey = null;
            }
          },
          error : function(jqXHR, textStatus, errorThrown) {
            console.log("Something really bad happened " + textStatus);
            $("#ajaxResponse").html(jqXHR.responseText);
          },
          // capture the request before it was sent to server
          beforeSend : function(jqXHR, settings) {
            // disable the button until we get the response
            $('#add').attr("disabled", true);
          },
          // this is called after the response or error functions are finsihed
          complete : function(jqXHR, textStatus) {
            // enable the button
            $('#add').attr("disabled", false);
          }
        });
      }
    }
  });

}
function remove(i) {
  // Get the keyword of the selected promoLink and serialize it
  var keyword = d.response.docs[i].keyword;
  var datastring = "keyword=" + keyword;
  $.ajax({ // Ajax request to the doPost of Admin to delete a promoLink
    type : "POST",
    url : "./../admin/PromoLink",
    data : datastring,
    // if received a response from the server
    success : function(textStatus, jqXHR) {
      // Suppress the row of the deleted promoLink
      var line = document.getElementById(i);
      line.parentNode.removeChild(line);
    },
    // If there was no resonse from the server
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#ajaxResponse").html(jqXHR.responseText);
    },
    // capture the request before it was sent to server
    beforeSend : function(jqXHR, settings) {
      // disable the button until we get the response
      $('#myButton').attr("disabled", true);
    },
    // this is called after the response or error functions are finsihed
    complete : function(jqXHR, textStatus) {
      // enable the button
      $('#myButton').attr("disabled", false);
    }
  });
}
function formatDate(date) {// Format the date
  if (!date) {
    return window.i18n.msgStore['undefined'];
  }
  var res = "" + date;
  res = res.substring(5, 7) + "/" + res.substring(8, 10) + "/" + res.substring(0, 4);
  return res;
}
function isGreater(biggerDate, smallerDate) { // Compare two dates
  if (biggerDate.getTime() >= smallerDate.getTime()) {
    return true;
  } else {
    return false;
  }
}