$(function($) {

	window.i18n.setLanguageUrl('js/AjaxFranceLabs/locale/');
	var agent = window.navigator.userAgent;
	var msie = agent.indexOf("MSIE ");

	// Get the lang parameter, if defined
	var language = $.url('?lang');

	if (language === undefined) {

		if (msie > 0) {
			language = navigator.userLanguage;
		} else {
			language = window.navigator.language;
		}

		language = language.substring(0, 2);

		var urlLang;
		
		// Check if we already have some parameters defined, to add the lang
		// parameter in the correct way
		
		if (window.location.href.indexOf('?') != -1) {
			
			// Add a parameter to the params list
			urlLang = '&lang=' + language
		} else {
			
			// Old URL doesn't contain any parameters, add the first one
			urlLang = '?lang=' + language
		}

		// Add lang parameter to the URL
		window.history.replaceState({
			'lang' : language
		}, 'Datafari home page', window.location.href + urlLang);

	}

	// Check if current language is supported by Datafari UI
	if ($.inArray(language, window.i18n.availableLanguages) === -1) {

		// If the current language is not yet supported by Datafari, default to
		// English, and replace or add the default lang parameter to the URL
		// (supported in HTML 5)

		window.history.replaceState({
			'lang' : window.i18n.defaultLanguage
		}, 'Datafari home page', window.location.href.replace('lang='
				+ language, 'lang=' + window.i18n.defaultLanguage));

		language = window.i18n.defaultLanguage;
	}

	window.i18n.setLanguage(language);

	Manager = new AjaxFranceLabs.Manager({
		serverUrl : 'http://' + window.location.hostname
				+ ':8080/Datafari/SearchProxy/',
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

	Manager.store.addByValue("fl", 'title,url,id,extension');

	Manager.addWidget(new AjaxFranceLabs.LanguageSelectorWidget({
		// Take the languageSelector element by ID.
		elm : $('#languageSelector'),
		id : 'languageSelector'
	}));

});
