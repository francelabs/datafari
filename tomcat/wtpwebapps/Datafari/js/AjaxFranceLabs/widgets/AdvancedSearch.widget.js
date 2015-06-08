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
		elm.append('<button>Make search</button>').find('button').click(function() {
			self.manager.makeRequest();
		});
		AjaxFranceLabs.addMultiElementClasses($(this.elm).find('.advTable'));
		elm.find('.advTable input').keypress(function(event) {
			if (event.keyCode === 13) {
				self.manager.makeRequest();
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
	},

	addTable : function(table) {
		this.tables.push(table);
	},

	beforeRequest : function() {
		this.manager.store.remove('fq');
		this.manager.store.remove('q');
		var q = '';
		for (var table in this.tables) {
			for (var field in this.tables[table].fieldStore) {
				var value = this.tables[table].fieldStore[field].getValue();
				if (!AjaxFranceLabs.empty(value)) {
					if (this.tables[table].fieldStore[field].filter === true) {
						this.manager.store.addByValue('fq', value);
					} else {
						q += '(' + value + ') AND ';
					}
				}
			}
			if (q.lastIndexOf('AND ') === q.length - 4)
				q = q.substring(0, q.length - 5);
			if (AjaxFranceLabs.empty(q) !== true)
				this.manager.store.addByValue('q', q);
			else
				this.manager.store.addByValue('q', '*:*');
		}
	}
});
