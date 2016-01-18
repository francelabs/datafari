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
AjaxFranceLabs.OntologySuggestionWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'suggestion',
	parentLabelsField : '',
	childLabelsField : '',
	useLanguage : false,

	//Methods
	

	// Constructor
	buildWidget : function() {
		$(this.elm).addClass('suggestionWidget').addClass('widget');
		$(this.elm).hide();
		if(this.useLanguage) { // Check if we need to use the language
			var userLang = navigator.language || navigator.userLanguage;
			if(userLang.toLowerCase().startsWith('en')) { // Unify the english language
				userLang = "en";
			}
			this.parentLabelsField += "_" + userLang; // Set the parent label with the proper language
			this.childLabelsField += "_" + userLang; // Set the child label with the proper language
		}
	},

	// Nothing to do 
	beforeRequest : function() {
		$(this.elm).empty();
	},

	
	// Get the results, if results have expected facet fields, take the first parent ontology label and the first child ontology label as suggestions
	afterRequest : function() {
		var self = this;
		var data = this.manager.response;
		var elm = $(this.elm);
		if(data.facet_counts !== undefined && data.facet_counts.facet_fields !== undefined && (data.facet_counts.facet_fields[self.parentLabelsField] !== undefined || data.facet_counts.facet_fields[self.childLabelsField] !== undefined)) {
			$(self.elm).show();
			var parentSuggestion = "";
			var childSuggestion = "";
			if(data.facet_counts.facet_fields[self.parentLabelsField] !== undefined && data.facet_counts.facet_fields[self.parentLabelsField].length > 0) { // If the facet fields contain the ontology parents labels, then take the first facet as the parent suggestion because it's the often one
				parentSuggestion = data.facet_counts.facet_fields[self.parentLabelsField][0];
			}
			if(data.facet_counts.facet_fields[self.childLabelsField] !== undefined  && data.facet_counts.facet_fields[self.childLabelsField].length > 0) { // If the facet fields contain the ontology children labels, then take the first facet as the child suggestion because it's the often one
				childSuggestion = data.facet_counts.facet_fields[self.childLabelsField][0];
			}
			if(parentSuggestion == "" && childSuggestion == "") { // If no suggestions to purpose then hide the facet
				$(self.elm).hide();
			} else { // Else show the suggestion(s)
					
					var res = "";
					var res2 = "";
					// Determine the first suggestion, depending of the availability of the suggestions. Either both are available or just one of them
					if(parentSuggestion !== "") {
						res = parentSuggestion;
					} else {
						res = childSuggestion;
					}
					if(res !== childSuggestion && childSuggestion !== "") {
						res2 = childSuggestion;
					}
					// Show the first suggestion (may be the parent or the child one, depending on their availability)
					$(self.elm).append('<span>').find('span').append('Related subjects: ' + '<span class="result">' + res + '</span>').find('.result').click(function() {
						self.manager.store.get('q').val(res);
						for (var w in self.manager.widgets) {
							if (self.manager.widgets[w].type === 'searchBar') {
								$(self.manager.widgets[w].elm).find('input[type=text]').val(res);
							}
						}
						self.manager.generateAndSetQueryID();
						self.manager.makeRequest();
					});
					if(res2 !== "") { // If there is a second suggestion then show it
						$(self.elm).find('span').first().append(' , ' + '<span class="result">' + res2 + '</span>').find('.result:nth-child(2)').click(function() {
							self.manager.store.get('q').val(res2);
							for (var w in self.manager.widgets) {
								if (self.manager.widgets[w].type === 'searchBar') {
									$(self.manager.widgets[w].elm).find('input[type=text]').val(res2);
								}
							}
							self.manager.generateAndSetQueryID();
							self.manager.makeRequest();
						});
					}
				
			}
		} else { // None of the facet fields concern the ontology parents or children ==> hide the widget
			$(self.elm).hide();
		}
		
	}
	
	
});
