// Function called only once, automatically by JavaScript
(function(window, undefined) {
	var i18n = {

		/* The loaded JSON message store will be set on this object */
		msgStore : {},
		languageUrl : null,

		// Keep the selected language variable to update the languageSelector
		// widget
		language : null,

		// Available languages for Datafari
		availableLanguages : [ 'en', 'fr', 'it', 'pt_br' ],

		// Default language for Datafari
		defaultLanguage : 'en',

		persistMsgStore : function(data) {
			if (window.localStorage) {
				localStorage.setItem("msgStore", JSON.stringify(data));
				this.msgStore = data;
			} else {
				this.msgStore = data;
			}
		},

		// Called by i18nInit.js (for AdminUI) and main.js (for SearchUI)
		setLanguageUrl : function(languageUrl) {
			this.languageUrl = languageUrl;
		},

		setLanguage : function(lang) {
			var self = this;
			// Set the language selected, to update the languageSelector widget
			self.language = lang;

			$.ajax({
				url : self.languageUrl + lang + ".json",
				dataType : "json",
				async : false,
				success : function(data) {
					i18n.persistMsgStore(data);
				}
			});
		},

		initMsgStore : function(options) {

			var lang = "en-US";

			$.ajax({
				url : options.dataUrl,
				success : function(data) {
					lang = options.supportLocale ? data : data.substring(0, 2);
					i18n.setLanguage(lang);
				},
				error : function(jqXHR, textStatus, errorThrown) {
					lang = options.supportLocale ? lang : lang.substring(0, 2);
					i18n.setLanguage(lang);
				}
			});
		},

		// Function called by languageSelector widget on language change event
		userSelected : function(lang) {

			// Update the manager and the widgets with the new language
			// Reinit all the application, go to index page, cleaning its cache
			// to force the rebuild of widgets
			// window.location.origin may be undefined for some environments
			location.assign(window.location.protocol +"//" + window.location.host + window.location.pathname + "?lang=" + lang);
		},

		init : function(options) {

			var localMsgStore = "";

			if (!!window.localStorage) {

				localMsgStore = localStorage.getItem("msgStore");

				if (localMsgStore !== null) {
					this.msgStore = JSON.parse(localMsgStore);
				} else {
					this.initMsgStore(options);
				}
			} else {
				this.initMsgStore(options);
			}
		},

		// Called by i18nInit.js (for AdminUI) and main.js (for SearchUI)
		setupLanguage : function(replaceStateMsg) {

			// Get the lang parameter, if defined
			var language = $.url('?lang');

			if (language === undefined) {
				
				if (window.navigator.userAgent.indexOf("MSIE ") > 0) {
					language = navigator.userLanguage;
				} else {
					language = window.navigator.language;
				}

				language = language.substring(0, 2);

				var urlLang;

				// Check if we already have some parameters defined, to add the
				// lang parameter in the correct way

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
				}, replaceStateMsg, window.location.href + urlLang);

			}

			// Check if current language is supported by Datafari UI
			if ($.inArray(language, window.i18n.availableLanguages) === -1) {

				// If the current language is not yet supported by Datafari,
				// default to English, and replace or add the default lang 
				// parameter to the URL (supported in HTML 5)

				window.history.replaceState({
					'lang' : window.i18n.defaultLanguage
				}, replaceStateMsg, window.location.href.replace('lang='
						+ language, 'lang=' + window.i18n.defaultLanguage));

				language = window.i18n.defaultLanguage;
			}
			
			this.setLanguage(language);
		}
	};

	/* Expose i18n to the global object */
	window.i18n = i18n;

})(window);