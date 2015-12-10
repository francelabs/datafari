'use strict';

module.exports = function (kbnServer) {
  var Config = require('./Config');
  var schema = require('./schema');

  kbnServer.config = new Config(schema, kbnServer.settings || {});
};
