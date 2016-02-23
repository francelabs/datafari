$(function($) {
	
	Manager.addWidget(new AjaxFranceLabs.LanguageSelectorWidget({
		// Take the languageSelector element by ID.
		elm : $('#languageSelector'),
		id : 'languageSelector'
	}));
	
	Manager.addWidget(new AjaxFranceLabs.LoginDatafariLinksWidget({
		// Take the loginDatafariLinks element by ID.
		elm : $('#loginDatafariLinks'),
		id : 'loginDatafariLinks'
	}));
	
	Manager.addWidget(new AjaxFranceLabs.SearchBarWidget({
		elm : $('#searchBar'),
		id : 'searchBar',
		autocomplete : true,
		noRequest : true,
		updateBrowserAddressBar : false
	}));

	/*
	* Add advanced search widget
	*/
	
	var as = new AjaxFranceLabs.AdvancedSearchWidget({
		// Take the advancedSearch element by ID.
		elm : $('#advancedSearch'),
		id : 'advancedSearch'
	});
	
	createAdvancedSearchTable(as);

	Manager.addWidget(as);

	Manager.addModule(new AjaxFranceLabs.AutocompleteModule({
		elm : $('.searchBar input[type=text]'),
		searchForItSelf : true
	}));

	Manager.init();

	$('.search').click(search);
	$('.search i').click(search);
	$('.searchBar input[type=button]').click(search);
	$('.searchBar input[type=text]').keypress(function(event) {
		if (event.keyCode === 13) {
			search();
		}
	});
});

function search() {

	var searchType = 'allWords';
	var radios = $('#searchBar').find('.searchMode input[type=radio]');
	$.each(radios, function(key, radio) {
		if (radio.checked) {
			searchType = radio.value;
		}
	});

	window.open('Search?searchType=' + searchType + '&query='
			+ encodeURIComponent($('.searchBar input[type=text]').val())
			+ '&lang=' + window.i18n.language, '_self');
};

function createAdvancedSearchTable(as) {
	
	var ast = new AjaxFranceLabs.AdvancedSearchTable({
		parent : '#advancedSearch',
		title : window.i18n.msgStore['advancedSearch-label'],
		description : window.i18n.msgStore['advancedSearch-descr']
	});
	
	// TODO Call Solr to create rows dynamically, based on the index fields
	
	var asf = new AjaxFranceLabs.AdvancedSearchField({
		parent : '#advancedSearchTable',
		label : window.i18n.msgStore['advancedSearch-title-label'],
		description : window.i18n.msgStore['advancedSearch-title-descr'],
		field : 'title'		
	});
	
	ast.addField(asf);
	
	asf = new AjaxFranceLabs.AdvancedSearchField({
		parent : '#advancedSearchTable',
		label : window.i18n.msgStore['advancedSearch-content-label'],
		description : window.i18n.msgStore['advancedSearch-content-descr'],
		field : 'content'		
	});
	
	ast.addField(asf);
	
	as.addTable(ast);	
};
