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
AjaxFranceLabs.Parameter = AjaxFranceLabs.Class.extend({

	//Variables

	name : null,

	value : null,

	locals : {},

	//Methods

	val : function(value) {
		if (value === undefined) {
			return this.value;
		} else {
			this.value = value;
		}
	},

	local : function(name, value) {
		if (value === undefined) {
			return this.locals[name];
		} else {
			this.locals[name] = value;
		}
	},

	remove : function(name) {
		delete this.locals[name];
	},

	string : function() {
		var pairs = [];

		for (var name in this.locals) {
			if (this.locals[name]) {
				pairs.push(name + '%3D' + encodeURIComponent(this.locals[name]));
			}
		}

		var prefix = pairs.length ? '{!' + pairs.join('%20') + '}' : '';

		if (this.value) {
			return this.name + '=' + prefix + this.valueString(this.value);
		} else if (this.name == 'q' && prefix) {
			return 'q.alt=' + prefix + encodeURIComponent('*:*');
		} else {
			return '';
		}
	},

	parseString : function(str) {
		var param = str.match(/^([^=]+)=(?:\{!([^\}]*)\})?(.*)$/);
		if (param) {
			var matches;
			while ( matches = /([^\s=]+)=(\S*)/g.exec(decodeURIComponent(param[2]))) {
				this.locals[matches[1]] = decodeURIComponent(matches[2]);
				param[2] = param[2].replace(matches[0], '');
			}
			if (param[1] == 'q.alt') {
				this.name = 'q';
			} else {
				this.name = param[1];
				this.value = this.parseValueString(param[3]);
			}
		}
	},

	valueString : function(value) {
		value = $.isArray(value) ? value.join(',') : value;
		return encodeURIComponent(value);
	},

	parseValueString : function(str) {
		str = decodeURIComponent(str);
		return str.indexOf(',') == -1 ? str : str.split(',');
	}
});

AjaxFranceLabs.Parameter.escapeValue = function(value) {
	value = value.toString();
	if (value.match(/[ :]/) && !value.match(/[\[\{]\S+ TO \S+[\]\}]/) && !value.match(/^["\(].*["\)]$/)) {
		return '"' + value + '"';
	}
	return value;
}