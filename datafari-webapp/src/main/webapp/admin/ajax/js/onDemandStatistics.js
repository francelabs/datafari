//# sourceURL=/Datafari/admin/ajax/js/onDemandStatistics.js

setupLanguage();
function setupLanguage() {
  $(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Statistics'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-OnDemandStats'];
}

var previous = 0;
var id;
var compt = 0;
function autoResize() {
  if (previous !== $("#iFrame").contents().height()) {
    previous = $("#iFrame").contents().height();
    document.getElementById("iFrame").setAttribute("height", $("#iFrame").contents().height());
  } else {
    compt++;
    if (compt > 20) {
      window.clearInterval(id);
    }
  }
}
id = setInterval(autoResize, 100);