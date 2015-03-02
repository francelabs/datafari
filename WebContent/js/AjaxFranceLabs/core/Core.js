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

AjaxFranceLabs = function() {
};

AjaxFranceLabs.Class = function() {
};

AjaxFranceLabs.Class.extend = function(properties) {
	var klass = this,
		subclass = function(options) {
			AjaxFranceLabs.extend(this, new klass(options), properties, options);
		}
	subclass.extend = this.extend;
	return subclass;
}

AjaxFranceLabs.tinyUrl = function(url, length) {
	var maxSize = (length) ? length : 75;
	if (url.length <= maxSize)
		return url;
	return url.substr(0, (url.lastIndexOf('/') - url.length + maxSize - 3)) + '...' + url.substr(url.lastIndexOf('/') + 1);
}

AjaxFranceLabs.tinyString = function(string, length) {
	var maxSize = (length) ? length : 25;
	if (string.length <= maxSize)
		return string;
	return string.substr(0, maxSize - 1) + '...';
}

AjaxFranceLabs.truncate = function(string, length) {
	var maxSize = (length) ? length : 150
		strArr = string.split(' '),
		str = '';
	while (str.length < length && str != string) {
		if (str !== '')
			str += ' ';
		str += strArr.shift();
	}
	if (str !== string)
		str += '...';
	return str;
}

AjaxFranceLabs.addMultiElementClasses = function(source) {
	source.each(function(i) {
		if (i == 0)
			$(this).addClass('first');
		else if (i !== source.length - 1)
			$(this).addClass('middle');
		if (i == source.length - 1)
			$(this).addClass('last');
		if (i % 2 == 0)
			$(this).addClass('odd');
		else
			$(this).addClass('even');
		$(this).addClass('e-' + i);
	});
}

AjaxFranceLabs.clearMultiElementClasses = function(source) {
	source.each(function() {
		if ($(this).attr('class') !== undefined)
			$(this).attr('class', AjaxFranceLabs.trim($(this).attr('class').replace(/e-.*|first|middle|last|odd|even/g, ' ')));
	});
}

AjaxFranceLabs.isString = function(obj) {
	return obj != null && typeof obj == 'string';
};

AjaxFranceLabs.isRegExp = function(obj) {
	return obj != null && ( typeof obj == 'object' || typeof obj == 'function') && 'ignoreCase' in obj;
};

AjaxFranceLabs.equals = function(foo, bar) {
	if ($.isArray(foo) && $.isArray(bar)) {
		if (foo.length !== bar.length) {
			return false;
		}
		for (var i = 0, l = foo.length; i < l; i++) {
			if (foo[i] !== bar[i]) {
				return false;
			}
		}
		return true;
	} else if (AjaxFranceLabs.isRegExp(foo) && AjaxFranceLabs.isString(bar)) {
		return bar.match(foo);
	} else if (AjaxFranceLabs.isRegExp(bar) && AjaxFranceLabs.isString(foo)) {
		return foo.match(bar);
	} else {
		return foo === bar;
	}
};

AjaxFranceLabs.empty = function(mixed_var) {
	var key;
	if (mixed_var === "" || mixed_var === 0 || mixed_var === "0" || mixed_var === null || mixed_var === false || typeof mixed_var === 'undefined') {
		return true;
	}
	if ( typeof mixed_var == 'object') {
		for (key in mixed_var) {
			return false;
		}
		return true;
	}
	return false;
};

AjaxFranceLabs.trim = function(str, charlist) {
	var whitespace, l = 0, i = 0;
	str += '';
	if (!charlist) {
		// default list
		whitespace = " \n\r\t\f\x0b\xa0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u2028\u2029\u3000";
	} else {
		// preg_quote custom list
		charlist += '';
		whitespace = charlist.replace(/([\[\]\(\)\.\?\/\*\{\}\+\$\^\:])/g, '$1');
	}
	l = str.length;
	for ( i = 0; i < l; i++) {
		if (whitespace.indexOf(str.charAt(i)) === -1) {
			str = str.substring(i);
			break;
		}
	}
	l = str.length;
	for ( i = l - 1; i >= 0; i--) {
		if (whitespace.indexOf(str.charAt(i)) === -1) {
			str = str.substring(0, i + 1);
			break;
		}
	}
	return whitespace.indexOf(str.charAt(0)) === -1 ? str : '';
};

AjaxFranceLabs.extractLast = function(term) {
	return AjaxFranceLabs.split(term).pop();
}

AjaxFranceLabs.split = function(val) {
	return val.split(/\u200c\s*/);
}

AjaxFranceLabs.extend = function() {
	var target = arguments[0] || {}, i = 1, length = arguments.length, options;
	for (; i < length; i++) {
		if (( options = arguments[i]) != null) {
			for (var name in options) {
				var src = target[name], copy = options[name];
				if (target === copy) {
					continue;
				}
				if (copy && typeof copy == 'object' && !copy.nodeType) {
					target[name] = AjaxFranceLabs.extend(src || (copy.length != null ? [] : {}), copy);
				} else if (copy && src && typeof copy == 'function' && typeof src == 'function') {
					target[name] = (function(superfn, fn) {
						return function() {
							var tmp = this._super, ret;
							this._super = superfn;
							ret = fn.apply(this, arguments);
							this._super = tmp;
							return ret;
						};
					})(src, copy);
				} else if (copy !== undefined) {
					target[name] = copy;
				}
			}
		}
	}
	return target;
};
