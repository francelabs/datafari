'use strict';

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _this = this;

module.exports = function callee$0$0(kbnServer, server, config) {
  var lazy, bundles, FsOptimizer, optimizer, start, seconds;
  return _regeneratorRuntime.async(function callee$0$0$(context$1$0) {
    while (1) switch (context$1$0.prev = context$1$0.next) {
      case 0:
        if (config.get('optimize.enabled')) {
          context$1$0.next = 2;
          break;
        }

        return context$1$0.abrupt('return');

      case 2:
        lazy = config.get('optimize.lazy');

        if (!lazy) {
          context$1$0.next = 7;
          break;
        }

        context$1$0.next = 6;
        return _regeneratorRuntime.awrap(kbnServer.mixin(require('./lazy/lazy')));

      case 6:
        return context$1$0.abrupt('return', context$1$0.sent);

      case 7:
        bundles = kbnServer.bundles;

        server.exposeStaticDir('/bundles/{path*}', bundles.env.workingDir);
        context$1$0.next = 11;
        return _regeneratorRuntime.awrap(bundles.writeEntryFiles());

      case 11:
        if (!config.get('optimize.useBundleCache')) {
          context$1$0.next = 15;
          break;
        }

        context$1$0.next = 14;
        return _regeneratorRuntime.awrap(bundles.getInvalidBundles());

      case 14:
        bundles = context$1$0.sent;

      case 15:
        if (bundles.getIds().length) {
          context$1$0.next = 18;
          break;
        }

        server.log(['debug', 'optimize'], 'All bundles are cached and ready to go!');
        return context$1$0.abrupt('return');

      case 18:
        FsOptimizer = require('./FsOptimizer');
        optimizer = new FsOptimizer({
          env: bundles.env,
          bundles: bundles,
          profile: config.get('optimize.profile'),
          sourceMaps: config.get('optimize.sourceMaps'),
          unsafeCache: config.get('optimize.unsafeCache')
        });

        server.log(['info', 'optimize'], 'Optimizing and caching ' + bundles.desc() + '. This may take a few minutes');

        start = Date.now();
        context$1$0.next = 24;
        return _regeneratorRuntime.awrap(optimizer.run());

      case 24:
        seconds = ((Date.now() - start) / 1000).toFixed(2);

        server.log(['info', 'optimize'], 'Optimization of ' + bundles.desc() + ' complete in ' + seconds + ' seconds');

      case 26:
      case 'end':
        return context$1$0.stop();
    }
  }, null, _this);
};

// the lazy optimizer sets up two threads, one is the server listening
// on 5601 and the other is a server listening on 5602 that builds the
// bundles in a "middleware" style.
//
// the server listening on 5601 may be restarted a number of times, depending
// on the watch setup managed by the cli. It proxies all bundles/* requests to
// the other server. The server on 5602 is long running, in order to prevent
// complete rebuilds of the optimize content.

// in prod, only bundle what looks invalid or missing

// we might not have any work to do

// only require the FsOptimizer when we need to
