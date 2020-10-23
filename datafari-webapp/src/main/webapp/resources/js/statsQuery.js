$(function($) {

	window.i18n.setLanguageUrl('../js/AjaxFranceLabs/locale/');
	window.i18n.setLanguage('en');

	Manager = new AjaxFranceLabs.Manager({
		serverUrl : 'http://' + window.location.hostname
				+ ':8080/Datafari/SearchProxy/',
		servlet : 'statsQuery',
		constellio : false
	});

});



$(function($) {
	var query = getParamValue('query', decodeURIComponent(window.location.search));
	var id = getParamValue('id', decodeURIComponent(window.location.search));
	var mode;
	var q;
	
	if (id !== ''){
		q = "id:\""+id+"\"";
		mode = 'queryID';
	} else {
		if (query !== ''){
			q = "q:\""+query+"\"";
			mode = 'query';
		}
	}

	Manager.addWidget(new AjaxFranceLabs.StatsInformationWidget({
		elm : $('#result_information'),
		id : 'searchInformation',
		mode : mode
	}));

	Manager.addWidget(new AjaxFranceLabs.ResultStatsQueryWidget({
		elm : $('#results'),
		id : 'documents',
		pagination : false,
		mode : mode
	}));

	$.when(Manager.init()).then(function() {
		Manager.store.addByValue("q", q);
		if (mode === 'queryID'){

			Manager.store.addByValue("fl", "history");
		}
		
		Manager.makeRequest();
	});
});



function getParamValue(param,url)
{
	var u = url == undefined ? document.location.href : url;
	var reg = new RegExp('(\\?|&|^)'+param+'=(.*?)(&|$)');
	matches = u.match(reg);
	if (matches === null)
		return '';
	return matches[2] != undefined ? decodeURIComponent(matches[2]).replace(/\+/g,' ') : '';
}
