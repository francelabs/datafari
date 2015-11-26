'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _toConsumableArray = require('babel-runtime/helpers/to-consumable-array')['default'];

var _Symbol = require('babel-runtime/core-js/symbol')['default'];

var _Set = require('babel-runtime/core-js/set')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _require = require('lodash');

var get = _require.get;
var indexBy = _require.indexBy;

var inspect = require('util').inspect;

var PluginApi = require('./PluginApi');
var Collection = require('requirefrom')('src')('utils/Collection');

var byIdCache = _Symbol('byIdCache');
var pluginApis = _Symbol('pluginApis');

module.exports = (function (_Collection) {
  _inherits(Plugins, _Collection);

  function Plugins(kbnServer) {
    _classCallCheck(this, Plugins);

    _get(Object.getPrototypeOf(Plugins.prototype), 'constructor', this).call(this);
    this.kbnServer = kbnServer;
    this[pluginApis] = new _Set();
  }

  _createClass(Plugins, [{
    key: 'new',
    value: function _new(path) {
      var api, output, config, _iteratorNormalCompletion, _didIteratorError, _iteratorError, _iterator, _step, product, plugin, enabled;

      return _regeneratorRuntime.async(function _new$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            api = new PluginApi(this.kbnServer, path);

            this[pluginApis].add(api);

            output = [].concat(require(path)(api) || []);
            config = this.kbnServer.config;

            if (output.length) {
              context$2$0.next = 6;
              break;
            }

            return context$2$0.abrupt('return');

          case 6:

            // clear the byIdCache
            this[byIdCache] = null;

            _iteratorNormalCompletion = true;
            _didIteratorError = false;
            _iteratorError = undefined;
            context$2$0.prev = 10;
            _iterator = _getIterator(output);

          case 12:
            if (_iteratorNormalCompletion = (_step = _iterator.next()).done) {
              context$2$0.next = 26;
              break;
            }

            product = _step.value;

            if (!(product instanceof api.Plugin)) {
              context$2$0.next = 22;
              break;
            }

            plugin = product;

            this.add(plugin);

            context$2$0.next = 19;
            return _regeneratorRuntime.awrap(plugin.readConfig());

          case 19:
            enabled = context$2$0.sent;

            if (!enabled) this['delete'](plugin);
            return context$2$0.abrupt('continue', 23);

          case 22:
            throw new TypeError('unexpected plugin export ' + inspect(product));

          case 23:
            _iteratorNormalCompletion = true;
            context$2$0.next = 12;
            break;

          case 26:
            context$2$0.next = 32;
            break;

          case 28:
            context$2$0.prev = 28;
            context$2$0.t0 = context$2$0['catch'](10);
            _didIteratorError = true;
            _iteratorError = context$2$0.t0;

          case 32:
            context$2$0.prev = 32;
            context$2$0.prev = 33;

            if (!_iteratorNormalCompletion && _iterator['return']) {
              _iterator['return']();
            }

          case 35:
            context$2$0.prev = 35;

            if (!_didIteratorError) {
              context$2$0.next = 38;
              break;
            }

            throw _iteratorError;

          case 38:
            return context$2$0.finish(35);

          case 39:
            return context$2$0.finish(32);

          case 40:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[10, 28, 32, 40], [33,, 35, 39]]);
    }
  }, {
    key: 'getPluginApis',
    value: function getPluginApis() {
      return this[pluginApis];
    }
  }, {
    key: 'byId',
    get: function get() {
      return this[byIdCache] || (this[byIdCache] = indexBy([].concat(_toConsumableArray(this)), 'id'));
    }
  }]);

  return Plugins;
})(Collection);
