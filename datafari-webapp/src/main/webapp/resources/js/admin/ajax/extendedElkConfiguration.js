//# sourceURL=/Datafari/resources/js/admin/ajax/extendedElkConfiguration.js

var externalELK = false;

function fillExtendedFields(data) {
  if (data.code == 0) {
    if (data.externalELK == "true") {
      externalELK = true;
      $("#ELKServerDiv").show();
      $("#ELKScriptsDirDiv").show();
    } else {
      externalELK = false;
      $("#ELKServerDiv").hide();
      $("#ELKScriptsDirDiv").hide();
    }
    $("#externalELKLabel input").prop('checked', externalELK);
    $("#ELKServer").val(data.ELKServer);
    $("#ELKScriptsDir").val(data.ELKScriptsDir);

    return externalELK;
  }
}

function getExternalELK() {
  return externalELK;
}

$(document).ready(function() {

  // Internationalize content
  $("#externalELKText").html(window.i18n.msgStore['externalELK']);
  $("#ELKServerLabel").html(window.i18n.msgStore['ELKServer']);
  $("#ELKScriptsDirLabel").html(window.i18n.msgStore['ELKScriptsDir']);

  $("#externalELKInput").change(function() {
    if ($(this).is(':checked')) {
      externalELK = true;
      $("#ELKServerDiv").show();
      $("#ELKScriptsDirDiv").show();
    } else {
      externalELK = false;
      $("#ELKServerDiv").hide();
      $("#ELKScriptsDirDiv").hide();
    }
  });

});
