//# sourceURL=/Datafari/resources/js/admin/ajax/elkConfiguration.js

$(document)
    .ready(
        function() {
          var SERVERALLOK = 0;
          var SERVERGENERALERROR = -1;
          var PROBLEMSERVERLDAPCONNECTION = -6;

          // Init toggle buttons
          $('#elk_activation').bootstrapToggle();

          // Internationalize content
          $("#topbar1").text(window.i18n.msgStore['home']);
          $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
          $("#topbar3").text(window.i18n.msgStore['adminUI-ELKConf']);
          $("#submit").text(window.i18n.msgStore['save']);
          $("#title").text(window.i18n.msgStore['adminUI-ELKConf']);
          $("#documentation-elkconfiguration").text(window.i18n.msgStore['documentation-elkconfiguration']);
          $("#elkActivationLabel")
              .html(
                  window.i18n.msgStore['elkActivationLabel']
                      + " ("
                      + window.i18n.msgStore['no_save_needed']
                      + ")"
                      + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title=' Should ELK (re)start whenever Datafari is (re)started ? (Note that if you switching it from OFF to ON, it will immediately start ELK)'>i</button></span>");
          $('#ELKSave-button')
              .html(
                  window.i18n.msgStore['adminUI-ELKSave']
                      + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Only saves your modifcations. You need to deactivate/activate ELK (button above) for them to be taken into account'>i</button></span>");
          var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';

          $.get("../SearchExpert/ELKAdmin", function(data) {
            inputActivation(data);
          }, "json");

          function inputActivation(data) {
            if (data.code == 0) {
              if (data.ELKactivation == "true") {
                $("#elk_activation").bootstrapToggle('on', true);
              } else {
                $("#elk_activation").bootstrapToggle('off', true);
              }
            } else {
              $("#message").html('<i class="fas fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
            }
          }

          $("#elk_activation").change(function(e) {
            e.preventDefault();
            if ($(this).is(':checked')) {
              var bool = "true";
            } else {
              var bool = "false";
            }
            $.post("../SearchExpert/ELKAdmin", {
              ELKactivation : bool,
            }, function(data) {
              inputActivation(data);
            }, "json");
          });

        });