'use strict';

module.exports = function (kbnServer, server, config) {
  var _ = require('lodash');
  var Samples = require('./Samples');
  var lastReport = Date.now();

  kbnServer.metrics = new Samples(12);

  server.plugins.good.monitor.on('ops', function (event) {
    var now = Date.now();
    var secSinceLast = (now - lastReport) / 1000;
    lastReport = now;

    var port = config.get('server.port');
    var requests = _.get(event, ['requests', port, 'total'], 0);
    var requestsPerSecond = requests / secSinceLast;

    kbnServer.metrics.add({
      heapTotal: _.get(event, 'psmem.heapTotal'),
      heapUsed: _.get(event, 'psmem.heapUsed'),
      load: event.osload,
      responseTimeAvg: _.get(event, ['responseTimes', port, 'avg']),
      responseTimeMax: _.get(event, ['responseTimes', port, 'max']),
      requestsPerSecond: requestsPerSecond
    });
  });
};
