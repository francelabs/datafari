'use strict';

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _ = require('lodash');

var _require = require('cluster');

var isWorker = _require.isWorker;

var _require2 = require('path');

var resolve = _require2.resolve;

var cwd = process.cwd();
var src = require('requirefrom')('src');
var fromRoot = src('utils/fromRoot');

var canCluster = undefined;
try {
  require.resolve('../cluster/ClusterManager');
  canCluster = true;
} catch (e) {
  canCluster = false;
}

var pathCollector = function pathCollector() {
  var paths = [];
  return function (path) {
    paths.push(resolve(process.cwd(), path));
    return paths;
  };
};

var pluginDirCollector = pathCollector();
var pluginPathCollector = pathCollector();

module.exports = function (program) {
  var command = program.command('serve');

  command.description('Run the kibana server').collectUnknownOptions().option('-e, --elasticsearch <uri>', 'Elasticsearch instance').option('-c, --config <path>', 'Path to the config file', fromRoot('config/kibana.yml')).option('-p, --port <port>', 'The port to bind to', parseInt).option('-q, --quiet', 'Prevent all logging except errors').option('-Q, --silent', 'Prevent all logging').option('--verbose', 'Turns on verbose logging').option('-H, --host <host>', 'The host to bind to').option('-l, --log-file <path>', 'The file to log to').option('--plugin-dir <path>', 'A path to scan for plugins, this can be specified multiple ' + 'times to specify multiple directories', pluginDirCollector, [fromRoot('installedPlugins'), fromRoot('src/plugins')]).option('--plugin-path <path>', 'A path to a plugin which should be included by the server, ' + 'this can be specified multiple times to specify multiple paths', pluginPathCollector, []).option('--plugins <path>', 'an alias for --plugin-dir', pluginDirCollector);

  if (canCluster) {
    command.option('--dev', 'Run the server with development mode defaults').option('--no-watch', 'Prevents automatic restarts of the server in --dev mode');
  }

  command.action(function callee$1$0(opts) {
    var ClusterManager, readYamlConfig, KbnServer, settings, set, get, kbnServer, _kbnServer, server;

    return _regeneratorRuntime.async(function callee$1$0$(context$2$0) {
      while (1) switch (context$2$0.prev = context$2$0.next) {
        case 0:
          if (!(canCluster && opts.dev && !isWorker)) {
            context$2$0.next = 4;
            break;
          }

          ClusterManager = require('../cluster/ClusterManager');

          new ClusterManager(opts);
          return context$2$0.abrupt('return');

        case 4:
          readYamlConfig = require('./readYamlConfig');
          KbnServer = src('server/KbnServer');
          settings = readYamlConfig(opts.config);

          if (opts.dev) {
            try {
              _.merge(settings, readYamlConfig(fromRoot('config/kibana.dev.yml')));
            } catch (e) {
              null;
            }
          }

          set = _.partial(_.set, settings);
          get = _.partial(_.get, settings);

          if (opts.dev) {
            set('env', 'development');
            set('optimize.lazy', true);
          }

          if (opts.elasticsearch) set('elasticsearch.url', opts.elasticsearch);
          if (opts.port) set('server.port', opts.port);
          if (opts.host) set('server.host', opts.host);
          if (opts.quiet) set('logging.quiet', true);
          if (opts.silent) set('logging.silent', true);
          if (opts.verbose) set('logging.verbose', true);
          if (opts.logFile) set('logging.dest', opts.logFile);

          set('plugins.scanDirs', _.compact([].concat(get('plugins.scanDirs'), opts.pluginDir)));

          set('plugins.paths', [].concat(opts.pluginPath || []));

          kbnServer = {};
          context$2$0.prev = 21;

          kbnServer = new KbnServer(_.merge(settings, this.getUnknownOptions()));
          context$2$0.next = 25;
          return _regeneratorRuntime.awrap(kbnServer.ready());

        case 25:
          context$2$0.next = 35;
          break;

        case 27:
          context$2$0.prev = 27;
          context$2$0.t0 = context$2$0['catch'](21);
          _kbnServer = kbnServer;
          server = _kbnServer.server;

          if (server) server.log(['fatal'], context$2$0.t0);
          console.error('FATAL', context$2$0.t0);

          kbnServer.close();
          process.exit(1); // eslint-disable-line no-process-exit

        case 35:
          return context$2$0.abrupt('return', kbnServer);

        case 36:
        case 'end':
          return context$2$0.stop();
      }
    }, null, this, [[21, 27]]);
  });
};

// stop processing the action and handoff to cluster manager
