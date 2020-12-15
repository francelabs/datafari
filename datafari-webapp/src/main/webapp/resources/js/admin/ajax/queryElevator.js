// Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax/queryElevator.js

$(document).ready(function() {
  setupLanguage();

  // Init toggle buttons
  $('#query-elevator_activation').bootstrapToggle();
  $("#query-elevator_activation").change(function(e) {
    e.preventDefault();
    if ($(this).is(':checked')) {
      var bool = "true";
    } else {
      var bool = "false";
    }
    $.post("../WidgetManager", {
      id: "queryelevator",
      activated: bool
    }, function(data) {
      inputActivation(data);
    }, "json");
  });

  var aggregatorEnabled = false;

  // Check if search aggregation is enabled or not.
  // If it is enable then the query elevator graphical feature must be disabled to avoid confusion and errors with the doc boosts
  $.ajax({
    url: '../SearchAdministrator/searchAggregatorConfig',
    success: function(data) {
      if (data.code == 0) {
        aggregatorEnabled = data.activated;
      }
    },
    dataType: "json",
    async: false
  });

  fillQuerySelector();
  var core = "FileShare";

  if (aggregatorEnabled == true) {
    $("#query-elevator_activation").bootstrapToggle('off', true).change();
    $('#query-elevator_activation').bootstrapToggle('disable');
    $("#query-elevator-activation-message").show();
  } else {
    $("#query-elevator-activation-message").hide();
    $('#query-elevator_activation').bootstrapToggle('enable');
    $.get("../WidgetManager", {
      id: "queryelevator"
    }, function(data) {
      if (data.code == 0) {
        if (data.activated == "true") {
          $("#query-elevator_activation").bootstrapToggle('on', true);
        } else {
          $("#query-elevator_activation").bootstrapToggle('off', true);
        }
      }
    });
  }

  function inputActivation(data) {
    if (data.code == 0) {
      if (data.activated == "true") {
        $("#query-elevator_activation").bootstrapToggle('on', true);
      } else {
        $("#query-elevator_activation").bootstrapToggle('off', true);
      }
    } else {
      $("#messageActivation").html('<i class="fas fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
    }
  }

  // Make the docsTableContent lines drag and droppable
  $("#docsTableContent").sortable({

    helper: function(e, tr) {
      var $originals = tr.children();
      var $helper = tr.clone();
      $helper.children().each(function(index) {
        // Set helper cell sizes to match the original sizes
        $(this).width($originals.eq(index).width());
      });
      return $helper;
    },

    stop: function(event, ui) {
      refreshPositions();
    }

  }).disableSelection();

  // Set the onChange function of the select query element
  $("#query").change(function() {
    getQuery()
  });

  // Set the onClick function of the saveElevateConf button
  $("#saveElevateConf").click(function(e) {
    e.preventDefault();
    if ($("#query").val() !== "") {
      $("#queryElevatorModal_confirm").prop("onclick", null).off("click");
      $("#queryElevatorModal_confirm").click(function() {
        $("#queryElevatorModal").modal('hide');
        // Disable the button until the called servlet responds
        $("#saveElevateConf").loading("loading");
        $("#message").hide();
        var docsList = new Array();
        $("#docsTableContent tr").each(function(index) {
          docsList[index] = $(this).attr("id");
        });
        $.post("../SearchExpert/queryElevator", {
          query: $("#query").val(),
          docs: docsList,
          tool: "modify"
        }, function(data) {
          if (data.code == 0) {
            $("#message").html(window.i18n.msgStore["LocalConfSaved"]);
            $("#message").addClass("success");
            $("#message").show();
            $.get("../SearchExpert/zookeeperConf?action=upload_and_reload", function(data) {
              // Re-enable the button
              $("#saveElevateConf").loading("reset");
              if (data.code == 0) {
                $("#messageZK").html(window.i18n.msgStore["zkOK"]);
                $("#messageZK").addClass("success");
                $("#messageZK").show();
                $("#message").fadeOut(8000);
                $("#messageZK").fadeOut(8000);
              } else {
                $("#messageZK").html(window.i18n.msgStore["zkDown"]);
                $("#messageZK").addClass("error");
                $("#messageZK").show();
              }
            }, "json");
          } else {
            // Re-enable the button
            $("#saveElevateConf").loading("reset");
            $("#message").html(window.i18n.msgStore["LocalConfSaveError"]);
            $("#message").addClass("error");
            $("#message").show();
          }
        }, "json");
      });
      $("#queryElevatorModal").modal();
    }
  });

  // Set the onClick function of the deleteElevateConf button
  $("#deleteElevateConf").click(function(e) {
    e.preventDefault();
    if ($("#query").val() !== "") {
      $("#queryElevatorModal_confirm").prop("onclick", null).off("click");
      $("#queryElevatorModal_confirm").click(function() {
        $("#queryElevatorModal").modal('hide');
        // Disable the button until the called servlet responds
        $("#deleteElevateConf").loading("loading");
        $("#message").hide();
        $.post("../SearchExpert/queryElevator", {
          query: $("#query").val(),
          tool: "delete"
        }, function(data) {
          if (data.code == 0) {
            $("#message").html(window.i18n.msgStore["ConfDeleted"]);
            $("#message").addClass("success");
            $("#message").show();
            fillQuerySelector();
            $.get("../SearchExpert/zookeeperConf?action=upload_and_reload", function(data) {
              // Re-enable the button
              $("#deleteElevateConf").loading("reset");
              if (data.code == 0) {
                $("#messageZK").html(window.i18n.msgStore["zkOK"]);
                $("#messageZK").addClass("success");
                $("#messageZK").show();
                $("#message").fadeOut(8000);
                $("#messageZK").fadeOut(8000);
              } else {
                $("#messageZK").html(window.i18n.msgStore["zkDown"]);
                $("#messageZK").addClass("error");
                $("#messageZK").show();
              }
            }, "json");
          } else {
            // Re-enable the button
            $("#deleteElevateConf").loading("reset");
            $("#message").html(window.i18n.msgStore["ConfDeletedError"]);
            $("#message").addClass("error");
            $("#message").show();
          }
        }, "json");
      });
      $("#queryElevatorModal").modal();
    }
  });

  // Set the onClick function of the addDocButton button
  $("#addDocButton").click(function() {
    addNewDocLine();
  });

  // Set the onClick function of the saveNewElevate button
  $("#saveNewElevate").click(function(e) {
    e.preventDefault();
    var docsList = new Array();
    $(".docInput").each(function(index) {
      if ($(this).val()) {
        docsList[index] = $.trim($(this).val());
      }
    });
    var queryVal = $.trim($("#queryInput").val());
    if (queryVal !== "" && docsList.length > 0) {

      $("#queryElevatorModal_confirm").prop("onclick", null).off("click");
      $("#queryElevatorModal_confirm").click(function() {
        $("#queryElevatorModal").modal('hide');

        // Disable the button until the called servlet responds
        $("#saveNewElevate").loading("loading");
        $("#message2").hide();

        $.post("../SearchExpert/queryElevator", {
          query: queryVal,
          docs: docsList,
          tool: "create"
        }, function(data) {
          if (data.code == 0) {
            $("#message2").html(window.i18n.msgStore["LocalConfSaved"]);
            $("#message2").addClass("success");
            $("#message2").show();
            fillQuerySelector();
            reinitCreateTbody();
            $.get("../SearchExpert/zookeeperConf?action=upload_and_reload", function(data) {
              // Re-enable the button
              $("#saveNewElevate").loading("reset");
              if (data.code == 0) {
                $("#messageZK2").html(window.i18n.msgStore["zkOK"]);
                $("#messageZK2").addClass("success");
                $("#messageZK2").show();
                $("#message2").fadeOut(8000);
                $("#messageZK2").fadeOut(8000);
              } else {
                $("#messageZK2").html(window.i18n.msgStore["zkDown"]);
                $("#messageZK2").addClass("error");
                $("#messageZK2").show();
              }
            }, "json");
          } else {
            // Re-enable the button
            $("#saveNewElevate").loading("reset");
            $("#message2").html(window.i18n.msgStore["LocalConfSaveError"]);
            $("#message2").addClass("error");
            $("#message2").show();
          }
        }, "json");
      });

    }

  });
});

function reinitCreateTbody() {
  $("#createTbody").empty();
  $("#createTbody").append(
    "<tr>" + "<td><input type='text' class='textInput' id='queryInput'/></td>" + "<td><input type='text' class='textInput docInput'/></td>"
    + "<td><img src='../images/icons/plus-icon-32x32.png' id='addDocButton'/></td>" + "</tr>");
  $("#addDocButton").click(function() {
    addNewDocLine();
  });
}

// Refresh the position of elements in docsTableContent
function refreshPositions() {
  $("#docsTableContent tr").each(function(index) {
    $(this).find('.position').html((index + 1));
  });
}

function addNewDocLine() {
  $("#createTbody").append("<tr><td/><td><input type='text' class='textInput docInput'/></td><td/></tr>");
  $("#addDocButton").appendTo("#createTbody tr:last td:last");
}

function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-QueryElevator'];
  document.getElementById("documentation-queryelevator").innerHTML = window.i18n.msgStore['documentation-queryelevator'];
  document.getElementById("selectQuery").innerHTML = window.i18n.msgStore['selectQuery']
    + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Select a query which already contains elevated documents'>i</button></span>";
  document.getElementById("modifyElevateLabel").innerHTML = window.i18n.msgStore['modifyElevateLabel'];
  document.getElementById("modifyDocsOderLabel").innerHTML = window.i18n.msgStore['modifyDocsOderLabel']
    + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='The documents are ordered by priority of appearance in the search results, which is also indicated by their \"Position\" number. Here you can simply drag and drop the documents to change their position in the search results. You can also remove some of them if you want by clicking on their associated trash icon'>i</button></span>";
  document.getElementById("elevatorDocsListLabel").innerHTML = window.i18n.msgStore['elevatorDocsListLabel'];
  $("#saveElevateConf").html(window.i18n.msgStore["confirm"]);
  $("#saveElevateConf").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);
  $("#deleteElevateConf").html(window.i18n.msgStore["deleteElevatorConf"]);
  $("#deleteElevateConf").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['deleteElevatorConf']);
  $('.confirmElevateConf').html(window.i18n.msgStore['adminUI-ClickConfirm']);
  $('#deleteElevateConfButton')
    .html(
      window.i18n.msgStore['adminUI-ClickDelete']
      + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Clicking Delete will erase all the boosts for the documents listed above, for the currently selected query'>i</button></span>");

  $("#createElevateLabel").html(window.i18n.msgStore["createElevateLabel"]);
  $("#queryThLabel").html(window.i18n.msgStore["queryThLabel"]);
  $("#saveNewElevate").html(window.i18n.msgStore["confirm"]);
  $("#saveNewElevate").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);
  $("#addDocButton").attr("title", window.i18n.msgStore["elevateAddDoc"]);
  $("#query-elevator-ui-desc").html(window.i18n.msgStore["query-elevator-ui-desc"]);
  
  $("#query-elevator-activation-message").html(window.i18n.msgStore["query-elevator-activation-message"] + ": <a href=\"https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/22052868/Boosting+documents+from+the+Search+UI\" target=\"_blank\">documentation</a>");

  // Set the tooltips
  $("#deleteElevateConf").attr("title", window.i18n.msgStore['deleteElevateConf-tip']);
  $("#queryElevatorModalLabel").html("<i class='fas fa-exclamation-triangle'></i> " + window.i18n.msgStore['warning']);
  $("#queryElevatorModal_cancel").html(window.i18n.msgStore['cancel']);
  $("#queryElevatorModal .modal-body").html(window.i18n.msgStore['solr-interruption']);
}

function fillQuerySelector() {
  // Clean the docs list
  $("#docsTableContent").empty();

  $.get("../SearchExpert/queryElevator", {
    get: "queries"
  }).done(function(data) {
    // Clean the select
    $("#query").empty();

    var queries = data.queries;
    var sel = document.getElementById('query');

    // Create default empty option
    var opt = document.createElement('option');
    opt.innerHTML = "";
    opt.value = "";
    sel.appendChild(opt);

    //
    for (var i = 0; i < queries.length; i++) {
      opt = document.createElement('option');
      opt.innerHTML = queries[i];
      opt.value = queries[i];
      sel.appendChild(opt);
    }
  }, "json");
}

function getQuery() {
  // Clean the docs list
  $("#docsTableContent").empty();

  // Clean potential message
  $("#message").empty();

  // get the selected query
  var query = document.getElementById("query").value;
  if (query != "") {
    $.get("../SearchExpert/queryElevator", {
      get: "docs",
      query: query
    }).done(
      function(data) {
        for (var i = 0; i < data.docs.length; i++) {
          $("#docsTableContent").append(
            "<tr class='movable_line' id='" + data.docs[i] + "'><td>" + data.docs[i] + "</td><td class='position'>" + (i + 1)
            + "</td><td class='btn-danger'><a class='delete'><i class='far fa-trash-alt'></i></a></td></tr>");
          $("#docsTableContent tr:last td:last").click(function() {
            $(this).parent("tr").remove();
            refreshPositions();
          });
        }
      }, "json");
  }
}