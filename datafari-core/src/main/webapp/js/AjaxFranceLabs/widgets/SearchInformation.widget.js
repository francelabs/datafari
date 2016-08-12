/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
AjaxFranceLabs.SearchInformationWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'searchInformation',

	//Methods

	buildWidget : function() {
		$(this.elm).addClass('searchInformationWidget bc-color').addClass('widget').attr('widgetId', this.id).append('<span class="information" id="result_information_collectionName"></span>').append('<span class="information" id="result_information_numberOfResults"></span>').append('<span class="information" id="result_information_search"></span>').append('<span class="information" id="result_information_requestTime"></span>');
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
		$("#number_results_mobile span").empty().append(numFound);
		if (end > numFound)
			end = numFound;
		if (numFound === 0)
			start = 0;
		
		elm.find('#result_information_numberOfResults').append('<span>' + window.i18n.msgStore['Results'] + ' ' + start + ' - ' + end + ' '+ window.i18n.msgStore['of'] +' ' + numFound + '</span>');
		elm.find('#result_information_search').append('<span> '+ window.i18n.msgStore['for'] +' ' + this.manager.store.get('q').val() + '</span>');
		elm.find('#result_information_requestTime').append('<span> (' + (data.responseHeader.QTime / 1000) + ' '+window.i18n.msgStore['Seconds']+')</span>');
	}
});
