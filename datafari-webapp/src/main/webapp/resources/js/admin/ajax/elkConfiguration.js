//# sourceURL=/Datafari/resources/js/admin/ajax/elkConfiguration.js

$(document)
    .ready(
        function() {
          var SERVERALLOK = 0;
          var SERVERGENERALERROR = -1;
          var PROBLEMSERVERLDAPCONNECTION = -6;
          var externalELK = false;

          // Init toggle buttons
          $('#elk_activation').bootstrapToggle();

          // Internationalize content
          $("#topbar1").text(window.i18n.msgStore['home']);
          $("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
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
          $("#kibanaURILabel")
              .html(
                  window.i18n.msgStore['kibanaURI']
                      + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='The URI to reach Kibana. By default it is the real IP address of your Datafari server, so if you have externalised your ELK, you need to point to your Kibana host'>i</button></span>");
          $("#authUserLabel")
              .html(
                  window.i18n.msgStore['authUser']
                      + '<span><button type=\'button\' class=\'btn btn-secondary tooltips\' data-toggle=\'tooltip\' data-placement=\'right\' title="This parameter should only be filled if you use ACLs for search. In that case, enter here the user used to crawl the files. Otherwise, ELK won\'t be able to generate statistics on the corpus">i</button></span>');
          $('#ELKSave-button')
              .html(
                  window.i18n.msgStore['adminUI-ELKSave']
                      + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Only saves your modifcations. You need to deactivate/activate ELK (button above) for them to be taken into account'>i</button></span>");
          var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';

          $.get("../SearchExpert/ELKAdmin", function(data) {
            inputActivation(data);
            fillFields(data);
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

          function fillFields(data) {
            if (data.code == 0) {
              $("#elasticsearchPort").val(data.ElasticsearchPort);
              $("#kibanaURI").val(data.KibanaURI);
              $("#authUser").val(data.authUser);
              externalELK = fillExtendedFields(data);

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
              externalELK : externalELK,
              ELKServer : $("#ELKServer").val(),
              ELKScriptsDir : $("#ELKScriptsDir").val()
            }, function(data) {
              inputActivation(data);
            }, "json");
          });

          $("form").submit(
              function(e) {
                e.preventDefault();
                if ($("#kibanaURI").val() != "" && $("#kibanaURI").val() != undefined) {
                  if ((externalELK === true && $("#ELKServer").val() != "" && $("#ELKServer").val() != undefined && $("#ELKScriptsDir").val() != "" && $("#ELKScriptsDir").val() != undefined)
                      || externalELK === false)
                    $.post("../SearchAdministrator/changeELKConf", {
                      KibanaURI : $("#kibanaURI").val(),
                      authUser : $("#authUser").val(),
                      externalELK : getExternalELK(),
                      ELKServer : $("#ELKServer").val(),
                      ELKScriptsDir : $("#ELKScriptsDir").val()
                    }, function(data) {
                      if (data != undefined && data.code != undefined) {
                        if (data.code == 0) {
                          $("#message").html('<i class="fas fa-check"></i> Well Saved').addClass("success").removeClass("error").show();
                        } else {
                          $("#message").html('<i class="fas fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
                        }
                      } else {
                        $("#message").html('<i class="fas fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
                      }
                      setTimeout(function() {
                        $("#message").addClass("animated fadeOut").one(ENDOFANIMATION, function() {
                          $("#message").hide().removeClass("animated fadeOut");
                        });
                      }, 2000);
                    }, "json");
                } else {
                  $("#message").html('<i class="fas fa-times"></i> Please fill in all the fields').addClass("error").removeClass("success").show();
                  setTimeout(function() {
                    $("#message").addClass("animated fadeOut").one(ENDOFANIMATION, function() {
                      $("#message").hide().removeClass("animated fadeOut");
                    });
                  }, 2000);
                }
                return false;
              });

        });