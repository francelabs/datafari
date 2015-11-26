'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _toConsumableArray = require('babel-runtime/helpers/to-consumable-array')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _ = require('lodash');
var minimatch = require('minimatch');

var UiAppCollection = require('./UiAppCollection');

var UiExports = (function () {
  function UiExports(kbnServer) {
    _classCallCheck(this, UiExports);

    this.apps = new UiAppCollection(this);
    this.aliases = {};
    this.exportConsumer = _.memoize(this.exportConsumer);
    this.consumers = [];
    this.bundleProviders = [];
  }

  _createClass(UiExports, [{
    key: 'consumePlugin',
    value: function consumePlugin(plugin) {
      var _this = this;

      plugin.apps = new UiAppCollection(this);

      var types = _.keys(plugin.uiExportsSpecs);
      if (!types) return false;

      var unkown = _.reject(types, this.exportConsumer, this);
      if (unkown.length) {
        throw new Error('unknown export types ' + unkown.join(', ') + ' in plugin ' + plugin.id);
      }

      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = _getIterator(this.consumers), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var consumer = _step.value;

          consumer.consumePlugin && consumer.consumePlugin(plugin);
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

      types.forEach(function (type) {
        _this.exportConsumer(type)(plugin, plugin.uiExportsSpecs[type]);
      });
    }
  }, {
    key: 'addConsumer',
    value: function addConsumer(consumer) {
      this.consumers.push(consumer);
    }
  }, {
    key: 'exportConsumer',
    value: function exportConsumer(type) {
      var _this2 = this;

      var _iteratorNormalCompletion2 = true;
      var _didIteratorError2 = false;
      var _iteratorError2 = undefined;

      try {
        for (var _iterator2 = _getIterator(this.consumers), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
          var consumer = _step2.value;

          if (!consumer.exportConsumer) continue;
          var fn = consumer.exportConsumer(type);
          if (fn) return fn;
        }
      } catch (err) {
        _didIteratorError2 = true;
        _iteratorError2 = err;
      } finally {
        try {
          if (!_iteratorNormalCompletion2 && _iterator2['return']) {
            _iterator2['return']();
          }
        } finally {
          if (_didIteratorError2) {
            throw _iteratorError2;
          }
        }
      }

      switch (type) {
        case 'app':
        case 'apps':
          return function (plugin, specs) {
            var _iteratorNormalCompletion3 = true;
            var _didIteratorError3 = false;
            var _iteratorError3 = undefined;

            try {
              for (var _iterator3 = _getIterator([].concat(specs || [])), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {
                var spec = _step3.value;

                var app = _this2.apps['new'](_.defaults({}, spec, { id: plugin.id }));
                plugin.apps.add(app);
              }
            } catch (err) {
              _didIteratorError3 = true;
              _iteratorError3 = err;
            } finally {
              try {
                if (!_iteratorNormalCompletion3 && _iterator3['return']) {
                  _iterator3['return']();
                }
              } finally {
                if (_didIteratorError3) {
                  throw _iteratorError3;
                }
              }
            }
          };

        case 'visTypes':
        case 'fieldFormats':
        case 'spyModes':
          return function (plugin, spec) {
            _this2.aliases[type] = _.union(_this2.aliases[type] || [], spec);
          };

        case 'bundle':
          return function (plugin, spec) {
            _this2.bundleProviders.push(spec);
          };

        case 'aliases':
          return function (plugin, specs) {
            _.forOwn(specs, function (spec, adhocType) {
              _this2.aliases[adhocType] = _.union(_this2.aliases[adhocType] || [], spec);
            });
          };
      }
    }
  }, {
    key: 'find',
    value: function find(patterns) {
      var aliases = this.aliases;
      var names = _.keys(aliases);
      var matcher = _.partialRight(minimatch.filter, { matchBase: true });

      return _.chain(patterns).map(function (pattern) {
        var matches = names.filter(matcher(pattern));
        if (!matches.length) {
          throw new Error('Unable to find uiExports for pattern ' + pattern);
        }
        return matches;
      }).flattenDeep().reduce(function (found, name) {
        return found.concat(aliases[name]);
      }, []).value();
    }
  }, {
    key: 'getAllApps',
    value: function getAllApps() {
      var _ref;

      var apps = this.apps;

      return (_ref = [].concat(_toConsumableArray(apps))).concat.apply(_ref, _toConsumableArray(apps.hidden));
    }
  }, {
    key: 'getApp',
    value: function getApp(id) {
      return this.apps.byId[id];
    }
  }, {
    key: 'getHiddenApp',
    value: function getHiddenApp(id) {
      return this.apps.hidden.byId[id];
    }
  }, {
    key: 'getBundleProviders',
    value: function getBundleProviders() {
      return this.bundleProviders;
    }
  }]);

  return UiExports;
})();

module.exports = UiExports;
