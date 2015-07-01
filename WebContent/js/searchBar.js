$(function($) {
	Manager.addWidget(new AjaxFranceLabs.SearchBarWidget({
		elm : $('#searchBar'),
		id : 'searchBar',
		autocomplete : true,
		noRequest : true,
		updateBrowserAddressBar : false
	}));

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
			+ encodeURIComponent($('.searchBar input[type=text]').val()), '_self');
};
