'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _ = require('lodash');
var Plugin = require('./Plugin');

var _require = require('path');

var basename = _require.basename;
var join = _require.join;

module.exports = (function () {
  function PluginApi(kibana, pluginPath) {
    _classCallCheck(this, PluginApi);

    this.config = kibana.config;
    this.rootDir = kibana.rootDir;
    this['package'] = require(join(pluginPath, 'package.json'));
    this.autoload = require('../../ui/autoload');
    this.Plugin = Plugin.scoped(kibana, pluginPath, this['package']);
  }

  _createClass(PluginApi, [{
    key: 'uiExports',
    get: function get() {
      throw new Error('plugin.uiExports is not defined until initialize phase');
    }
  }]);

  return PluginApi;
})();
