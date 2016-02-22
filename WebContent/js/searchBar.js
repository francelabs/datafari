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

	var ast = new AjaxFranceLabs.AdvancedSearchTable({
		id : 'advancedSearchTable',
		parent : '#advancedSearch',
		elm : $('#advancedSearchTable'),
		title : 'Advanced Search',
		description : 'Description'
	});
	
	var asf = new AjaxFranceLabs.AdvancedSearchField({
		id : 'advancedSearchField',
		parent : '#advancedSearchTable',
		elm : $('#advancedSearchField'),
		label : 'test field',
		description : 'field descr',
		field : 'title'		
	});
	
	ast.addField(asf);
	
	as.addTable(ast);

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
