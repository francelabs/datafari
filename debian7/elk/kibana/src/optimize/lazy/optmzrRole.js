'use strict';

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _this = this;

module.exports = function callee$0$0(kbnServer, kibanaHapiServer, config) {
  var src, fromRoot, LazyServer, LazyOptimizer, server, ready, sendReady;
  return _regeneratorRuntime.async(function callee$0$0$(context$1$0) {
    while (1) switch (context$1$0.prev = context$1$0.next) {
      case 0:
        src = require('requirefrom')('src');
        fromRoot = src('utils/fromRoot');
        LazyServer = require('./LazyServer');
        LazyOptimizer = require('./LazyOptimizer');
        server = new LazyServer(config.get('optimize.lazyHost'), config.get('optimize.lazyPort'), new LazyOptimizer({
          log: function log(tags, data) {
            return kibanaHapiServer.log(tags, data);
          },
          env: kbnServer.bundles.env,
          bundles: kbnServer.bundles,
          profile: config.get('optimize.profile'),
          sourceMaps: config.get('optimize.sourceMaps'),
          prebuild: config.get('optimize.lazyPrebuild'),
          unsafeCache: config.get('optimize.unsafeCache')
        }));
        ready = false;

        sendReady = function sendReady() {
          if (!process.connected) return;
          process.send(['WORKER_BROADCAST', { optimizeReady: ready }]);
        };

        process.on('message', function (msg) {
          if (msg && msg.optimizeReady === '?') sendReady();
        });

        sendReady();

        context$1$0.next = 11;
        return _regeneratorRuntime.awrap(server.init());

      case 11:

        ready = true;
        sendReady();

      case 13:
      case 'end':
        return context$1$0.stop();
    }
  }, null, _this);
};
