//# sourceURL=/Datafari/resources/js/admin/ajax/helpediotor.js

var previous = "";
$(document).ready(function() {
  setupLanguage();
  getFile();
});

function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-helpPageEditor'];
}

function getFile() {
  // clean the response area
  $("#ajaxResponse").empty();
  $.ajax({ // Ajax request to get the content
    url : './../admin/Help',
    type : 'GET',
    success : function(data, textStatus, jqXHR) {
      // If the servlet catched an exception
      if (data.toString().indexOf("Error code : ") !== -1) {
        $("#ajaxResponse").append("<div class=col-xs-3></div>");
        $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
        $("#ajaxResponse").append("<div class=col-xs-3></div>");
      }
      // Else print the content of the file in a textArea
      else {
        $("#ajaxResponse").append("<legend>" + window.i18n.msgStore['adminUI-helpPage'] + "</legend>");
        $("#ajaxResponse").append("<div class=\"col-sm-1\"</div>");
        $("#ajaxResponse").append("<form class=\"col-sm-11\" id=\"res\">");
        $("#res").append("<div class=\"form-group\" id=\"div1\">");
        $("#div1").append("<fieldset id=\"fields\">");
        $("#fields").append("<textarea id=\"input\" required resizable=\"true\"></textarea>");
        document.getElementById("input").value = data;
        $("#input").cleditor({
          width : "600", // width not including margins, borders or padding
          height : "600", // height not including margins, borders or padding
          controls : // controls to add to the toolbar
          " undo redo | style image link unlink | cut copy paste pastetext | print source",
          styles : // styles in the style popup
          [ [ "Paragraph", "<p>" ], [ "Header 1", "<h1>" ], [ "Header 2", "<h2>" ], [ "Header 3", "<h3>" ], [ "Header 4", "<h4>" ], [ "Header 5", "<h5>" ], [ "Header 6", "<h6>" ] ],
          useCSS : false, // use CSS to style HTML when possible (not supported in ie)
          bodyStyle : // style to assign to document body contained within the editor
          "margin:4px; font:10pt Arial,Verdana; cursor:text"
        });
        $("#fields").append(
            "<button style=\"margin-top : 10px;\" type=\"Submit\" class=\"btn btn-primary btn-label-left\" id=\"submit\" data-loading-text=\"<i class='fa fa-spinner fa-spin'></i> "
                + window.i18n.msgStore['confirm'] + "\">" + window.i18n.msgStore['confirm'] + "</button>");
        $("#div1").append("</fieldset>");
        $("#res").append("</div>");
        $("#ajaxResponse").append("</form>");
        // On submit send the value of the textArea to upload()
        $("#res").submit(function(e) {
          e.preventDefault();
        });
        $("#res").submit(function(e) {
          upload(document.getElementById("input").value);
        });
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#ajaxResponse").html(jqXHR.responseText);
    }
  });
}
function upload(text) {
  $("#submit").loading("loading");
  // Get the language and the content
  var content = text;
  $.ajax({ // Ajax request to rewrite the file
    url : './Help',
    type : 'POST',
    // Form data
    data : {
      content : content
    },
    datetype : "text",
    contenttype : "text-plain/utf-8",
    success : function(data, textStatus, jqXHR) {
      if (data.toString().indexOf("Error code : ") !== -1) {
        $("#ajaxResponse").append("<div class=col-xs-3></div>");
        $("#ajaxResponse").append("<h3 class=col-xs-6>" + data + "</h3>");
        $("#ajaxResponse").append("<div class=col-xs-3></div>");
      } else {
        $("#ajaxResponse").empty();
        $("#ajaxResponse").append(window.i18n.msgStore['modifDone']);
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#ajaxResponse").html(jqXHR.responseText);
    },
    complete : function(jqXHR, textStatus) {
      $("#submit").loading("reset");
    }
  });
}