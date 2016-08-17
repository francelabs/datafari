$(function($) {

	/*
	 * Absolute path instead of js/Ajax.. from the root of the context, to be able to be used for:
	 * - search.jsp (context in /Datafari)
	 * - and login.jsp (context in /Datafari/admin)
	 */ 
	window.i18n.setLanguageUrl('/Datafari/js/AjaxFranceLabs/locale/');
	
	window.i18n.setupLanguage('Datafari home page');

	var port = '8080';
	
	if (typeof portHeader !== 'undefined'){
		port = parseInt(portHeader);
	}
	
	
	Manager = new AjaxFranceLabs.Manager({
		serverUrl : 'http://' + window.location.hostname
				+ ':'+port+'/Datafari/SearchProxy/',
		constellio : false,
		connectionInfo : {
			autocomplete : {
				serverUrl : '',
				servlet : 'suggest',
				queryString : 'q='
			},
			spellcheck : {
				serverUrl : '',
				servlet : '',
				queryString : ''
			}
		}
	});
	

	var languages = ['en'];
	if (typeof langHeader !== 'undefined'){
		languages = langHeader
	}
	
	Manager.addWidget(new AjaxFranceLabs.LanguageSelectorWidget({
		// Take the languageSelector element by ID.
		languages : languages,
		elm : $('#languageSelector'),
		id : 'languageSelector'
	}));

	Manager.store.addByValue("fl", 'title,url,id,extension');
	
	// add Konami code
	var k = [38, 38, 40, 40, 37, 39, 37, 39, 66, 65],
	n = 0;
	$(document).keydown(function (e) {
	    if (e.keyCode === k[n++]) {
	        if (n === k.length) {
	            alert('Congratulation, you found the konami code! More surprises are coming soon!'); 
	            n = 0;
	            return false;
	        }
	    }
	    else {
	        n = 0;
	    }
	});

});
