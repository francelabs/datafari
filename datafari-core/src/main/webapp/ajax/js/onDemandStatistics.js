
//@ sourceURL=onDemandStatistics.js


setupLanguage();
function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
	 document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	 document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Statistics'];
	 document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-OnDemandStats'];
	 document.getElementById("content").setAttribute('style',"background-color : #272b30");
}


var previous = 0;
var id;
var compt = 0 ;
function autoResize(){
	if (previous!==$("#iFrame").contents().height()){
		previous = $("#iFrame").contents().height();
		document.getElementById("iFrame").setAttribute("height", $("#iFrame").contents().height());
	}else{
		compt++;
		if(compt>20){
			window.clearInterval(id);
		}
	}
}
id = setInterval(autoResize, 100);
	
$("a").click(function(e){
	if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
		document.getElementById("content").setAttribute('style',"background-color : #F0F0F0");
		window.clearInterval(id);
	}
});