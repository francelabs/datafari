'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _toConsumableArray = require('babel-runtime/helpers/to-consumable-array')['default'];

var _Symbol = require('babel-runtime/core-js/symbol')['default'];

var _Map = require('babel-runtime/core-js/map')['default'];

var _Object$create = require('babel-runtime/core-js/object/create')['default'];

var Promise = require('bluebird');
var Joi = require('joi');
var _ = require('lodash');

var _require = require('lodash');

var zipObject = _require.zipObject;

var override = require('./override');
var pkg = require('requirefrom')('src/utils')('packageJson');

var schema = _Symbol('Joi Schema');
var schemaKeys = _Symbol('Schema Extensions');
var vals = _Symbol('config values');
var pendingSets = _Symbol('Pending Settings');

module.exports = (function () {
  function Config(initialSchema, initialSettings) {
    _classCallCheck(this, Config);

    this[schemaKeys] = new _Map();

    this[vals] = _Object$create(null);
    this[pendingSets] = new _Map(_.pairs(_.cloneDeep(initialSettings || {})));

    if (initialSchema) this.extendSchema(initialSchema);
  }

  _createClass(Config, [{
    key: 'getPendingSets',
    value: function getPendingSets() {
      return this[pendingSets];
    }
  }, {
    key: 'extendSchema',
    value: function extendSchema(key, extension) {
      var _this = this;

      if (key && key.isJoi) {
        return _.each(key._inner.children, function (child) {
          _this.extendSchema(child.key, child.schema);
        });
      }

      if (this.has(key)) {
        throw new Error('Config schema already has key: ' + key);
      }

      this[schemaKeys].set(key, extension);
      this[schema] = null;

      var initialVals = this[pendingSets].get(key);
      if (initialVals) {
        this.set(key, initialVals);
        this[pendingSets]['delete'](key);
      } else {
        this._commit(this[vals]);
      }
    }
  }, {
    key: 'removeSchema',
    value: function removeSchema(key) {
      if (!this[schemaKeys].has(key)) {
        throw new TypeError('Unknown schema key: ' + key);
      }

      this[schema] = null;
      this[schemaKeys]['delete'](key);
      this[pendingSets]['delete'](key);
      delete this[vals][key];
    }
  }, {
    key: 'resetTo',
    value: function resetTo(obj) {
      this._commit(obj);
    }
  }, {
    key: 'set',
    value: function set(key, value) {
      // clone and modify the config
      var config = _.cloneDeep(this[vals]);
      if (_.isPlainObject(key)) {
        config = override(config, key);
      } else {
        _.set(config, key, value);
      }

      // attempt to validate the config value
      this._commit(config);
    }
  }, {
    key: '_commit',
    value: function _commit(newVals) {
      // resolve the current environment
      var env = newVals.env;
      delete newVals.env;
      if (_.isObject(env)) env = env.name;
      if (!env) env = process.env.NODE_ENV || 'production';

      var dev = env === 'development';
      var prod = env === 'production';

      // pass the environment as context so that it can be refed in config
      var context = {
        env: env,
        prod: prod,
        dev: dev,
        notProd: !prod,
        notDev: !dev,
        version: _.get(pkg, 'version'),
        buildNum: dev ? Math.pow(2, 53) - 1 : _.get(pkg, 'build.number', NaN),
        buildSha: dev ? 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX' : _.get(pkg, 'build.sha', '')
      };

      if (!context.dev && !context.prod) {
        throw new TypeError('Unexpected environment "' + env + '", expected one of "development" or "production"');
      }

      var results = Joi.validate(newVals, this.getSchema(), { context: context });

      if (results.error) {
        throw results.error;
      }

      this[vals] = results.value;
    }
  }, {
    key: 'get',
    value: function get(key) {
      if (!key) {
        return _.cloneDeep(this[vals]);
      }

      var value = _.get(this[vals], key);
      if (value === undefined) {
        if (!this.has(key)) {
          throw new Error('Unknown config key: ' + key);
        }
      }
      return _.cloneDeep(value);
    }
  }, {
    key: 'has',
    value: function has(key) {
      function has(key, schema, path) {
        path = path || [];
        // Catch the partial paths
        if (path.join('.') === key) return true;
        // Only go deep on inner objects with children
        if (schema._inner.children.length) {
          for (var i = 0; i < schema._inner.children.length; i++) {
            var child = schema._inner.children[i];
            // If the child is an object recurse through it's children and return
            // true if there's a match
            if (child.schema._type === 'object') {
              if (has(key, child.schema, path.concat([child.key]))) return true;
              // if the child matches, return true
            } else if (path.concat([child.key]).join('.') === key) {
                return true;
              }
          }
        }
      }

      if (_.isArray(key)) {
        // TODO: add .has() support for array keys
        key = key.join('.');
      }

      return !!has(key, this.getSchema());
    }
  }, {
    key: 'getSchema',
    value: function getSchema() {
      if (!this[schema]) {
        var objKeys = zipObject([].concat(_toConsumableArray(this[schemaKeys])));
        this[schema] = Joi.object().keys(objKeys)['default']();
      }

      return this[schema];
    }
  }]);

  return Config;
})();
