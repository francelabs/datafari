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
AjaxFranceLabs.AdvancedSearchWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	tables : [],

	resizable : false,

	options : {},

	optionsExtend : true,

	type : 'advancedSearch',

	//Methods

	buildWidget : function() {
		var self = this, elm = $(this.elm);
		elm.addClass('advancedSearchWidget').addClass('widget').attr('widgetId', this.id).append('<div class="wrapper">');
		for (var table in this.tables) {
			this.tables[table].parent = $(this.elm).find('.wrapper');
			this.tables[table].manager = this.manager;
			this.tables[table].init();
			elm.find('.wrapper').append('<span class="separator">');
		}
		elm.append('<button>' + window.i18n.msgStore['advancedSearch-makesearch-btn'] + '</button>').find('button').click(function() {
			self.makeRequest();
		});
		AjaxFranceLabs.addMultiElementClasses($(this.elm).find('.advTable'));
		elm.find('.advTable input').keypress(function(event) {
			if (event.keyCode === 13) {
				self.makeRequest();
			}
		});
		if (this.resizable === true) {
			var options = {
				handles : 's',
				minHeight : 135,
				maxHeight : $(self.elm).find('.wrapper').innerHeight()
			};
			if (this.optionsExtend) {
				$.extend(this.options, this.options, options);
			}
			elm.find('.wrapper').css('overflow', 'hidden').resizable(this.options).find('.wrapper').resizable({
				start : function(event, ui) {
					elm.find('.wrapper').css('height', '100%').resizable("option", "maxHeight", elm.find('.wrapper').height());
				},
				stop : function(event, ui) {
					if (elm.find('.wrapper').resizable("option", "maxHeight") === elm.find('.wrapper').height())
						elm.find('.wrapper').css('height', '100%');
				}
			});
		}
		
		// Basic Search link
		elm.append('<div id="basicSearchLink" class="searchModeLink"><a href="">'+ window.i18n.msgStore['basicSearchLink'] +'</a></div>');
		
		
		$('#basicSearchLink').click(function(event){
			// Hide the advanced search
			elm.hide();
			
			// Reset the widget status (radios, entered text, ...)
			self.manager.getWidgetByID('searchBar').reset();
			
			// Display the basic search
			$('#searchBar').show();
			
			// Perform a "select all" request: *:*
			self.manager.makeDefaultRequest();
			
			// Prevent page reload
			event.preventDefault();
		});
		
		// Hidden by default: basic search is displayed
		elm.hide();		
	},

	addTable : function(table) {
		this.tables.push(table);
	},

	beforeRequest : function() {
		
		// If the widget is displayed and we are not performing the spellchecker query
		if ($(this.elm).is(':visible') && !this.manager.store.isParamDefined('original_query')){
			
			this.manager.store.remove('fq');
			this.manager.store.remove('q');
			
			var qArr = [];
			
			for (var table in this.tables) {
				for (var field in this.tables[table].fieldStore) {
					var value = this.tables[table].fieldStore[field].getValue();
					if (!AjaxFranceLabs.empty(value)) {
						if (this.tables[table].fieldStore[field].filter === true) {
							this.manager.store.addByValue('fq', value);
						} else {
							qArr.push('(' + value + ')');
						}
					}
				}			
					
				if (qArr.length > 0){
					// Create the query, combining the different search fields with boolean values from radio buttons (natural order from left to right)
					this.manager.store.addByValue('q', qArr.join($(this.elm).find('.advSearchBooleanOperator .radio:checked').val()));
				}
				else {
					this.manager.store.addByValue('q', '*:*');
				}
			}
			
			this.updateAddressBar();
		}
	},
	
	/**
	 * Resets the inputs of the widget: underlying elements table and fields
	 */
	reset : function() {
		for (var table in this.tables){
			this.tables[table].reset();
		}
	},
	
	makeRequest : function() {
		this.cleanResults();
		this.manager.makeRequest();
	},
	
	/**
	 * Cleans results area (same as for basic search)
	 */
	cleanResults : function() {
		
		this.manager.store.remove('start');

		$.each(this.manager.widgets, function(index, widget) {
			if (typeof widget.pagination !== "undefined") {
				widget.pagination.pageSelected = 0;
			}
		});
	},
	
	/**
	 * Update the address bar and generates the id for the query
	 */
	updateAddressBar : function() {
		
		window.history.pushState('Object', 'Title',
				window.location.pathname + '?query=' + this.manager.store.get('q').val() + '&lang='
						+ window.i18n.language);

		this.manager.store.addByValue("id", UUID.generate());		
	}
});
