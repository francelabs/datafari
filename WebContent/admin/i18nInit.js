$(function($) {

	window.i18n.setLanguageUrl('./../js/AjaxFranceLabs/locale/');
	var agent = window.navigator.userAgent;
	var msie = agent.indexOf("MSIE ");
	var language;

	if (msie > 0) {
		language = navigator.userLanguage;
	} else {
		language = window.navigator.language;
	}

	language = language.substring(0, 2);

	if ($.inArray(language, window.i18n.availableLanguages) === -1) {

		// If the current language is not yet supported by Datafari, default to
		// English
		language = window.i18n.defaultLanguage;
	}

	window.i18n.setLanguage(language);
});