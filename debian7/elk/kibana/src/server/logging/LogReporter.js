'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _ = require('lodash');
var Squeeze = require('good-squeeze').Squeeze;
var writeStr = require('fs').createWriteStream;

var LogFormatJson = require('./LogFormatJson');
var LogFormatString = require('./LogFormatString');

module.exports = (function () {
  function KbnLogger(events, config) {
    _classCallCheck(this, KbnLogger);

    this.squeeze = new Squeeze(events);
    this.format = config.json ? new LogFormatJson() : new LogFormatString();

    if (config.dest === 'stdout') {
      this.dest = process.stdout;
    } else {
      this.dest = writeStr(config.dest, {
        flags: 'a',
        encoding: 'utf8'
      });
    }
  }

  _createClass(KbnLogger, [{
    key: 'init',
    value: function init(readstream, emitter, callback) {
      readstream.pipe(this.squeeze).pipe(this.format).pipe(this.dest);

      emitter.on('stop', _.noop);

      callback();
    }
  }]);

  return KbnLogger;
})();
