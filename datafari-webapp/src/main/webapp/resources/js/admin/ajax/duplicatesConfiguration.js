//# sourceURL=/Datafari/resources/js/admin/ajax/duplicatesConfiguration.js

var timeouts = [];

var clearTimeouts = function() {
  for (var i = 0; i < timeouts.length; i++) {
    clearTimeout(timeouts[i]);
  }

  // quick reset of the timer array you just cleared
  timeouts = [];

  $('#main').unbind('DOMNodeRemoved');
}

$(document).ready(
    function() {
      $('#main').bind('DOMNodeRemoved', clearTimeouts);

      // Init toggle buttons
      $('#duplicates_activation').bootstrapToggle();

      // Internationalize content
      $("#topbar1").text(window.i18n.msgStore['home']);
      $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
      $("#topbar3").text(window.i18n.msgStore['adminUI-DuplicatesConf']);
      $("#submit").text(window.i18n.msgStore['save']);
      $("#submit-algorithm").text(window.i18n.msgStore['save']);
      $("#title").text(window.i18n.msgStore['adminUI-DuplicatesConf']);
      $("#algorithm-title").text(window.i18n.msgStore['duplicates-algorithm-conf']);
      $("#duplicatesActivationLabel").html(window.i18n.msgStore['duplicates-sync-activation']);
      $("#duplicatesHostLabel").html(window.i18n.msgStore['duplicates-host']);
      $("#duplicatesCollectionLabel").html(window.i18n.msgStore['duplicates-collection']);
      $("#duplicatesFiledsLabel").html(window.i18n.msgStore['duplicates-fields']);
      $("#duplicatesQuantLabel").html(window.i18n.msgStore['duplicates-quant']);
      $("#duplicatesConfSaveLabel").html(window.i18n.msgStore['save-and-activate']);
      $("#algorithmSaveLabel").html(window.i18n.msgStore['save-and-activate']);
      $("#submit").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save'] + "...");
      $("#submit-algorithm").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save'] + "...");
      var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';

      $.get("../SearchExpert/DuplicatesAdmin", function(data) {
        if (data.error === undefined) {
          inputActivation(data.enabled);
          $("#duplicatesHost").val(data.host);
          $("#duplicatesCollection").val(data.collection);
          $("#duplicatesFields").val(data.fields);
          $("#duplicatesQuant").val(data.quant);
        } else {
          $("#message").html('<i class="fas fa-times"></i> Error: ' + data.error).addClass("error").removeClass("success").show();
        }

      }, "json");

      function inputActivation(enabled) {
        if (enabled == "true") {
          $("#duplicates_activation").bootstrapToggle('on', true);
        } else {
          $("#duplicates_activation").bootstrapToggle('off', true);
        }
      }

      $("#duplicates_activation").change(function(e) {
        e.preventDefault();
        var checked = "false";
        if ($('#duplicates_activation').is(':checked')) {
          var checked = "true";
        }
        inputActivation(checked);
      });

      $("#duplicates-conf-form").submit(
          function(e) {
            e.preventDefault();
            $("#duplicates-conf-message").css('visibility', 'hidden');
            $("#submit").loading("loading");
            var enabled = "false";
            if ($("#duplicates_activation").is(':checked')) {
              enabled = "true";
            }
            $.post("../SearchExpert/DuplicatesAdmin", {
              config : "synchronization",
              enabled : enabled,
              host : $("#duplicatesHost").val(),
              collection : $("#duplicatesCollection").val()
            }, function(data) {
              $("#submit").loading("reset");
              if (data.error === undefined) {
                $("#duplicates-conf-message").html('<i class="fas fa-check"></i> ' + window.i18n.msgStore['success']).addClass("alert-success").removeClass("alert-danger").removeClass(
                    "animated fadeOut").css('visibility', 'visible');
                timeouts.push(setTimeout(function() {
                  $("#duplicates-conf-message").addClass("animated fadeOut");
                }, 3000));
              } else {
                $("#duplicates-conf-message").html('<i class="fas fa-times"></i> Error: ' + data.error).addClass("alert-danger").removeClass("alert-success").removeClass("animated fadeOut").css(
                    'visibility', 'visible');
              }

            }, "json");
          });

      $("#algorithm-conf-form").submit(
          function(e) {
            e.preventDefault();
            $("#algorithm-conf-message").css('visibility', 'hidden');
            $("#submit-algorithm").loading("loading");
            $.post("../SearchExpert/DuplicatesAdmin", {
              config : "algorithm",
              fields : $("#duplicatesFields").val(),
              quant : $("#duplicatesQuant").val()
            }, function(data) {
              $("#submit-algorithm").loading("reset");
              if (data.error === undefined) {
                $("#algorithm-conf-message").html('<i class="fas fa-check"></i> ' + window.i18n.msgStore['success']).addClass("alert-success").removeClass("alert-danger").removeClass(
                    "animated fadeOut").css('visibility', 'visible');
                timeouts.push(setTimeout(function() {
                  $("#algorithm-conf-message").addClass("animated fadeOut");
                }, 3000));
              } else {
                $("#algorithm-conf-message").html('<i class="fas fa-times"></i> Error: ' + data.error).addClass("alert-danger").removeClass("alert-success").removeClass("animated fadeOut").css(
                    'visibility', 'visible');
              }

            }, "json");
          });

    });