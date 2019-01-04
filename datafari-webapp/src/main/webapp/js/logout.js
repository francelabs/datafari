/**
 * logout function that will logout from ManifoldCF before logout from Datafari
 */
function logout() {
	var getUrl = window.location;
	var mcfUrl = getUrl.protocol + "//" + getUrl.host + ":9080" + "/" + "datafari-mcf-crawler-ui/logout.jsp";
	$.get(mcfUrl, function(data) {
		if (window.i18n.language !== null && window.i18n.language !== undefined){
			window.open("/Datafari/SignOut?lang=" + window.i18n.language,"_self");
		} else {
			window.open("/Datafari/SignOut","_self");
		}
	});
}