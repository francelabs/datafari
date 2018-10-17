/**
 * logout function that will logout from ManifoldCF before logout from Datafari
 */
function logout() {
	$.get("/datafari-mcf-crawler-ui/logout.jsp", function(data) {
		if (window.i18n.language !== null && window.i18n.language !== undefined){
			window.open("/Datafari/SignOut?lang=" + window.i18n.language,"_self");
		} else {
			window.open("/Datafari/SignOut","_self");
		}
	});
}