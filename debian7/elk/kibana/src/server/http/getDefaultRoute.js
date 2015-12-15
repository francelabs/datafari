'use strict';

var _ = require('lodash');

module.exports = _.once(function (kbnServer) {
  // user configured default route
  var defaultConfig = kbnServer.config.get('server.defaultRoute');
  if (defaultConfig) return defaultConfig;

  // redirect to the single app
  var apps = kbnServer.uiExports.apps.toArray();
  return '/app/kibana';
});
