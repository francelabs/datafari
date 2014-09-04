/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.SearchInformationWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#searchInformation_widget
 *
 */
AjaxFranceLabs.SearchInformationWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'searchInformation',

	//Methods

	buildWidget : function() {
		$(this.elm).addClass('searchInformationWidget').addClass('widget').attr('widgetId', this.id).append('<span class="information" id="result_information_collectionName">').append('<span class="information" id="result_information_numberOfResults">').append('<span class="information" id="result_information_search">').append('<span class="information" id="result_information_requestTime">');
	},

	beforeRequest : function() {
		$(this.elm).find('span.information').empty();
	},

	afterRequest : function() {
		var data = this.manager.response, elm = $(this.elm);
		if (this.manager.constellio)
			elm.find('#result_information_collectionName').append('<span>' + this.manager.collection + '</span>');
		var start = (parseInt(data.response.start) + 1);
		var end = (parseInt(data.response.start) + this.manager.store.get('rows').val());
		var numFound = data.response.numFound;
		if (end > numFound)
			end = numFound;
		if (numFound === 0)
			start = 0;
		
		elm.find('#result_information_numberOfResults').append('<span>' + window.i18n.msgStore['result'] + ' ' + start + ' - ' + end + ' '+window.i18n.msgStore['of']+' ' + numFound + '</span>');
		elm.find('#result_information_search').append('<span> '+window.i18n.msgStore['for']+' ' + this.manager.store.get('q').val() + '</span>');
		elm.find('#result_information_requestTime').append('<span> (' + (data.responseHeader.QTime / 1000) + ' '+window.i18n.msgStore['seconds']+')</span>');
	}
});
