'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var Stream = require('stream');
var moment = require('moment');
var _ = require('lodash');
var numeral = require('@spalger/numeral');
var ansicolors = require('ansicolors');
var stringify = require('json-stringify-safe');
var querystring = require('querystring');
var inspect = require('util').inspect;

function serializeError(err) {
  return {
    message: err.message,
    name: err.name,
    stack: err.stack,
    code: err.code,
    signal: err.signal
  };
}

var levelColor = function levelColor(code) {
  if (code < 299) return ansicolors.green(code);
  if (code < 399) return ansicolors.yellow(code);
  if (code < 499) return ansicolors.magenta(code);
  return ansicolors.red(code);
};

module.exports = (function (_Stream$Transform) {
  _inherits(TransformObjStream, _Stream$Transform);

  function TransformObjStream() {
    _classCallCheck(this, TransformObjStream);

    _get(Object.getPrototypeOf(TransformObjStream.prototype), 'constructor', this).call(this, {
      readableObjectMode: false,
      writableObjectMode: true
    });
  }

  _createClass(TransformObjStream, [{
    key: '_transform',
    value: function _transform(event, enc, next) {
      var data = this.readEvent(event);
      this.push(this.format(data) + '\n');
      next();
    }
  }, {
    key: 'readEvent',
    value: function readEvent(event) {
      var data = {
        type: event.event,
        '@timestamp': moment.utc(event.timestamp).format(),
        tags: [].concat(event.tags || []),
        pid: event.pid
      };

      if (data.type === 'response') {
        _.defaults(data, _.pick(event, ['method', 'statusCode']));

        data.req = {
          url: event.path,
          method: event.method,
          headers: event.headers,
          remoteAddress: event.source.remoteAddress,
          userAgent: event.source.remoteAddress,
          referer: event.source.referer
        };

        var contentLength = 0;
        if (typeof event.responsePayload === 'object') {
          contentLength = stringify(event.responsePayload).length;
        } else {
          contentLength = String(event.responsePayload).length;
        }

        data.res = {
          statusCode: event.statusCode,
          responseTime: event.responseTime,
          contentLength: contentLength
        };

        var query = querystring.stringify(event.query);
        if (query) data.req.url += '?' + query;

        data.message = data.req.method.toUpperCase() + ' ';
        data.message += data.req.url;
        data.message += ' ';
        data.message += levelColor(data.res.statusCode);
        data.message += ' ';
        data.message += ansicolors.brightBlack(data.res.responseTime + 'ms');
        data.message += ansicolors.brightBlack(' - ' + numeral(contentLength).format('0.0b'));
      } else if (data.type === 'ops') {
        _.defaults(data, _.pick(event, ['pid', 'os', 'proc', 'load']));
        data.message = ansicolors.brightBlack('memory: ');
        data.message += numeral(data.proc.mem.heapUsed).format('0.0b');
        data.message += ' ';
        data.message += ansicolors.brightBlack('uptime: ');
        data.message += numeral(data.proc.uptime).format('00:00:00');
        data.message += ' ';
        data.message += ansicolors.brightBlack('load: [');
        data.message += data.os.load.map(function (val) {
          return numeral(val).format('0.00');
        }).join(' ');
        data.message += ansicolors.brightBlack(']');
        data.message += ' ';
        data.message += ansicolors.brightBlack('delay: ');
        data.message += numeral(data.proc.delay).format('0.000');
      } else if (data.type === 'error') {
        data.level = 'error';
        data.message = event.error.message;
        data.error = serializeError(event.error);
        data.url = event.url;
      } else if (event.data instanceof Error) {
        data.level = _.contains(event.tags, 'fatal') ? 'fatal' : 'error';
        data.message = event.data.message;
        data.error = serializeError(event.data);
      } else if (_.isPlainObject(event.data) && event.data.tmpl) {
        _.assign(data, event.data);
        data.tmpl = undefined;
        data.message = _.template(event.data.tmpl)(event.data);
      } else {
        data.message = _.isString(event.data) ? event.data : inspect(event.data);
      }
      return data;
    }
  }]);

  return TransformObjStream;
})(Stream.Transform);
