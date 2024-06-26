//Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax//logsAnalysis.js

$(document).ready(function() {
  $.get("../SearchExpert/ELKAdmin", function(data) {
    if ((data.ELKactivation == "true" && data.isELKUp == "true") || data.isELKUp == "true") {
      var apacheProxy = "@APACHE-PRESENT@";
      if (apacheProxy === false) {
        var kibanaURI = data.KibanaURI;
        if (!kibanaURI.endsWith("/")) {
          kibanaURI += "/";
        }
        if (!kibanaURI.startsWith(window.location.protocol)) {
          kibanaURI = window.location.protocol + "//" + kibanaURI;
        }
      }
      else {
        var kibanaURI = "/app/kibana";
      }
      $('#iFrameContent').attr('src', kibanaURI + "#/dashboard/Global-Datafari-Dashboard");
      $('#warning').hide();
    } else if (data.ELKactivation == "true" && data.isELKUp == "false") {
      $('#warning').html('ELK is activated but it is impossible to load the page. Contact an administrator to investigate the problem !');
      $('#warning').show();
    } else {
      $('#warning').html('ELK is not activated, to activate ELK, go to the ELK configuration page !');
      $('#warning').show();
    }
  }, "json");
});

setupLanguage();
function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Statistics'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-GlobalDatafariErrorsStats'];
}