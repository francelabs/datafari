'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _require = require('bluebird');

var fromNode = _require.fromNode;

module.exports = (function () {
  function WeirdControlFlow(work) {
    _classCallCheck(this, WeirdControlFlow);

    this.handlers = [];
  }

  _createClass(WeirdControlFlow, [{
    key: 'get',
    value: function get() {
      var _this = this;

      return fromNode(function (cb) {
        if (_this.ready) return cb();
        _this.handlers.push(cb);
        _this.start();
      });
    }
  }, {
    key: 'work',
    value: function work(_work) {
      this._work = _work;
      this.stop();

      if (this.handlers.length) {
        this.start();
      }
    }
  }, {
    key: 'start',
    value: function start() {
      if (this.running) return;
      this.stop();
      if (this._work) {
        this.running = true;
        this._work();
      }
    }
  }, {
    key: 'stop',
    value: function stop() {
      this.ready = false;
      this.error = false;
      this.running = false;
    }
  }, {
    key: 'success',
    value: function success() {
      this.stop();
      this.ready = true;

      for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }

      this._flush(args);
    }
  }, {
    key: 'failure',
    value: function failure(err) {
      this.stop();
      this.error = err;
      this._flush([err]);
    }
  }, {
    key: '_flush',
    value: function _flush(args) {
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = _getIterator(this.handlers.splice(0)), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var fn = _step.value;

          fn.apply(null, args);
        }
      } catch (err) {
        _didIteratorError = true;
        _iteratorError = err;
      } finally {
        try {
          if (!_iteratorNormalCompletion && _iterator['return']) {
            _iterator['return']();
          }
        } finally {
          if (_didIteratorError) {
            throw _iteratorError;
          }
        }
      }
    }
  }]);

  return WeirdControlFlow;
})();
