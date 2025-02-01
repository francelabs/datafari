$(document).ready(function() {

  $('#userSavedSearchLink').click(function() {
    initParametersUI();
    changeContent("savedsearch");
  });

  $('#userAlertsLink').click(function() {
    initParametersUI();
    changeContent("alert");
  });

  // Internationalize content
  $("#lang-label").text(window.i18n.msgStore['facetlanguage']);
  $("#alert-label").text(window.i18n.msgStore['alerts']);
  $("#savedsearch-label").text(window.i18n.msgStore['savedsearch']);
  $("#param-label").text(window.i18n.msgStore['param']);

  var param = retrieveParamValue();
  if (param === "alert" || param === "savedsearch") {

    initParametersUI();
    changeContent(param);
  }

  let hash = window.location.hash;
  if (hash === "#alert") {
    initParametersUI();
    changeContent("alert");
  } else if (hash === "#savedsearch") {
    initParametersUI();
    changeContent("savedsearch");
  }

});
var d;
var alertsTable;
var searchesTable;

function initParametersUI() {
  hideSearchView();
  clearActiveLinks();
  $("#dropdown-search-tools").addClass("active");
  $("#parametersUi").removeClass('force-hide');
}

function retrieveParamValue() {
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0] == "param") {
      return pair[1];
      break;
    }
  }
}

function changeContent(param) {
  if (param == "alert") {
    $("#param-content-title").text(window.i18n.msgStore['param-alert']);
    createAlertContent();
  } else if (param == "savedsearch") {
    $("#param-content-title").text(window.i18n.msgStore['param-savedsearch']);
    createSavedSearchContent();
  }
}

function createSavedSearchContent() {
  $.ajax({ // Ajax request to the doGet of the Alerts servlet
    type : "POST",
    url : "./GetSearches",
    beforeSend : function(jqXHR, settings) {
      $("#param-content").html("<center><div class=\"bar-loader\" style=\"display : block; height : 32px; width : 32px;\"></div></center>");
    },
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if (data.code == 0) {
        if (data.searchesList !== undefined && !jQuery.isEmptyObject(data.searchesList)) {
          $("#param-content").html(
              "<table id='searchesTable' class='table table-striped table-bordered'><thead><tr><th>" + window.i18n.msgStore['search'] + "</th><th>" + window.i18n.msgStore['link'] + "</th><th>"
                  + window.i18n.msgStore['delete'] + "</th></tr></thead><tbody></tbody></table>");
          $.each(data.searchesList, function(name, search) {
            var line = $('<tr class="tr">' + '<td>' + name + '</td>' + '<td><a href="/Datafari/Search?lang=' + window.i18n.language + '&request=' + encodeURIComponent(search) + '">'
                + window.i18n.msgStore['exec-search'] + '</a></td>' + "<td><a class='delete-button'>x</a></td>" + '</tr>');
            line.data("id", search);
            line.data("name", name);
            $("#searchesTable tbody").append(line);
          });
          searchesTable = $("#searchesTable").DataTable({
            "info" : false,
            "lengthChange" : false,
            "searching" : false,
            "columns" : [ null, {
              "orderable" : false
            }, {
              "orderable" : false
            } ]
          });
          $('.delete-button').click(function(e) {
            var element = $(e.target);
            while (!element.hasClass('tr')) {
              element = element.parent();
            }
            $.post("./deleteSearch", {
              name : element.data('name'),
              request : element.data('id')
            }, function(data) {
              if (data.code == 0) {
                searchesTable.row(element).remove().draw();

                var nbData = searchesTable.column(0).data().length
                if (nbData < 1) {
                  $("#param-content").html("<div><b>" + window.i18n.msgStore["nosavedsearches"] + "</b></div>");
                  destroySavedSearchesTable();
                }
              } else {
                console.log(data.status);
              }
            }).fail(function() {
              console.log(window.i18n.msgStore['dbError']);
            });
          });
        } else {
          $("#param-content").html("<div><b>" + window.i18n.msgStore["nosavedsearches"] + "</b></div>");
        }
      } else {
        console.log(window.i18n.msgStore['dbError']);
      }
    },

    // If there was no response from the server
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
    }
  });
}

function createAlertContent() {
  var dataString = "keyword=";
  var alertAggregatorWarning = "Reminder: in case you are using the aggregator mode of Datafari with multiple Datafaris, alerts are only monitoring the documents indexed in your main Datafari";
  if (window.i18n.msgStore['alertAggregatorWarning']) {
    alertAggregatorWarning = window.i18n.msgStore['alertAggregatorWarning'];
  }
  $("#param-content").html("<div id='addAlertWarning' class='documentation-style'><p class='documentation-preview'>" + alertAggregatorWarning + "</p></div><div id='addAlertDiv'><button onclick='javascript:addAlert();' id='addAlertButton'>" + window.i18n.msgStore['addAlert'] + "</button></div>");
  $("#param-content").append("<div id='alertsListDiv'></div>");
  $.ajax({ // Ajax request to the doGet of the Alerts servlet
    type : "GET",
    url : "./Alerts",
    data : dataString,
    beforeSend : function(jqXHR, settings) {
      $("#alertsListDiv").html("<center><div class=\"bar-loader\" style=\"display : block; height : 32px; width : 32px;\"></div></center>");
    },
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if (data.toString().indexOf("Error code : ") !== -1) {
        console.log(data);
      } else if (data.alerts != undefined && data.alerts.length > 0) {
        $("#alertsListDiv").html(
            "<table id='alerts_table' class='table table-striped table-bordered'><thead><tr><th>" + window.i18n.msgStore['search'] + "</th><th>" + window.i18n.msgStore['subject'] + "</th><th>"
                + window.i18n.msgStore['mail'] + "</th><th>" + window.i18n.msgStore['send-frequency'] + "</th><th>" + window.i18n.msgStore['delete'] + "</th></tr></thead><tbody></tbody></table>");
        // get the data in a global var so it can be used in edit() or remove()
        d = data;
        var numb = data.alerts.length;
        var i = 0;
        while (i < numb) { // While they are still alerts to print
          var doc = data.alerts[i];
          // Print the alert with an href of the keyword towards edit()
          $("#alerts_table tbody").append(
              "<tr id=\"alert-" + i + "\"><td><span class='alert_search_term'>" + doc.keyword + "</span></td><td>" + doc.subject + "</td><td>" + doc.mail + "</td><td id='frequency-" + i
                  + "' class='frequency'>" + window.i18n.msgStore[doc.frequency] + " <span class='modify-link'><button onclick='javascript: modify(" + i + ")'>" + window.i18n.msgStore['modify']
                  + "</button></span></td><td class='delete-column'><a href=\"javascript: remove(" + i + ")\" class='delete-button'>x</a></td></tr>");
          // Print a button with an href towards remove()
          i++;
        }
        alertsTable = $("#alerts_table").DataTable({
          "info" : false,
          "lengthChange" : false,
          "searching" : false,
          "columns" : [ null, null, null, null, {
            "orderable" : false
          } ]
        });
      } else {
        $("#alertsListDiv").html("<div><b>" + window.i18n.msgStore['noAlerts'] + "</b></div>");
      }
    },

    // If there was no response from the server
    error : function(jqXHR, textStatus, errorThrown) {
      if (jqXHR.responseText === "error connecting to the database") {
        console.log(window.i18n.msgStore['dbError'])
      } else {
        console.log("Something really bad happened " + textStatus);
      }
    }
  });
}

function addAlert() {
  $("#addAlertDiv").html("");
  $("#addAlertDiv").append("<form id=\"add\" role=\"form\">");
  $("#add").append("<table id=\"addAlertTable\" ></table>");
  var tr = $("<tr id='alert-type-tr'>");
  tr.append("<td><label>" + window.i18n.msgStore['alert-type'] + "</label></td>");
  var selectAlertType = $("<select id='select-alert-type' name='alert-type'><option selected value='current-query'>" + window.i18n.msgStore['current-query'] + "</option><option value='custom'>"
      + window.i18n.msgStore['custom-alert'] + "</option></select>");
  selectAlertType.change(function() {
    if ($(this).val() == "custom") {
      var tr = $("<tr id='custom-alert-tr'>");
      tr.append("<td><label>" + window.i18n.msgStore['keyword'] + "</label></td>");
      tr.append("<td><input required type=\"text\" id=\"keyword\" name=\"keyword\" placeholder=" + window.i18n.msgStore['keyword'] + "/></td>");
      $("#alert-type-tr").after(tr);
    } else {
      $("#custom-alert-tr").remove();
    }
  });
  var td = $("<td>");
  td.append(selectAlertType);
  tr.append(td);
  $("#addAlertTable").append(tr);
  tr = $("<tr>");
  tr.append("<td><label>" + window.i18n.msgStore['subject'] + "</label></td>");
  tr.append("<td><input required type=\"text\" id=\"subject\" name=\"subject\" placeholder=" + window.i18n.msgStore['subject'] + "/></td>");
  $("#addAlertTable").append(tr);
  tr = $("<tr>");
  tr.append("<td><label>" + window.i18n.msgStore['mail'] + "</label></td>");
  tr.append("<td><input required type=\"text\" id=\"mail\" name=\"mail\" placeholder=" + window.i18n.msgStore['mail'] + "/></td>");
  $("#addAlertTable").append(tr);
  tr = $("<tr style='display: none;'>");
  tr.append("<td><label>" + window.i18n.msgStore['core'] + "</label></td>");
  tr.append("<td><input required type=\"text\" id=\"core\" name=\"core\" placeholder=\"Core\" value=\"@MAINCOLLECTION@\"/></td>");
  $("#addAlertTable").append(tr);
  tr = $("<tr>");
  tr.append("<td><label>" + window.i18n.msgStore['frequency'] + "</label></td>");
  tr.append("<td><select required id=\"frequency\" name=\"frequency\">	<OPTION value='hourly'>" + window.i18n.msgStore['hourly'] + "</OPTION><OPTION value='daily'>" + window.i18n.msgStore['daily']
      + "</OPTION><OPTION value='weekly'>" + window.i18n.msgStore['weekly'] + "</OPTION></select></td>");
  $("#addAlertTable").append(tr);
  tr = $("<tr>");
  tr.append("<td colspan=2 id='addAlertSubmit'><input type=\"Submit\" id=\"newAlerts\" name=\"AddAlert\" value=\"" + window.i18n.msgStore['confirm'] + "\"/><button id='addAlertCancel'>"
      + window.i18n.msgStore['cancel'] + "</button></td>");
  $("#addAlertTable").append(tr);
  $("#addAlertDiv").append("</form>");
  $("#addAlertDiv").append("<div id='addAlertMessage'></div>");

  $("#addAlertCancel").click(function() {
    initCreateAlertButton();
  });

  $("#add").submit(function(e) {
    e.preventDefault();
  });

  $("#add").submit(function(e) {
    var datastring = $("#add").serialize();
    if ($("#select-alert-type").val() == "current-query") {
      var query = "filters=";
      var fqs = window.Manager.store.values("fq");
      for (var i = 0; i < fqs.length; i++) {
        query += encodeURIComponent(fqs[i] + "&");
      }
      // Get query value from window.location thanks to getParamValue method from search.js file
      var rawQuery = getParamValue('query', decodeURIComponent(window.location.search));
      query += "&keyword=" + rawQuery;
      datastring += "&" + query;
    }
    $.ajax({ // Ajax request to the doPost of the Alerts servlet
      type : "POST",
      url : "./Alerts",
      data : datastring,
      // if received a response from the server
      success : function(data, textStatus, jqXHR) {
        if (data.toString().indexOf("Error code : ") !== -1) {
          $("#addAlertMessage").addClass("fail");
          $("#addAlertMessage").html(data);
        } else {
          $("#addAlertMessage").addClass("success");
          $("#addAlertMessage").html(window.i18n.msgStore['success']);
          $("#addAlertMessage").fadeOut(1500, function() {
            destroyAlertsTable();
            createAlertContent();
          });
        }
      },
      error : function(jqXHR, textStatus, errorThrown) {
        if (jqXHR.responseText === "error connecting to the database") {
          $("#addAlertsForm").append(window.i18n.msgStore['dbError'])
        } else {
          console.log("Something really bad happened " + textStatus);
          $("#addAlertsForm").html(jqXHR.responseText);
        }
      },
      // capture the request before it was sent to server
      beforeSend : function(jqXHR, settings) {
        // disable the button until we get the response
        $('#newAlerts').attr("disabled", true);
      },
      // called after the response or error functions are finsihed
      complete : function(jqXHR, textStatus) {
        // enable the button
        $('#add').attr("disabled", false);
      }
    });
  });
}

function initCreateAlertButton() {
  $("#addAlertDiv").html("<button onclick='javascript:addAlert();' id='addAlertButton'>" + window.i18n.msgStore['addAlert'] + "</button>");
}

function modify(i) {
  $("#frequency-" + i).html(
      "<select required id=\"select-frequency-" + i + "\" name=\"frequency\" class=\"col-sm-4\">	<OPTION value='hourly'>" + window.i18n.msgStore['hourly'] + "</OPTION><OPTION value='daily'>"
          + window.i18n.msgStore['daily'] + "</OPTION><OPTION value='weekly'>" + window.i18n.msgStore['weekly'] + "</OPTION></select> <button onclick='javascript: validate(" + i + ")' >"
          + window.i18n.msgStore['validate'] + "</button>")
  $("#select-frequency-" + i).val(d.alerts[i].frequency);
}

function validate(i) {
  var id = "_id=" + d.alerts[i]._id;
  var datastring = "";
  for ( var key in d.alerts[i]) {
    if (datastring != "") {
      datastring += "&";
    }
    var value = "";
    if (key === "frequency") {
      value = $("#select-frequency-" + i).val();
    } else {
      value = d.alerts[i][key];
    }
    datastring += key + "=" + value;
  }
  $.ajax({ // Ajax request to the doPost of the Alerts servlet
    type : "POST",
    url : "./Alerts",
    data : datastring,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if (data.toString().indexOf("Error code : ") !== -1) {
        console.log(data);
      } else {
        d.alerts[i].frequency = $("#select-frequency-" + i).val();
        d.alerts[i]._id = JSON.parse(data).uuid;
        alertsTable.cell("#frequency-" + i).data(
            window.i18n.msgStore[d.alerts[i].frequency] + " <span class='modify-link'><button onclick='javascript: modify(" + i + ")'>" + window.i18n.msgStore['modify'] + "</button></span>").draw();
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
    }
  });
}

function remove(i) {
  // get the id of the alert to remove and serialize it
  var id = "_id=" + d.alerts[i]._id;
  $.ajax({ // Ajax request to the doPost of the Alerts servlet
    type : "POST",
    url : "./Alerts",
    data : id,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if (data.toString().indexOf("Error code : ") !== -1) {
        console.log(data);
      } else {
        // Suppress the row of the tab that was show the now removed alert
        // var row = document.getElementById(i);
        // row.parentNode.removeChild(row);
        alertsTable.row("#alert-" + i).remove().draw();

        var nbData = alertsTable.column(0).data().length
        if (nbData < 1) {
          $("#alertsListDiv").html("<div><b>" + window.i18n.msgStore['noAlerts'] + "</b></div>");
          destroyAlertsTable();
        }
      }
    },
    // If there was no resonse from the server
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
    }
  });
}

function destroyDatatables() {
  destroyAlertsTable();
  destroySavedSearchesTable();
}

function destroySavedSearchesTable() {
  if (searchesTable !== undefined) {
    searchesTable.clear();
    searchesTable.destroy(true);
    searchesTable = undefined;
  }
}

function destroyAlertsTable() {
  if (alertsTable !== undefined) {
    alertsTable.clear();
    alertsTable.destroy(true);
    alertsTable = undefined;
  }
}

$(function() {

});
