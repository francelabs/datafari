'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _toConsumableArray = require('babel-runtime/helpers/to-consumable-array')['default'];

var _Symbol = require('babel-runtime/core-js/symbol')['default'];

var _Set = require('babel-runtime/core-js/set')['default'];

var _Symbol$iterator = require('babel-runtime/core-js/symbol/iterator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _Symbol$species = require('babel-runtime/core-js/symbol/species')['default'];

var set = _Symbol('internal set');

module.exports = (function () {
  function Collection() {
    _classCallCheck(this, Collection);

    // Set's have a length of 0, mimic that
    this[set] = new _Set(arguments[0] || []);
  }

  /******
   ** Collection API
   ******/

  _createClass(Collection, [{
    key: 'toArray',
    value: function toArray() {
      return [].concat(_toConsumableArray(this.values()));
    }
  }, {
    key: 'toJSON',
    value: function toJSON() {
      return this.toArray();
    }

    /******
     ** ES Set Api
     ******/

  }, {
    key: 'add',
    value: function add(value) {
      return this[set].add(value);
    }
  }, {
    key: 'clear',
    value: function clear() {
      return this[set].clear();
    }
  }, {
    key: 'delete',
    value: function _delete(value) {
      return this[set]['delete'](value);
    }
  }, {
    key: 'entries',
    value: function entries() {
      return this[set].entries();
    }
  }, {
    key: 'forEach',
    value: function forEach(callbackFn, thisArg) {
      return this[set].forEach(callbackFn, thisArg);
    }
  }, {
    key: 'has',
    value: function has(value) {
      return this[set].has(value);
    }
  }, {
    key: 'keys',
    value: function keys() {
      return this[set].keys();
    }
  }, {
    key: 'values',
    value: function values() {
      return this[set].values();
    }
  }, {
    key: _Symbol$iterator,
    value: function value() {
      return _getIterator(this[set]);
    }
  }, {
    key: 'size',
    get: function get() {
      return this[set].size;
    }
  }], [{
    key: _Symbol$species,
    get: function get() {
      return Collection;
    }
  }]);

  return Collection;
})();
