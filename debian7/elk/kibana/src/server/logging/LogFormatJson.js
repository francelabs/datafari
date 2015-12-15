'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var LogFormat = require('./LogFormat');
var stringify = require('json-stringify-safe');

var stripColors = function stripColors(string) {
  return string.replace(/\u001b[^m]+m/g, '');
};

module.exports = (function (_LogFormat) {
  _inherits(KbnLoggerJsonFormat, _LogFormat);

  function KbnLoggerJsonFormat() {
    _classCallCheck(this, KbnLoggerJsonFormat);

    _get(Object.getPrototypeOf(KbnLoggerJsonFormat.prototype), 'constructor', this).apply(this, arguments);
  }

  _createClass(KbnLoggerJsonFormat, [{
    key: 'format',
    value: function format(data) {
      data.message = stripColors(data.message);
      return stringify(data);
    }
  }]);

  return KbnLoggerJsonFormat;
})(LogFormat);
