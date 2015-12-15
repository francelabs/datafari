'use strict';

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _this = this;

module.exports = function callee$0$0(kbnServer, server, config) {
  var _require, isWorker;

  return _regeneratorRuntime.async(function callee$0$0$(context$1$0) {
    while (1) switch (context$1$0.prev = context$1$0.next) {
      case 0:
        _require = require('cluster');
        isWorker = _require.isWorker;

        if (isWorker) {
          context$1$0.next = 4;
          break;
        }

        throw new Error('lazy optimization is only available in "watch" mode');

      case 4:
        context$1$0.t0 = process.env.kbnWorkerType;
        context$1$0.next = context$1$0.t0 === 'optmzr' ? 7 : context$1$0.t0 === 'server' ? 10 : 13;
        break;

      case 7:
        context$1$0.next = 9;
        return _regeneratorRuntime.awrap(kbnServer.mixin(require('./optmzrRole')));

      case 9:
        return context$1$0.abrupt('break', 14);

      case 10:
        context$1$0.next = 12;
        return _regeneratorRuntime.awrap(kbnServer.mixin(require('./proxyRole')));

      case 12:
        return context$1$0.abrupt('break', 14);

      case 13:
        throw new Error('unknown kbnWorkerType "' + process.env.kbnWorkerType + '"');

      case 14:
      case 'end':
        return context$1$0.stop();
    }
  }, null, _this);
};

/**
 * When running in lazy mode two workers/threads run in one
 * of the modes: 'optmzr' or 'server'
 *
 * optmzr: this thread runs the LiveOptimizer and the LazyServer
 *   which serves the LiveOptimizer's output and blocks requests
 *   while the optimizer is running
 *
 * server: this thread runs the entire kibana server and proxies
 *   all requests for /bundles/* to the optmzr
 *
 * @param  {string} process.env.kbnWorkerType
 */
