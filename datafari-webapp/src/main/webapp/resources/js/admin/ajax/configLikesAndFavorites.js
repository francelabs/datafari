//# sourceURL=/Datafari/resources/js/admin/ajax/configLikesAndFavorites.js

$(document).ready(function() {
  // Init toggle buttons
  $('#likesAndFavorites').bootstrapToggle();
  setupLanguage();

  $.post("./ConfigureLikesAndFavorites", {
    initiate : "true"
  }, function(data) {
    if (data.code == 0) {
      if (data.isEnabled == "true") {
        $("#likesAndFavorites").bootstrapToggle('on', true);
      } else {
        $("#likesAndFavorites").bootstrapToggle('off', true);
      }

      $("#likesAndFavorites").change(function click_handler(e) {
        e.preventDefault();
        var element = $(this);

        var enable = "true";
        if (!element.is(':checked')) {
          enable = "false";
        }

        $.post("./ConfigureLikesAndFavorites", {
          enable : enable
        }, function(data) {
          if (data.code == 0) {
            // if all was ok
            message_info.hide();
            message_info.removeClass().addClass(CLASSMESSAGESUCCESS);
            message_info_contenue.text(MESSAGESUCCESS);
            message_info_title.text(TITLESUCCESS);
            message_info.show("slow");
            if (enable === "true") {
              element.bootstrapToggle('on', true);
            } else {
              element.bootstrapToggle('off', true);
            }
          } else {
            // if the server say that there's an error (probably camed from datafarie.properties)
            message_info.hide();
            message_info.removeClass().addClass(CLASSMESSAGEDANGER);
            message_info_contenue.text(MESSAGEERRORINTERN);
            message_info_title.text(TITLEERRORINTERN);
            message_info.show("slow");
          }
        }, "json").fail(function() {
          message_info.hide();
          message_info.removeClass().addClass(CLASSMESSAGEDANGER);
          message_info_contenue.text(MESSAGESERVERDOWN);
          message_info_title.text(TITLESERVERDOWN);
          message_info.show("slow");
        });

      });
    } else {
      message_info.hide();
      message_info.removeClass().addClass(CLASSMESSAGEDANGER);
      message_info_contenue.text(MESSAGEERRORINTERN);
      message_info_title.text(TITLEERRORINTERN);
      message_info.show("slow");
    }

  }, "json");
});

function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-LikesAndFavorites'];

  $("#favorites-title").html(window.i18n.msgStore['adminUI-LikesAndFavorites']);
  $("#favorites-label").html(window.i18n.msgStore['favorites-label']);
}

var message_info = $("#message_info");
var message_info_title = $("#message_info h4");
var message_info_contenue = $("#message_info p");
var CLASSMESSAGE = "bs-callout";
var CLASSMESSAGEDANGER = CLASSMESSAGE + " bt-callout-danger";
var CLASSMESSAGESUCCESS = CLASSMESSAGE + " bt-callout-success";
var MESSAGESERVERDOWN = "The server doesn't respond. Please try again or contact your system adminstrator";
var TITLESERVERDOWN = "The server doesn't respond";
var MESSAGEERRORINTERN = "A problem was encountered while trying to submit your request. The server sent back an error message. Please try again " + "or contact your system adminstrator ";
var TITLEERRORINTERN = "A problem was encountered";
var TITLESUCCESS = "Success";
var MESSAGESUCCESS = "The change was saved"