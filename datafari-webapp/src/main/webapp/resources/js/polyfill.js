//JQuery plugin for buttons so they behave in bootstrap 4 like in bootstrap 3
(function($) {
  $.fn.loading = function(action) {
    if (action === 'loading' && this.data('loading-text')) {
      this.data('original-text', this.html()).html(this.data('loading-text')).prop('disabled', true);
    }
    if (action === 'reset' && this.data('original-text')) {
      this.html(this.data('original-text')).prop('disabled', false);
    }
  };
}(jQuery));

// Add by default csrf token (if available) to any jquery post request
var csrf_param = $('meta[name="csrf-param"]').attr('content');
var csrf_token = $('meta[name="csrf-token"]').attr('content');
if (csrf_token != undefined) {
  $.ajaxSetup({
    headers : {
      'X-CSRF-TOKEN' : csrf_token
    }
  });
}

// isArray
if (!Array.isArray) {
  Array.isArray = function(arg) {
    return Object.prototype.toString.call(arg) === '[object Array]';
  };
}

// truncate
String.prototype.truncate = function(length, length_show) {
  var string = this.valueOf();

  if (string.length > length) {
    var between_txt = string.substring(length_show, string.length - length_show);

    return string.replace(between_txt, "...");
  } else {
    return string;
  }
}

/* ! https://mths.be/endswith v0.2.0 by @mathias */
// endsWith
if (!String.prototype.endsWith) {
  (function() {
    'use strict'; // needed to support `apply`/`call` with `undefined`/`null`
    var defineProperty = (function() {
      // IE 8 only supports `Object.defineProperty` on DOM elements
      try {
        var object = {};
        var $defineProperty = Object.defineProperty;
        var result = $defineProperty(object, object, object) && $defineProperty;
      } catch (error) {
      }
      return result;
    }());
    var toString = {}.toString;
    var endsWith = function(search) {
      if (this == null) {
        throw TypeError();
      }
      var string = String(this);
      if (search && toString.call(search) == '[object RegExp]') {
        throw TypeError();
      }
      var stringLength = string.length;
      var searchString = String(search);
      var searchLength = searchString.length;
      var pos = stringLength;
      if (arguments.length > 1) {
        var position = arguments[1];
        if (position !== undefined) {
          // `ToInteger`
          pos = position ? Number(position) : 0;
          if (pos != pos) { // better `isNaN`
            pos = 0;
          }
        }
      }
      var end = Math.min(Math.max(pos, 0), stringLength);
      var start = end - searchLength;
      if (start < 0) {
        return false;
      }
      var index = -1;
      while (++index < searchLength) {
        if (string.charCodeAt(start + index) != searchString.charCodeAt(index)) {
          return false;
        }
      }
      return true;
    };
    if (defineProperty) {
      defineProperty(String.prototype, 'endsWith', {
        'value' : endsWith,
        'configurable' : true,
        'writable' : true
      });
    } else {
      String.prototype.endsWith = endsWith;
    }
  }());
}

// startsWith
/* ! https://mths.be/startswith v0.2.0 by @mathias */
if (!String.prototype.startsWith) {
  (function() {
    'use strict'; // needed to support `apply`/`call` with `undefined`/`null`
    var defineProperty = (function() {
      // IE 8 only supports `Object.defineProperty` on DOM elements
      try {
        var object = {};
        var $defineProperty = Object.defineProperty;
        var result = $defineProperty(object, object, object) && $defineProperty;
      } catch (error) {
      }
      return result;
    }());
    var toString = {}.toString;
    var startsWith = function(search) {
      if (this == null) {
        throw TypeError();
      }
      var string = String(this);
      if (search && toString.call(search) == '[object RegExp]') {
        throw TypeError();
      }
      var stringLength = string.length;
      var searchString = String(search);
      var searchLength = searchString.length;
      var position = arguments.length > 1 ? arguments[1] : undefined;
      // `ToInteger`
      var pos = position ? Number(position) : 0;
      if (pos != pos) { // better `isNaN`
        pos = 0;
      }
      var start = Math.min(Math.max(pos, 0), stringLength);
      // Avoid the `indexOf` call if no match is possible
      if (searchLength + start > stringLength) {
        return false;
      }
      var index = -1;
      while (++index < searchLength) {
        if (string.charCodeAt(start + index) != searchString.charCodeAt(index)) {
          return false;
        }
      }
      return true;
    };
    if (defineProperty) {
      defineProperty(String.prototype, 'startsWith', {
        'value' : startsWith,
        'configurable' : true,
        'writable' : true
      });
    } else {
      String.prototype.startsWith = startsWith;
    }
  }());
}

// isInteger
Number.isInteger = Number.isInteger || function(value) {
  return typeof value === "number" && isFinite(value) && Math.floor(value) === value;
};

/* https://stackoverflow.com/questions/31221341/ie-does-not-support-includes-method @Sunho Hong */
if (!Array.prototype.includes) {
  Object.defineProperty(Array.prototype, "includes", {
    enumerable : false,
    value : function(obj) {
      var newArr = this.filter(function(el) {
        return el == obj;
      });
      return newArr.length > 0;
    }
  });
}

/* https://stackoverflow.com/questions/31221341/ie-does-not-support-includes-method @InferOn and @Bill */
if (!String.prototype.includes) {
  String.prototype.includes = function(search, start) {
    'use strict';
    if (typeof start !== 'number') {
      start = 0;
    }

    if (start + search.length > this.length) {
      return false;
    } else {
      return this.indexOf(search, start) !== -1;
    }
  };
}

/**
 * Polyfill for Object.keys
 *
 */
if (!Object.keys) {
  Object.keys = (function () {
    var hasOwnProperty = Object.prototype.hasOwnProperty,
        hasDontEnumBug = !({toString: null}).propertyIsEnumerable('toString'),
        dontEnums = [
          'toString',
          'toLocaleString',
          'valueOf',
          'hasOwnProperty',
          'isPrototypeOf',
          'propertyIsEnumerable',
          'constructor'
        ],
        dontEnumsLength = dontEnums.length;
 
    return function (obj) {
      if (typeof obj !== 'object' && typeof obj !== 'function' || obj === null) throw new TypeError('Object.keys called on non-object');
 
      var result = [];
 
      for (var prop in obj) {
        if (hasOwnProperty.call(obj, prop)) result.push(prop);
      }
 
      if (hasDontEnumBug) {
        for (var i=0; i < dontEnumsLength; i++) {
          if (hasOwnProperty.call(obj, dontEnums[i])) result.push(dontEnums[i]);
        }
      }
      return result;
    }
  })()
};


// Object.entries
if (!Object.entries) {
  Object.entries = function( obj ){
    var ownProps = Object.keys( obj ),
        i = ownProps.length,
        resArray = new Array(i);
    while (i--)
      resArray[i] = [ownProps[i], obj[ownProps[i]]];
    
    return resArray;
  };
}