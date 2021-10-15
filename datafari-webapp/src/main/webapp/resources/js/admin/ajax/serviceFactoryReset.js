// Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax//MCFChangePassword.js

$(document).ready(function() {
		setupLanguage();
		
});


	
function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-ServiceAdministration']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-ClusterActions-ServiceFactoryReset']);
	document.getElementById("factoryResetMenu").innerHTML = window.i18n.msgStore['adminUI-ClusterActions-ServiceFactoryReset'];
	document.getElementById("documentation-servicefactoryreset").innerHTML = window.i18n.msgStore['documentation-servicefactoryreset'];
	document.getElementById("factoryResetLabel").innerHTML = window.i18n.msgStore['factoryResetLabel'];
	 
	 
	
}



