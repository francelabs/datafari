'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _ = require('lodash');
var ansicolors = require('ansicolors');
var moment = require('moment');

var LogFormat = require('./LogFormat');

var statuses = ['err', 'info', 'error', 'warning', 'fatal', 'status', 'debug'];

var typeColors = {
  log: 'blue',
  req: 'green',
  res: 'green',
  ops: 'cyan',
  err: 'red',
  info: 'green',
  error: 'red',
  warning: 'red',
  fatal: 'magenta',
  status: 'yellow',
  debug: 'brightBlack',
  server: 'brightBlack',
  optmzr: 'white',
  optimize: 'magenta',
  listening: 'magenta'
};

var color = _.memoize(function (name) {
  return ansicolors[typeColors[name]] || _.identity;
});

var type = _.memoize(function (t) {
  return color(t)(_.pad(t, 7).slice(0, 7));
});

var workerType = process.env.kbnWorkerType ? type(process.env.kbnWorkerType) + ' ' : '';

module.exports = (function (_LogFormat) {
  _inherits(KbnLoggerJsonFormat, _LogFormat);

  function KbnLoggerJsonFormat() {
    _classCallCheck(this, KbnLoggerJsonFormat);

    _get(Object.getPrototypeOf(KbnLoggerJsonFormat.prototype), 'constructor', this).apply(this, arguments);
  }

  _createClass(KbnLoggerJsonFormat, [{
    key: 'format',
    value: function format(data) {
      var time = color('time')(moment(data.timestamp).format('HH:mm:ss.SSS'));
      var msg = data.error ? color('error')(data.error.stack) : color('message')(data.message);

      var tags = _(data.tags).sortBy(function (tag) {
        if (color(tag) === _.identity) return '2' + tag;
        if (_.includes(statuses, tag)) return '0' + tag;
        return '1' + tag;
      }).reduce(function (s, t) {
        return s + ('[' + color(t)(t) + ']');
      }, '');

      return '' + workerType + type(data.type) + ' [' + time + '] ' + tags + ' ' + msg;
    }
  }]);

  return KbnLoggerJsonFormat;
})(LogFormat);
