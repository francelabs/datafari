'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _require = require('lodash');

var constant = _require.constant;
var once = _require.once;
var compact = _require.compact;
var flatten = _require.flatten;

var _require2 = require('bluebird');

var promisify = _require2.promisify;
var resolve = _require2.resolve;
var fromNode = _require2.fromNode;

var Hapi = require('hapi');

var utils = require('requirefrom')('src/utils');
var rootDir = utils('fromRoot')('.');
var pkg = utils('packageJson');

module.exports = (function () {
  function KbnServer(settings) {
    var _this = this;

    _classCallCheck(this, KbnServer);

    this.name = pkg.name;
    this.version = pkg.version;
    this.build = pkg.build || false;
    this.rootDir = rootDir;
    this.settings = settings || {};

    this.ready = constant(this.mixin(require('./config/setup'), // sets this.config, reads this.settings
    require('./http'), // sets this.server
    require('./logging'), require('./status'),

    // find plugins and set this.plugins
    require('./plugins/scan'),

    // tell the config we are done loading plugins
    require('./config/complete'),

    // setup this.uiExports and this.bundles
    require('../ui'),

    // ensure that all bundles are built, or that the
    // lazy bundle server is running
    require('../optimize'),

    // finally, initialize the plugins
    require('./plugins/initialize'), function () {
      if (_this.config.get('server.autoListen')) {
        _this.ready = constant(resolve());
        return _this.listen();
      }
    }));

    this.listen = once(this.listen);
  }

  /**
   * Extend the KbnServer outside of the constraits of a plugin. This allows access
   * to APIs that are not exposed (intentionally) to the plugins and should only
   * be used when the code will be kept up to date with Kibana.
   *
   * @param {...function} - functions that should be called to mixin functionality.
   *                         They are called with the arguments (kibana, server, config)
   *                         and can return a promise to delay execution of the next mixin
   * @return {Promise} - promise that is resolved when the final mixin completes.
   */

  _createClass(KbnServer, [{
    key: 'mixin',
    value: function mixin() {
      var _iteratorNormalCompletion,
          _didIteratorError,
          _iteratorError,
          _len,
          fns,
          _key,
          _iterator,
          _step,
          fn,
          args$2$0 = arguments;

      return _regeneratorRuntime.async(function mixin$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            _iteratorNormalCompletion = true;
            _didIteratorError = false;
            _iteratorError = undefined;
            context$2$0.prev = 3;

            for (_len = args$2$0.length, fns = Array(_len), _key = 0; _key < _len; _key++) {
              fns[_key] = args$2$0[_key];
            }

            _iterator = _getIterator(compact(flatten(fns)));

          case 6:
            if (_iteratorNormalCompletion = (_step = _iterator.next()).done) {
              context$2$0.next = 13;
              break;
            }

            fn = _step.value;
            context$2$0.next = 10;
            return _regeneratorRuntime.awrap(fn.call(this, this, this.server, this.config));

          case 10:
            _iteratorNormalCompletion = true;
            context$2$0.next = 6;
            break;

          case 13:
            context$2$0.next = 19;
            break;

          case 15:
            context$2$0.prev = 15;
            context$2$0.t0 = context$2$0['catch'](3);
            _didIteratorError = true;
            _iteratorError = context$2$0.t0;

          case 19:
            context$2$0.prev = 19;
            context$2$0.prev = 20;

            if (!_iteratorNormalCompletion && _iterator['return']) {
              _iterator['return']();
            }

          case 22:
            context$2$0.prev = 22;

            if (!_didIteratorError) {
              context$2$0.next = 25;
              break;
            }

            throw _iteratorError;

          case 25:
            return context$2$0.finish(22);

          case 26:
            return context$2$0.finish(19);

          case 27:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[3, 15, 19, 27], [20,, 22, 26]]);
    }

    /**
     * Tell the server to listen for incoming requests, or get
     * a promise that will be resolved once the server is listening.
     *
     * @return undefined
     */
  }, {
    key: 'listen',
    value: function listen() {
      var server, config;
      return _regeneratorRuntime.async(function listen$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            server = this.server;
            config = this.config;
            context$2$0.next = 4;
            return _regeneratorRuntime.awrap(this.ready());

          case 4:
            context$2$0.next = 6;
            return _regeneratorRuntime.awrap(fromNode(function (cb) {
              return server.start(cb);
            }));

          case 6:
            context$2$0.next = 8;
            return _regeneratorRuntime.awrap(require('./pid')(this, server, config));

          case 8:

            server.log(['listening', 'info'], 'Server running at ' + server.info.uri);
            return context$2$0.abrupt('return', server);

          case 10:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'close',
    value: function close() {
      return _regeneratorRuntime.async(function close$(context$2$0) {
        var _this2 = this;

        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(fromNode(function (cb) {
              return _this2.server.stop(cb);
            }));

          case 2:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }]);

  return KbnServer;
})();
