'use strict';

var _require = require('bluebird');

var fromNode = _require.fromNode;

var _require2 = require('lodash');

var get = _require2.get;
var once = _require2.once;

module.exports = function (kbnServer, server, config) {

  server.route({
    path: '/bundles/{path*}',
    method: 'GET',
    handler: {
      proxy: {
        host: config.get('optimize.lazyHost'),
        port: config.get('optimize.lazyPort'),
        passThrough: true,
        xforward: true
      }
    }
  });

  return fromNode(function (cb) {
    var timeout = setTimeout(function () {
      cb(new Error('Server timedout waiting for the optimizer to become ready'));
    }, config.get('optimize.lazyProxyTimeout'));

    var waiting = once(function () {
      server.log(['info', 'optimize'], 'Waiting for optimizer completion');
    });

    if (!process.connected) return;

    process.send(['WORKER_BROADCAST', { optimizeReady: '?' }]);
    process.on('message', function (msg) {
      switch (get(msg, 'optimizeReady')) {
        case true:
          clearTimeout(timeout);
          cb();
          break;
        case false:
          waiting();
          break;
      }
    });
  });
};
