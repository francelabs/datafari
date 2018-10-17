$(function($) {

	window.i18n.setLanguageUrl('../js/AjaxFranceLabs/locale/');
	window.i18n.setLanguage('en');

	Manager = new AjaxFranceLabs.Manager({
		serverUrl : 'http://' + window.location.hostname
				+ ':8080/Datafari/SearchProxy/',
		servlet : 'stats',
		constellio : false
	});

});

$(function($) {

	Manager.addWidget(new AjaxFranceLabs.StatsQueryWidget({
		elm : $('#searchBar'),
		id : 'searchBar',
		autocomplete : true
	}));

	Manager.addWidget(new AjaxFranceLabs.StatsInformationWidget({
		elm : $('#result_information'),
		id : 'searchInformation'
	}));

	Manager.addWidget(new AjaxFranceLabs.ResultStatsWidget({
		elm : $('#results'),
		id : 'documents',
		pagination : false
	}));

	$.when(Manager.init()).then(function() {

		Manager.makeRequest();
	});
});
