$(function($) {

	// 'this' is the document (adminUI index page) here
	
	// Set the language parameter for main search page
	$(this).find('#datafariHomePageSearchUiLink').prop('href', "../index.jsp?lang=" + window.i18n.language);
	
	// Set the language parameter for search page
	$(this).find('#datafariSearchUiLink').prop('href', "/Datafari/Search?lang=" + window.i18n.language);
	
	$(this).find('#datafariSearchUiLink').html(window.i18n.msgStore['adminUI-SearchPage']);
	$(this).find('#welcomeAdminUiMsg').html(window.i18n.msgStore['adminUI-Welcome']);
	$(this).find('#logout-AdminUI').html(window.i18n.msgStore['logoutAdminUiLink']);
	$(this).find('#myAccount-AdminUI').html(window.i18n.msgStore['adminUI-MyAccount']);
	$(this).find('#alerts-AdminUI').html(window.i18n.msgStore['adminUI-Alerts']);
	$(this).find('#favorites-AdminUI').html(window.i18n.msgStore['adminUI-Favorites']);
	$(this).find('#connectors-AdminUI').html(window.i18n.msgStore['adminUI-Connectors']);
	$(this).find('#statistics-AdminUI').html(window.i18n.msgStore['adminUI-Statistics']);
	$(this).find('#usageStatistics-AdminUI').html(window.i18n.msgStore['adminUI-UsageStats']);
	$(this).find('#queryStatistics-AdminUI').html(window.i18n.msgStore['adminUI-QueryStats']);
	$(this).find('#searchEngineAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
	$(this).find('#solrAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SolrAdmin']);
	$(this).find('#alertAdmin-AdminUI').html(window.i18n.msgStore['adminUI-AlertAdmin']);
	$(this).find('#indexField-AdminUI').html(window.i18n.msgStore['adminUI-IndexField']);
	$(this).find('#schemaAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-SchemaAnalysis']);
	$(this).find('#sizeLimitation-AdminUI').html(window.i18n.msgStore['adminUI-SizeLimitation']);
	$(this).find('#autocompleteConfig-AdminUI').html(window.i18n.msgStore['adminUI-AutocompleteConfig']);
	$(this).find('#searchEngineConfig-AdminUI').html(window.i18n.msgStore['adminUI-SearchEngineConfig']);
	$(this).find('#promoLinks-AdminUI').html(window.i18n.msgStore['adminUI-PromoLinks']);
	$(this).find('#synonyms-AdminUI').html(window.i18n.msgStore['adminUI-Synonyms']);
	// Commented in JSP page 
	//$(this).find('#stopwords-AdminUI').html(window.i18n.msgStore['adminUI-Stopwords']);
	$(this).find('#fieldWeight-AdminUI').html(window.i18n.msgStore['adminUI-FieldWeight']);
	$(this).find('#facetConfig-AdminUI').html(window.i18n.msgStore['adminUI-FacetConfig']);
	$(this).find('#deduplication-AdminUI').html(window.i18n.msgStore['adminUI-Deduplication']);
	$(this).find('#likesFavoritesSearchEng-AdminUI').html(window.i18n.msgStore['adminUI-LikesAndFavorites']);
	$(this).find('#servers-AdminUI').html(window.i18n.msgStore['adminUI-Servers']);
	$(this).find('#userManagement-AdminUI').html(window.i18n.msgStore['adminUI-UserManagement']);
	$(this).find('#ldapConfig-AdminUI').html(window.i18n.msgStore['adminUI-LdapConfig']);
	$(this).find('#modifyUsers-AdminUI').html(window.i18n.msgStore['adminUI-ModifyUsers']);
	$(this).find('#addUser-AdminUI').html(window.i18n.msgStore['adminUI-AddUser']);

});