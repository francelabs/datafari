'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _require = require('hapi');

var Server = _require.Server;

var _require2 = require('bluebird');

var fromNode = _require2.fromNode;

var Boom = require('boom');

module.exports = (function () {
  function LazyServer(host, port, optimizer) {
    _classCallCheck(this, LazyServer);

    this.optimizer = optimizer;
    this.server = new Server();
    this.server.connection({
      host: host,
      port: port
    });
  }

  _createClass(LazyServer, [{
    key: 'init',
    value: function init() {
      return _regeneratorRuntime.async(function init$(context$2$0) {
        var _this = this;

        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(this.optimizer.init());

          case 2:
            this.optimizer.bindToServer(this.server);
            context$2$0.next = 5;
            return _regeneratorRuntime.awrap(fromNode(function (cb) {
              return _this.server.start(cb);
            }));

          case 5:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }]);

  return LazyServer;
})();
