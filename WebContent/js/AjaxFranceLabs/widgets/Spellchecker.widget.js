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
AjaxFranceLabs.SpellcheckerWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'spellchecker',

	//Methods
	

	buildWidget : function() {
		$(this.elm).addClass('spellcheckerWidget').addClass('widget');
		$(this.elm).hide();
	},

	beforeRequest : function() {
		$(this.elm).empty();
	},

	afterRequest : function() {
		var self = this, res = '';
		var data = this.manager.response, elm = $(this.elm);
			if (data.spellcheck !== undefined && data.spellcheck.suggestions.length > 0) {
				$(self.elm).show();
				res = data.spellcheck.collations[1];
				$(self.elm).append('<span>').find('span').append('Essayer avec ' + '<span class="result">' + res + '</span> ?').find('.result').click(function() {
					self.manager.store.get('q').val(res);
					for (var w in self.manager.widgets) {
						if (self.manager.widgets[w].type === 'searchBar') {
							$(self.manager.widgets[w].elm).find('input[type=text]').val(res);
						}
					}
					self.manager.generateAndSetQueryID();
					self.manager.makeRequest();
				});
			}else{
				$(self.elm).hide();
			}
		
	},
	
	
});
