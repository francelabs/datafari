(function(window, undefined) {
    var i18n = {
    		
        /* The loaded JSON message store will be set on this object */
        msgStore: {},
        languageUrl : null,
        
        // Keep the selected language variable to update the languageSelector widget
        language : null,
        
        // Available languages for Datafari
        availableLanguages : [ 'en' , 'fr', 'it' ],
        
        // Default language for Datafari
        defaultLanguage : 'en',
        
        persistMsgStore: function(data) {
            if(window.localStorage) {
                localStorage.setItem("msgStore", JSON.stringify(data));
                this.msgStore = data;
            } else {
                this.msgStore = data;
            }
        },
        
        // Called by i18nInit.js
        setLanguageUrl: function(languageUrl) {
            this.languageUrl = languageUrl;
        },
        
        // Called by i18nInit.js
        setLanguage: function(lang) {
        	
        	// Set the language selected, to update the languageSelector widget
        	this.language = lang;
        	
            $.ajax({
                url: this.languageUrl + lang + ".json",
                dataType: "json",
                async: false,
                success: function(data) {
                    i18n.persistMsgStore(data);
                },
                error: function(error) {
                    $.getJSON(this.languageUrl + lang + ".json", function(data) {
                        i18n.persistMsgStore(data);
                    });
                }
            });
        },
        
        initMsgStore: function(options) {

            var lang = "en-US";

            $.ajax({
                url: options.dataUrl,
                success: function(data) {
                    lang = options.supportLocale ? data : data.substring(0, 2);
                    i18n.setLanguage(lang);
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    lang = options.supportLocale ? lang : lang.substring(0, 2);
                    i18n.setLanguage(lang);
                }
            });
        },
        
        // Function called by languageSelector widget on language change event
        userSelected: function(lang) {
            
            // Update the manager and the widgets with the new language
            // Reinit all the application, go to index page, cleaning its cache to force the rebuild of widgets
            location.assign(window.location.origin + '/Datafari/?lang=' + lang);
        },
        
        init: function(options) {

            var localMsgStore = "";

            if(!!window.localStorage) {

                localMsgStore = localStorage.getItem("msgStore");
                
                if(localMsgStore !== null) {
                    this.msgStore = JSON.parse(localMsgStore);
                } else {
                    this.initMsgStore(options);
                }
            } else {
                this.initMsgStore(options);
            }
        }
    };

    /* Expose i18n to the global object */
    window.i18n = i18n;

})(window);