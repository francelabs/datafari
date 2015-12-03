
$(function($){
	

	window.i18n.setLanguageUrl('./../js/AjaxFranceLabs/locale/');
	var agent = window.navigator.userAgent;
	var msie = agent.indexOf("MSIE ");
	var language ;
	
	if (msie > 0)      
                language = navigator.userLanguage;
            else                 
                language = window.navigator.language;

	var language = language.substring(0, 2);
	if (language != 'fr' && language != 'it'){
		language = 'en';
		
	}
	window.i18n.setLanguage(language);
});