//@ sourceURL=manifoldcf.js


// Be careful: SetInterval executed in every page (and continuously every 250ms) after loading of manifoldcf.html page once
// TODO: use a callback and run the resize only once; when the frame is completely built.

function setupLanguage() {
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href',
			"./index.jsp?lang=" + window.i18n.language);
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Connectors'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Connectors-Admin'];
}

function autoResize() {
	// TODO handle iFrame security issue
	if (document.getElementById("iFrame") !== null) {
		// We have an embedded iFrame
		// TODO instead of an hardcoded size, we should use a callback to be executed after the content of the frame is built
		
		if ($("#iFrame").contents()[0].forms["loginform"] === undefined){
			// we are in the admin pages of MCF
			document.getElementById("iFrame").setAttribute("height",
				880);
		} else {
			// we are in the login page of MCF
			document.getElementById("iFrame").setAttribute("height",
				380);
		}			
	}
}

setupLanguage();

setInterval(autoResize, 250);

// Pragma needed to be able to debug JS in the browser
//# sourceURL=../ajax/js/manifoldcf.js