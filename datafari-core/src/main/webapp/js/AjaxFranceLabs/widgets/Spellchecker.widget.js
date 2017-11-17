/*******************************************************************************
 * Copyright 2015 - 2016 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

AjaxFranceLabs.SpellcheckerWidget = AjaxFranceLabs.AbstractWidget
		.extend({

			// Variables

			type : 'spellchecker',

			// Methods

			buildWidget : function() {
				$(this.elm).addClass('spellcheckerWidget').addClass('widget');
				$(this.elm).hide();
			},

			beforeRequest : function() {

				var self = this;

				// Last query is not the one corrected by spellchecker
				// => clear the spellchecker widget
				if (self.manager.store.isParamDefined('original_query') !== true) {
					$(this.elm).empty();
				}
				// ELSE: We just performed a new query with a corrected value by
				// spellchecker => do nothing

			},

			afterRequest : function() {

				var self = this, res = '';
				var data = this.manager.response, elm = $(this.elm);

				/*
				 * We entered an incorrect value in search box, this is why
				 * spellcheck.suggestions is not empty
				 * => show the spellchecker widget and perform a new query with
				 * the corrected value
				 */
				if (data.response.numFound == 0 && data.spellcheck !== undefined
						&& data.spellcheck.collations.length > 0) {

					$(self.elm).empty();
					$(self.elm).show();

					/*
					* Store the "original" searched value
					* We use this function instead of self.manager.store.add('original_query', ...);
					* to have an object of type subclass inside original_query instead of string.
					* Otherwise we get exception in params.push(this.params[name].string()); 
					* of ParametersStore.js
					*/
					self.manager.store.get('original_query').val(self.manager.store.get('q').val());					

					// Show localized message for corrected and misspelled searched values
					$(self.elm)
							.append(
									'<span class="spellcheckerResult">'
											+ window.i18n.msgStore['spellcheckerResults']
											+ '<span class="result">'
											+ data.spellcheck.collations[1]
											+ '</span></span><br/><br/><span>'
											+ window.i18n.msgStore['notHitsQueries']
											+ '<span class="result">'
											+ self.manager.store.get('original_query').val()
											+ '</span></span>');

					/* 
					 * Take the collation value: first value of array contains
					 *"collation", this is why we get [1] instead of [0]
					 * Perform a new query with the corrected value
					 */					
					self.doSpellcheckerQuery(data.spellcheck.collations[1]);

				} else if(data.spellcheck !== undefined
						&& data.spellcheck.collations.length > 0) {
					// Create suggestButton
					var suggestButton = $("<span class='result'>" + data.spellcheck.collations[1] + "</span>");
					suggestButton.click(function() {
						$('.searchBar input[type=text]').val(data.spellcheck.collations[1]);
						self.manager.store.get('q').val(data.spellcheck.collations[1]);
						self.manager.store.remove('original_query');
						self.manager.makeRequest();
					});
					
					// Create spellcheck span
					var spellcheckSpan = $("<span class='spellcheckerResult'></span>");
					spellcheckSpan.append(window.i18n.msgStore['spellcheckerProposal']);
					spellcheckSpan.append(suggestButton);
					
					// Add spellcheck span to elm
					$(self.elm).append("<br/><br/>");
					$(self.elm).append(spellcheckSpan);
					
					// Display widget
					$(self.elm).show();
				} else {

					// Last query is not the one corrected by spellchecker
					// => hide the spellchecker widget
					if (self.manager.store.isParamDefined('original_query') !== true) {

						$(self.elm).hide();

					} else {
						/* 
						* We just performed a new query with a corrected value
						* by spellchecker => reset the manager.store to be ready
						* for the next query from the user
						*/
						self.manager.store.remove('original_query');
					}
				}
			},

			doSpellcheckerQuery : function(correctedValue) {

				var self = this;

				// Update the Q parameter of the manager store, to be used for
				// the new search query
				self.manager.store.get('q').val(correctedValue);

				/*
				 * Do not update the searchBar widget linked to the current manager with the new
				 * searched value: we want to display the misspelled search term
				 * for ( var w in self.manager.widgets) {
				 *	if (self.manager.widgets[w].type === 'searchBar') {
				 *		$(self.manager.widgets[w].elm).find('input[type=text]').val(correctedValue);
				 *	}
				 * }
				 */ 

				// Submit a new query with the spellchecked (corrected) value,
				// the one set in the Q parameter of the manager store
				self.manager.generateAndSetQueryID();
				self.manager.makeRequest();
			}

		});
