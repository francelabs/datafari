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

      // Internationalize content
      $("#topbar1").text(window.i18n.msgStore['home']);
      $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
      $("#topbar3").text(window.i18n.msgStore['adminUI-DuplicatesConf']);
      $("#submit-algorithm").text(window.i18n.msgStore['save']);
      $("#title").text(window.i18n.msgStore['adminUI-DuplicatesConf']);
      $("#algorithm-title").text(window.i18n.msgStore['duplicates-algorithm-conf']);
      $("#duplicatesFiledsLabel").html(window.i18n.msgStore['duplicates-fields']);
      $("#duplicatesQuantLabel").html(window.i18n.msgStore['duplicates-quant']);
      $("#algorithmSaveLabel").html(window.i18n.msgStore['save-and-activate']);
      $("#submit-algorithm").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['save'] + "...");
      var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';

      $.get("../SearchExpert/DuplicatesAdmin", function(data) {
        if (data.error === undefined) {
          $("#duplicatesFields").val(data.fields);
          $("#duplicatesQuant").val(data.quant);
        } else {
          $("#message").html('<i class="fas fa-times"></i> Error: ' + data.error).addClass("error").removeClass("success").show();
        }

      }, "json");

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