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
AjaxFranceLabs.AbstractFacetWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	field : null,
	
	returnUnselectedFacetValues: false,
	
	selectionType : 'AND',

	//Methods

	init : function() {
		if (!this.initialized) {
			this.initialized = true;
			this.initStore();
			this.buildWidget();
		}
	},

	initStore : function() {
		var parameters = ['facet.prefix', 'facet.sort', 'facet.limit', 'facet.offset', 'facet.mincount', 'facet.missing', 'facet.method', 'facet.enum.cache.minDf'], p;
		this.manager.store.addByValue('facet', true);
		if (this['facet.field'] !== undefined && this['facet.field'] !== false) {
			p = this.manager.store.addByValue('facet.field', this.field);
		} else if (this['facet.date'] !== undefined && this['facet.date'] !== false) {
			p = this.manager.store.addByValue('facet.date', this.field);
			parameters = parameters.concat(['facet.date.start', 'facet.date.end', 'facet.date.gap', 'facet.date.hardend', 'facet.date.other', 'facet.date.include']);
		} else if (this['facet.range'] !== undefined && this['facet.range'] !== false) {
			p = this.manager.store.addByValue('facet.range', this.field);
			parameters = parameters.concat(['facet.range.start', 'facet.range.end', 'facet.range.gap', 'facet.range.hardend', 'facet.range.other', 'facet.range.include']);
		}
		if(this.returnUnselectedFacetValues)
			p.locals = { ex: this.field }
		for (var i = 0, l = parameters.length; i < l; i++) {
			if (this[parameters[i]] !== undefined) {
				this.manager.store.addByValue('f.' + this.field + '.' + parameters[i], this[parameters[i]]);
			}
		}
	},

	add : function(value) {
		var fq;
		if(this.selectionType === 'AND')
			fq = this.manager.store.addByValue('fq', this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value));
		if(!this.manager.store.find('fq', new RegExp(this.field + ':')))
			fq = this.manager.store.addByValue('fq', '(' + this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value) + ' )');
		else {
			fq = this.manager.store.get('fq', new RegExp(this.field + ':'))[this.manager.store.find('fq', new RegExp(this.field + ':'))];
			fq.val('(' + /\((.+)\)/.exec(fq.val())[1] + 'OR ' + this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value) + ' )');
		}
		if(this.returnUnselectedFacetValues)
			fq.locals = { tag: this.field }
		return fq;
	},

	set : function(value) {
		this.manager.store.removeByValue('fq', new RegExp('^-?' + this.field + ':'));
		var fq = this.manager.store.addByValue('fq', this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value));
		if(this.returnUnselectedFacetValues)
			fq.locals = { tag: this.field }
		return fq;
	},

	remove : function(value) {
		if(this.selectionType !== 'OR')
			return this.manager.store.removeByValue('fq', new RegExp('^-?' + this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value)));
		if(this.manager.store.find('fq', new RegExp(this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value.replace(/\\/g,"\\\\"))))){
			var fq = this.manager.store.get('fq', new RegExp(this.field + ':'))[this.manager.store.find('fq', new RegExp(this.field + ':'))];
			fq.val(fq.val().replace(new RegExp(this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(value.replace(/\\/g,"\\\\")) + ' '), ''));
			fq.val(fq.val().replace(new RegExp(' OR OR '),' OR '));
			if(fq.val().substr(fq.val().length-5, 4) == ' OR ')
				fq.val(fq.val().substring(0, fq.val().length-4) + ')');
			if(fq.val().substr(1, 3) == 'OR ')
				fq.val('(' + fq.val().substring(4, fq.val().length));
			if(fq.val() == '()') {
				fq.val('');
				this.manager.store.remove('fq');
			}
			return fq;
		}
	},

	clear : function() {
		return this.manager.store.removeByValue('fq', new RegExp('^-?' + this.field + ':'));
	},

	selectHandler : function(value) {
		var self = this;
		if (self[(self.selectionType === 'ONE' ? 'set' : 'add')](value)) {
			self.manager.store.remove('start');
			self.manager.makeRequest();
		}
	},

	unselectHandler : function(value) {
		var self = this;
		if (self.remove(value)) {
			self.manager.store.remove('start');
			self.manager.makeRequest();
		}
	},
	
	
	
	beforeRequest : function() {
		var test = $(this.elm).find("input");
		$.each(test, function(index, value) {
			value.disabled = true ;
		});
	},

	afterRequest : function() {
	}
});