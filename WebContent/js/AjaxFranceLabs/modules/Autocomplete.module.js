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
/**
 * 
 * @class AjaxFranceLabs.AutocompleteModule
 * @extends AjaxFranceLabs.AbstractModule
 * 
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#autocomplete_module
 * 
 */
AjaxFranceLabs.AutocompleteModule = AjaxFranceLabs.AbstractModule
.extend({

	// Variables

	optionsExtend : true,

	options : {},

	render : null,

	field : null,

	openOnFieldFocus : false,

	valueSelectFormat : function(value) {
		return value
	},

	singleValue : false,

	type : 'autocomplete',

	cache : [],

	lastXhr : null,

	// Methods

	init : function() {
		if (!this.initialized) {
			this.initialized = true;
			var self = this;
			var opt = {
					delay : 50,
					minLength : 0,
					source : function(request, response) {
						var term = AjaxFranceLabs.extractLast(request.term).toLowerCase();
						if (term in self.cache) {
							response(self.cache[term]);
							return;
						}
						var f = (self.field === null) ? '' : '&f='
							+ self.field;
						if (AjaxFranceLabs.extractLast(request.term) != '') {
							self.lastXhr = self.manager
							.executeRequest(
									self.connectionInfo.serverUrl,
									self.connectionInfo.servlet,
									self.connectionInfo.queryString
									+ encodeURIComponent('"' + AjaxFranceLabs
											.extractLast(request.term)
											+ '"')
											+ f + "&autocomplete=true",
											function(data, status, xhr) {
										var src = [];
										// self.manager.executeRequest('',
										// '', 'q=' +
										// AjaxFranceLabs.extractLast(request.term)
										// + f,
										// function(data) {
										if (data.spellcheck.suggestions.length > 1) {

											$.each(data.spellcheck.collations, function(index, value) {
												if (value != 'collation'){
													src.push(data.spellcheck.collations[index].collationQuery.replace(/"/g, ''));
												}

											});

											self.cache[term] = src;
											if (xhr === self.lastXhr) {
												response(src);
											}
										}

									});
							// });
						}
					},
					search : function() {
						var term = AjaxFranceLabs.extractLast(this.value);
						if (term.length < self.autocompleMinLength) {
							return false;
						}
					},
					select : function(event, ui) {
						if (self.singleValue === true) {
							this.value = self
							.valueSelectFormat(ui.item.value);
							this.value +=  ("\u200c");
						} else {
							var terms = AjaxFranceLabs.split(this.value);
							terms.pop();
							terms.push(self
									.valueSelectFormat(ui.item.value));
							terms.push("");
							this.value = terms.join("\u200c ");
							this.value = this.value.slice(0,-2);
						}
						return false;
					},
					focus : function() {
						return false;
					}
			};
			if (this.optionsExtend === true) {
				var newOpt = {};
				$.extend(true, newOpt, opt, this.options);
				this.options = newOpt;
			}
			$(this.elm).autocomplete(this.options);
			if (this.openOnFieldFocus === true) {
				$(this.elm).click(function() {
					$(this).autocomplete('search');
				});
			}
			if (typeof this.render === 'function') {
				$(this.elm).data("autocomplete")._renderItem = this.render;
			}
			if (this.manager.connectionInfo.autocomplete === undefined
					&& this.options.source == null)
				throw 'AutocompleteModule: connectionInfo not defined in Manager';
			else
				this.connectionInfo = this.manager.connectionInfo.autocomplete;
		}
	}
});