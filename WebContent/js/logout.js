/**
 * logout function that will logout from ManifoldCF before logout from Datafari
 */
function logout() {
	$.get("/datafari-mcf-crawler-ui/logout.jsp", function(data) {
		window.open("/Datafari/SignOut","_self");
	});
}