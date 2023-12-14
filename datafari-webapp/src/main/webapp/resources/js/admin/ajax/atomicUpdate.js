
$(document).ready(function() {
		setupLanguage();
		
});


function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-Connectors']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-atomicUpdate']);
	document.getElementById("atomicUpdateTitle").innerHTML = window.i18n.msgStore['adminUI-atomicUpdate'];
	document.getElementById("documentation-atomicUpdate").innerHTML = window.i18n.msgStore['documentation-atomicUpdate'];
	document.getElementById("atomicUpdate-doc-label").innerHTML = window.i18n.msgStore['atomicUpdate-doc-label'] + ": ";
	document.getElementById("atomicUpdate-log-link").innerHTML = window.i18n.msgStore['adminUI-Download-Logs'];
}