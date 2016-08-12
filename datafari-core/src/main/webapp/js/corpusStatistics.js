$(document).ready(function() {
	$.get("../SearchAdministrator/ELKAdmin",function(data){
		if((data.ELKactivation=="true" && data.isELKUp=="true") || data.isELKUp=="true") {
			var kibanaURI = data.KibanaURI;
			if(!kibanaURI.endsWith("/")) {
				kibanaURI += "/";
			}
			if(!kibanaURI.startsWith("http://")) {
				kibanaURI = "http://" + kibanaURI;
			}
			$('#iFrameContent').attr('src', kibanaURI + "#/dashboard/Content-Analysis");
			$('#warning').hide();
		} else if(data.ELKactivation=="true" && data.isELKUp=="false"){
			$('#warning').html('ELK is activated but it is impossible to load the page. Contact an administrator to investigate the problem !');
			$('#warning').show();
		} else {
			$('#warning').html('ELK is not activated, to activate ELK, go to the ELK configuration page !');
			$('#warning').show();
		}
	},"json");
});

setupLanguage();
function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
	 document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	 document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Statistics'];
	 document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-CorpusStats'];
	 document.getElementById("content").setAttribute('style',"background-color : #272b30");
}
	
$("a").click(function(e){
	if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
		document.getElementById("content").setAttribute('style',"background-color : #F0F0F0");
	}
});