'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _ = require('lodash');
var Joi = require('joi');

var _require = require('bluebird');

var attempt = _require.attempt;
var fromNode = _require.fromNode;

var _require2 = require('path');

var resolve = _require2.resolve;

var _require3 = require('util');

var inherits = _require3.inherits;

var defaultConfigSchema = Joi.object({
  enabled: Joi.boolean()['default'](true)
})['default']();

module.exports = (function () {
  function Plugin(kbnServer, path, pkg, opts) {
    _classCallCheck(this, Plugin);

    this.kbnServer = kbnServer;
    this.pkg = pkg;
    this.path = path;

    this.id = opts.id || pkg.name;
    this.uiExportsSpecs = opts.uiExports || {};
    this.requiredIds = opts.require || [];
    this.version = opts.version || pkg.version;
    this.publicDir = opts.publicDir !== false ? resolve(path, 'public') : null;
    this.externalCondition = opts.initCondition || _.constant(true);
    this.externalInit = opts.init || _.noop;
    this.getConfigSchema = opts.config || _.noop;
    this.init = _.once(this.init);
  }

  _createClass(Plugin, [{
    key: 'readConfig',
    value: function readConfig() {
      var schema, config;
      return _regeneratorRuntime.async(function readConfig$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(this.getConfigSchema(Joi));

          case 2:
            schema = context$2$0.sent;
            config = this.kbnServer.config;

            config.extendSchema(this.id, schema || defaultConfigSchema);

            if (!config.get([this.id, 'enabled'])) {
              context$2$0.next = 9;
              break;
            }

            return context$2$0.abrupt('return', true);

          case 9:
            config.removeSchema(this.id);
            return context$2$0.abrupt('return', false);

          case 11:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'init',
    value: function init() {
      var id, version, kbnServer, config, register;
      return _regeneratorRuntime.async(function init$(context$2$0) {
        var _this = this;

        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            id = this.id;
            version = this.version;
            kbnServer = this.kbnServer;
            config = kbnServer.config;

            register = function register(server, options, next) {
              _this.server = server;

              // bind the server and options to all
              // apps created by this plugin
              var _iteratorNormalCompletion = true;
              var _didIteratorError = false;
              var _iteratorError = undefined;

              try {
                for (var _iterator = _getIterator(_this.apps), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                  var app = _step.value;

                  app.getInjectedVars = _.partial(app.getInjectedVars, server, options);
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

              server.log(['plugins', 'debug'], {
                tmpl: 'Initializing plugin <%= plugin.id %>',
                plugin: _this
              });

              if (_this.publicDir) {
                server.exposeStaticDir('/plugins/' + id + '/{path*}', _this.publicDir);
              }

              _this.status = kbnServer.status.create('plugin:' + _this.id);
              server.expose('status', _this.status);

              attempt(_this.externalInit, [server, options], _this).nodeify(next);
            };

            register.attributes = { name: id, version: version };

            context$2$0.next = 8;
            return _regeneratorRuntime.awrap(fromNode(function (cb) {
              kbnServer.server.register({
                register: register,
                options: config.has(id) ? config.get(id) : null
              }, cb);
            }));

          case 8:

            // Only change the plugin status to green if the
            // intial status has not been changed
            if (this.status.state === 'uninitialized') {
              this.status.green('Ready');
            }

          case 9:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'toJSON',
    value: function toJSON() {
      return this.pkg;
    }
  }, {
    key: 'toString',
    value: function toString() {
      return this.id + '@' + this.version;
    }
  }], [{
    key: 'scoped',
    value: function scoped(kbnServer, path, pkg) {
      return (function (_Plugin) {
        _inherits(ScopedPlugin, _Plugin);

        function ScopedPlugin(opts) {
          _classCallCheck(this, ScopedPlugin);

          _get(Object.getPrototypeOf(ScopedPlugin.prototype), 'constructor', this).call(this, kbnServer, path, pkg, opts || {});
        }

        return ScopedPlugin;
      })(Plugin);
    }
  }]);

  return Plugin;
})();

// setup the hapi register function and get on with it
