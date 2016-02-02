$(function($) {

	// 'this' is the document (adminUI index page) here
	var elm = $(this);
	
	// Set the language parameter for main search page
	elm.find('#datafariHomePageSearchUiLink').prop('href', "../index.jsp?lang=" + window.i18n.language);
	
	// Set the language parameter for search page
	elm.find('#datafariSearchUiLink').prop('href', "/Datafari/Search?lang=" + window.i18n.language);
	
	elm.find('#datafariSearchUiLink').html(window.i18n.msgStore['adminUI-SearchPage']);
	elm.find('#welcomeAdminUiMsg').html(window.i18n.msgStore['adminUI-Welcome']);
	elm.find('#logout-AdminUI').html(window.i18n.msgStore['logoutAdminUiLink']);
	elm.find('#myAccount-AdminUI').html(window.i18n.msgStore['adminUI-MyAccount']);
	elm.find('#alerts-AdminUI').html(window.i18n.msgStore['adminUI-Alerts']);
	elm.find('#favorites-AdminUI').html(window.i18n.msgStore['adminUI-Favorites']);
	elm.find('#connectors-AdminUI').html(window.i18n.msgStore['adminUI-Connectors']);
	elm.find('#statistics-AdminUI').html(window.i18n.msgStore['adminUI-Statistics']);
	elm.find('#onDemandStatistics-AdminUI').html(window.i18n.msgStore['adminUI-OnDemandStats']);
	elm.find('#usageStatistics-AdminUI').html(window.i18n.msgStore['adminUI-UsageStats']);
	elm.find('#corpusStatistics-AdminUI').html(window.i18n.msgStore['adminUI-CorpusStats']);
	elm.find('#corpusOTStatistics-AdminUI').html(window.i18n.msgStore['adminUI-CorpusOTStats']);
	elm.find('#queryStatistics-AdminUI').html(window.i18n.msgStore['adminUI-QueryStats']);
	elm.find('#elkConfiguration-AdminUI').html(window.i18n.msgStore['adminUI-ELKConf']);
	elm.find('#searchEngineAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
	elm.find('#solrAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SolrAdmin']);
	elm.find('#alertAdmin-AdminUI').html(window.i18n.msgStore['adminUI-AlertAdmin']);
	elm.find('#indexField-AdminUI').html(window.i18n.msgStore['adminUI-IndexField']);
	elm.find('#schemaAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-SchemaAnalysis']);
	elm.find('#sizeLimitation-AdminUI').html(window.i18n.msgStore['adminUI-SizeLimitation']);
	elm.find('#autocompleteConfig-AdminUI').html(window.i18n.msgStore['adminUI-AutocompleteConfig']);
	elm.find('#searchEngineConfig-AdminUI').html(window.i18n.msgStore['adminUI-SearchEngineConfig']);
	elm.find('#promoLinks-AdminUI').html(window.i18n.msgStore['adminUI-PromoLinks']);
	elm.find('#synonyms-AdminUI').html(window.i18n.msgStore['adminUI-Synonyms']);
	// Commented in JSP page 
	//elm.find('#stopwords-AdminUI').html(window.i18n.msgStore['adminUI-Stopwords']);
	elm.find('#fieldWeight-AdminUI').html(window.i18n.msgStore['adminUI-FieldWeight']);
	elm.find('#facetConfig-AdminUI').html(window.i18n.msgStore['adminUI-FacetConfig']);
	elm.find('#deduplication-AdminUI').html(window.i18n.msgStore['adminUI-Deduplication']);
	elm.find('#likesFavoritesSearchEng-AdminUI').html(window.i18n.msgStore['adminUI-LikesAndFavorites']);
	elm.find('#servers-AdminUI').html(window.i18n.msgStore['adminUI-Servers']);
	elm.find('#userManagement-AdminUI').html(window.i18n.msgStore['adminUI-UserManagement']);
	elm.find('#ldapConfig-AdminUI').html(window.i18n.msgStore['adminUI-LdapConfig']);
	elm.find('#modifyUsers-AdminUI').html(window.i18n.msgStore['adminUI-ModifyUsers']);
	elm.find('#addUser-AdminUI').html(window.i18n.msgStore['adminUI-AddUser']);

});