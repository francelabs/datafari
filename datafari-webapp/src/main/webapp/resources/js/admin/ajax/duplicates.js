// Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax//duplicates.js

var detailsTable;
var rowsPerPages = 10;
var currentDetailedSignature = "";
var currentDetailedDoc = "";
$(document).ready(
    function() {

      // Set the breadcrumbs
      $("#topbar1").html(window.i18n.msgStore['home']);
      $("#topbar2").html(window.i18n.msgStore['adminUI-SystemAnalysis']);
      $("#topbar3").html(window.i18n.msgStore['adminUI-Duplicates']);

      // Set the i18n for page elements
      $("#box-title").html(window.i18n.msgStore['adminUI-Duplicates']);
      $("#duplicate_files").html(window.i18n.msgStore['duplicates-files']);
      $("#dupe_nb").html(window.i18n.msgStore['duplicates-nb']);
      $("#duplicate_file_names").html(window.i18n.msgStore['duplicates-file-names']);
      $("#duplicates-explanation").html(window.i18n.msgStore['duplicates-explanation']);

      $("#duplicates_table tbody").empty();
      detailsTable = $("#duplicates_details_table").DataTable({
        "paging" : false,
        "info" : false,
        "searching" : false
      });

      $.get("../SearchExpert/Duplicates", {
        "action" : "global",
        "page" : "1",
        "limit" : "100"
      }, function(data) {
        if (data.error === undefined) {
          var duplicates = data.duplicates;
          if (duplicates.length > 0) {
            for (var i = 0; i < duplicates.length; i++) {
              $("#duplicates_table tbody").append(
                  "<tr><td class='get-details'><span class='doc'>" + duplicates[i].doc + "</span><span class='signature'>" + duplicates[i].signature + "</span></td><td>" + duplicates[i].duplicates
                      + "</td></tr>")
            }
            $(".get-details").click(function() {
              currentDetailedSignature = $(this).find(".signature").html();
              currentDetailedDoc = $(this).find(".doc").html();
              updateDetails("1");
            });
          } else {
            $("#duplicates_table tbody").html("<tr><td colspan='2'>" + window.i18n.msgStore['duplicates-empty'] + "</td></tr>");
          }
          $("#duplicates_table").DataTable();
        } else {
          $("#duplicate-files-message").html('<i class="fas fa-times"></i> Error: ' + data.error).show();
        }
      }, "json");

    });

// Set page button click
$(document).on("click", "#duplicates_details_table_paginate .paginate_button", function() {
  if (!$(this).hasClass("disabled") && !$(this).hasClass("current")) {
    var currentPage = parseInt($("#duplicates_details_table_paginate span .current").html());
    if ($(this).hasClass("next")) {
      updateDetails(currentPage + 1);
    } else if ($(this).hasClass("previous")) {
      updateDetails(currentPage - 1);
    } else {
      var askedPage = $(this).html();
      updateDetails(askedPage);
    }
  }
});

function updateDetails(page) {
  $("#duplicate-details-message").hide();
  detailsTable.clear();
  $("#details-box-title").html(window.i18n.msgStore['duplicates-detail'] + " " + currentDetailedDoc);
  $.get("../SearchExpert/Duplicates", {
    "action" : "details",
    "signature" : currentDetailedSignature,
    "rows" : rowsPerPages,
    "page" : page
  }, function(data) {
    if (data.error === undefined) {
      var num_found = data.num_found;
      var duplicates = data.duplicates;
      var nbPages = Math.ceil(num_found / rowsPerPages);
      var start = 1;
      if (page > 1) {
        start = ((page - 1) * rowsPerPages) + 1;
      }
      var end = start + duplicates.length - 1;
      if (duplicates.length > 0) {
        for (var i = 0; i < duplicates.length; i++) {
          detailsTable.row.add([ '<a href="' + duplicates[i] + '" target="_blank">' + duplicates[i] + '</a>' ]);
        }
        $("#duplicates_details_table_info").html(window.i18n.msgStore['Results'] + ' ' + start + ' - ' + end + ' ' + window.i18n.msgStore['of'] + ' ' + num_found);
        $("#duplicates_details_table_info").show();
        if (page > 1) {
          $("#duplicates_details_table_previous").removeClass("disabled");
        } else {
          $("#duplicates_details_table_previous").addClass("disabled");
        }
        if (page == nbPages) {
          $("#duplicates_details_table_next").addClass("disabled");
        } else {
          $("#duplicates_details_table_next").removeClass("disabled");
        }
        // Update pages
        $("#duplicates_details_table_paginate span").empty();
        for (var i = 1; i <= nbPages; i++) {
          var currentPage = "";
          if (i == page) {
            currentPage = " current";
          }
          $("#duplicates_details_table_paginate span").append('<a class="paginate_button' + currentPage + '">' + i + '</a>');
        }
      } else {
        detailsTable.row.add([ window.i18n.msgStore['duplicates-empty'] ]);
        $("#duplicates_details_table_info").hide();
        $("#duplicates_details_table_paginate").hide();
      }
      detailsTable.draw();
      $("#details-box").show();
    } else {
      $("#duplicate-details-message").html('<i class="fas fa-times"></i> Error: ' + data.error).show();
    }
  }, "json");
}
