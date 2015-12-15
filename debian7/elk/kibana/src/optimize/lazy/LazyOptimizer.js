'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _require = require('lodash');

var once = _require.once;
var pick = _require.pick;
var size = _require.size;

var _require2 = require('path');

var join = _require2.join;

var Boom = require('boom');

var BaseOptimizer = require('../BaseOptimizer');
var WeirdControlFlow = require('./WeirdControlFlow');

module.exports = (function (_BaseOptimizer) {
  _inherits(LazyOptimizer, _BaseOptimizer);

  function LazyOptimizer(opts) {
    var _this = this;

    _classCallCheck(this, LazyOptimizer);

    _get(Object.getPrototypeOf(LazyOptimizer.prototype), 'constructor', this).call(this, opts);
    this.log = opts.log || function () {
      return null;
    };
    this.prebuild = opts.prebuild || false;

    this.timer = {
      ms: null,
      start: function start() {
        return _this.timer.ms = Date.now();
      },
      end: function end() {
        return _this.timer.ms = ((Date.now() - _this.timer.ms) / 1000).toFixed(2);
      }
    };

    this.build = new WeirdControlFlow();
  }

  _createClass(LazyOptimizer, [{
    key: 'init',
    value: function init() {
      var buildPromise;
      return _regeneratorRuntime.async(function init$(context$2$0) {
        var _this2 = this;

        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            this.initializing = true;

            context$2$0.next = 3;
            return _regeneratorRuntime.awrap(this.bundles.writeEntryFiles());

          case 3:
            context$2$0.next = 5;
            return _regeneratorRuntime.awrap(this.initCompiler());

          case 5:

            this.compiler.plugin('watch-run', function (w, webpackCb) {
              _this2.build.work(once(function () {
                _this2.timer.start();
                _this2.logRunStart();
                webpackCb();
              }));
            });

            this.compiler.plugin('done', function (stats) {
              if (!stats.hasErrors() && !stats.hasWarnings()) {
                _this2.logRunSuccess();
                _this2.build.success();
                return;
              }

              var err = _this2.failedStatsToError(stats);
              _this2.logRunFailure(err);
              _this2.build.failure(err);
              _this2.watching.invalidate();
            });

            this.watching = this.compiler.watch({ aggregateTimeout: 200 }, function (err) {
              if (err) {
                _this2.log('fatal', err);
                process.exit(1);
              }
            });

            buildPromise = this.build.get();

            if (!this.prebuild) {
              context$2$0.next = 12;
              break;
            }

            context$2$0.next = 12;
            return _regeneratorRuntime.awrap(buildPromise);

          case 12:

            this.initializing = false;
            this.log(['info', 'optimize'], {
              tmpl: 'Lazy optimization of ' + this.bundles.desc() + ' ready',
              bundles: this.bundles.getIds()
            });

          case 14:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'getPath',
    value: function getPath(relativePath) {
      return _regeneratorRuntime.async(function getPath$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(this.build.get());

          case 2:
            return context$2$0.abrupt('return', join(this.compiler.outputPath, relativePath));

          case 3:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'bindToServer',
    value: function bindToServer(server) {
      var _this3 = this;

      server.route({
        path: '/bundles/{asset*}',
        method: 'GET',
        handler: function handler(request, reply) {
          var path;
          return _regeneratorRuntime.async(function handler$(context$3$0) {
            while (1) switch (context$3$0.prev = context$3$0.next) {
              case 0:
                context$3$0.prev = 0;
                context$3$0.next = 3;
                return _regeneratorRuntime.awrap(this.getPath(request.params.asset));

              case 3:
                path = context$3$0.sent;
                return context$3$0.abrupt('return', reply.file(path));

              case 7:
                context$3$0.prev = 7;
                context$3$0.t0 = context$3$0['catch'](0);

                console.log(context$3$0.t0.stack);
                return context$3$0.abrupt('return', reply(context$3$0.t0));

              case 11:
              case 'end':
                return context$3$0.stop();
            }
          }, null, _this3, [[0, 7]]);
        }
      });
    }
  }, {
    key: 'logRunStart',
    value: function logRunStart() {
      this.log(['info', 'optimize'], {
        tmpl: 'Lazy optimization started',
        bundles: this.bundles.getIds()
      });
    }
  }, {
    key: 'logRunSuccess',
    value: function logRunSuccess() {
      this.log(['info', 'optimize'], {
        tmpl: 'Lazy optimization <%= status %> in <%= seconds %> seconds',
        bundles: this.bundles.getIds(),
        status: 'success',
        seconds: this.timer.end()
      });
    }
  }, {
    key: 'logRunFailure',
    value: function logRunFailure(err) {
      // errors during initialization to the server, unlike the rest of the
      // errors produced here. Lets not muddy the console with extra errors
      if (this.initializing) return;

      this.log(['fatal', 'optimize'], {
        tmpl: 'Lazy optimization <%= status %> in <%= seconds %> seconds<%= err %>',
        bundles: this.bundles.getIds(),
        status: 'failed',
        seconds: this.timer.end(),
        err: err
      });
    }
  }]);

  return LazyOptimizer;
})(BaseOptimizer);
