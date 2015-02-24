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
AjaxFranceLabs.ParameterStore = AjaxFranceLabs.Class.extend({

	//Variables

	params : {},

	//Methods

	isMultiple : function(name) {
		return name.match(/^(?:bf|bq|facet\.date|facet\.date\.other|facet\.date\.include|facet\.field|facet\.pivot|facet\.range|facet\.range\.other|facet\.range\.include|facet\.query|fq|group\.field|group\.func|group\.query|pf|qf)$/);
	},

	get : function(name) {
		if (this.params[name] === undefined) {
			var param = new AjaxFranceLabs.Parameter({
				name : name
			});
			if (this.isMultiple(name)) {
				this.params[name] = [param];
			} else {
				this.params[name] = param;
			}
		}
		return this.params[name];
	},

	values : function(name) {
		if (this.params[name] === undefined) {
			return [];
		}
		if (this.isMultiple(name)) {
			var values = [];
			for (var p = 0, l = this.params[name].length; p < l; p++) {
				values.push(this.params[name][p].val());
			}
			return values;
		} else {
			return [this.params[name].val()];
		}
	},

	add : function(name, param) {
		if (param === undefined) {
			param = new AjaxFranceLabs.Parameter({
				name : name
			});
		}
		if (this.isMultiple(name)) {
			if (this.params[name] === undefined) {
				this.params[name] = [param];
			} else {
				if ($.inArray(param.val(), this.values(name)) == -1) {
					this.params[name].push(param);
				} else {
					return false;
				}
			}
		} else {
			this.params[name] = param;
		}
		return param;
	},

	remove : function(name, index) {
		if (index === undefined) {
			delete this.params[name];
		} else {
			this.params[name].splice(index, 1);
			if (this.params[name].length == 0) {
				delete this.params[name];
			}
		}
	},

	addByValue : function(name, value, locals) {
		if (locals === undefined) {
			locals = {};
		}
		if (this.isMultiple(name) && $.isArray(value)) {
			var ret = [];
			for (var i = 0, l = value.length; i < l; i++) {
				ret.push(this.add(name, new AjaxFranceLabs.Parameter({
					name : name,
					value : value[i],
					locals : locals
				})));
			}
			return ret;
		} else {
			return this.add(name, new AjaxFranceLabs.Parameter({
				name : name,
				value : value,
				locals : locals
			}));
		}
	},

	removeByValue : function(name, value) {
		var indices = this.find(name, value);
		if (indices) {
			if ($.isArray(indices)) {
				for (var i = indices.length - 1; i >= 0; i--) {
					this.remove(name, indices[i]);
				}
			} else {
				this.remove(indices);
			}
		}
		return indices;
	},

	string : function() {
		var params = [];
		for (var name in this.params) {
			if (this.isMultiple(name)) {
				for (var p = 0, l = this.params[name].length; p < l; p++) {
					params.push(this.params[name][p].string());
				}
			} else {
				params.push(this.params[name].string());
			}
		}
		return $.grep(params, function(item) {
			return item.toString();
		}).join('&');
	},

	parseString : function(str) {
		var pairs = str.split('&');
		for (var p = 0, l = pairs.length; p < l; p++) {
			if (pairs[p]) {
				var param = new AjaxFranceLabs.Parameter();
				param.parseString(pairs[p]);
				this.add(param.name, param);
			}
		}
	},

	find : function(name, value) {
		if (this.params[name] !== undefined) {
			if (this.isMultiple(name)) {
				var indices = [];
				for (var i = 0, l = this.params[name].length; i < l; i++) {
					if (AjaxFranceLabs.equals(this.params[name][i].val(), value)) {
						indices.push(i);
					}
				}
				return indices.length ? indices : false;
			} else {
				if (AjaxFranceLabs.equals(this.params[name].val(), value)) {
					return name;
				}
			}
		}
		return false;
	}
});
