'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _require = require('bluebird');

var fromNode = _require.fromNode;

var _require2 = require('fs');

var writeFile = _require2.writeFile;

var BaseOptimizer = require('./BaseOptimizer');
var fromRoot = require('../utils/fromRoot');

module.exports = (function (_BaseOptimizer) {
  _inherits(FsOptimizer, _BaseOptimizer);

  function FsOptimizer() {
    _classCallCheck(this, FsOptimizer);

    _get(Object.getPrototypeOf(FsOptimizer.prototype), 'constructor', this).apply(this, arguments);
  }

  _createClass(FsOptimizer, [{
    key: 'init',
    value: function init() {
      return _regeneratorRuntime.async(function init$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(this.initCompiler());

          case 2:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'run',
    value: function run() {
      var stats;
      return _regeneratorRuntime.async(function run$(context$2$0) {
        var _this = this;

        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            if (this.compiler) {
              context$2$0.next = 3;
              break;
            }

            context$2$0.next = 3;
            return _regeneratorRuntime.awrap(this.init());

          case 3:
            context$2$0.next = 5;
            return _regeneratorRuntime.awrap(fromNode(function (cb) {
              _this.compiler.run(function (err, stats) {
                if (err || !stats) return cb(err);

                if (stats.hasErrors() || stats.hasWarnings()) {
                  return cb(_this.failedStatsToError(stats));
                } else {
                  cb(null, stats);
                }
              });
            }));

          case 5:
            stats = context$2$0.sent;

          case 6:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }]);

  return FsOptimizer;
})(BaseOptimizer);
