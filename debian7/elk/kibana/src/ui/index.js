'use strict';

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _this = this;

module.exports = function callee$0$0(kbnServer, server, config) {
  var _, Boom, formatUrl, _require, resolve, readFile, fromRoot, UiExports, UiBundle, UiBundleCollection, UiBundlerEnv, loadingGif, uiExports, bundlerEnv, _iteratorNormalCompletion, _didIteratorError, _iteratorError, _iterator, _step, plugin, bundles, _iteratorNormalCompletion2, _didIteratorError2, _iteratorError2, _iterator2, _step2, app, _iteratorNormalCompletion3, _didIteratorError3, _iteratorError3, _iterator3, _step3, gen, bundle;

  return _regeneratorRuntime.async(function callee$0$0$(context$1$0) {
    while (1) switch (context$1$0.prev = context$1$0.next) {
      case 0:
        _ = require('lodash');
        Boom = require('boom');
        formatUrl = require('url').format;
        _require = require('path');
        resolve = _require.resolve;
        readFile = require('fs').readFileSync;
        fromRoot = require('../utils/fromRoot');
        UiExports = require('./UiExports');
        UiBundle = require('./UiBundle');
        UiBundleCollection = require('./UiBundleCollection');
        UiBundlerEnv = require('./UiBundlerEnv');
        loadingGif = readFile(fromRoot('src/ui/public/loading.gif'), { encoding: 'base64' });
        uiExports = kbnServer.uiExports = new UiExports(kbnServer);
        bundlerEnv = new UiBundlerEnv(config.get('optimize.bundleDir'));

        bundlerEnv.addContext('env', config.get('env.name'));
        bundlerEnv.addContext('sourceMaps', config.get('optimize.sourceMaps'));
        bundlerEnv.addContext('kbnVersion', config.get('pkg.version'));
        bundlerEnv.addContext('buildNum', config.get('pkg.buildNum'));
        uiExports.addConsumer(bundlerEnv);

        _iteratorNormalCompletion = true;
        _didIteratorError = false;
        _iteratorError = undefined;
        context$1$0.prev = 22;
        for (_iterator = _getIterator(kbnServer.plugins); !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          plugin = _step.value;

          uiExports.consumePlugin(plugin);
        }

        context$1$0.next = 30;
        break;

      case 26:
        context$1$0.prev = 26;
        context$1$0.t0 = context$1$0['catch'](22);
        _didIteratorError = true;
        _iteratorError = context$1$0.t0;

      case 30:
        context$1$0.prev = 30;
        context$1$0.prev = 31;

        if (!_iteratorNormalCompletion && _iterator['return']) {
          _iterator['return']();
        }

      case 33:
        context$1$0.prev = 33;

        if (!_didIteratorError) {
          context$1$0.next = 36;
          break;
        }

        throw _iteratorError;

      case 36:
        return context$1$0.finish(33);

      case 37:
        return context$1$0.finish(30);

      case 38:
        bundles = kbnServer.bundles = new UiBundleCollection(bundlerEnv, config.get('optimize.bundleFilter'));
        _iteratorNormalCompletion2 = true;
        _didIteratorError2 = false;
        _iteratorError2 = undefined;
        context$1$0.prev = 42;

        for (_iterator2 = _getIterator(uiExports.getAllApps()); !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
          app = _step2.value;

          bundles.addApp(app);
        }

        context$1$0.next = 50;
        break;

      case 46:
        context$1$0.prev = 46;
        context$1$0.t1 = context$1$0['catch'](42);
        _didIteratorError2 = true;
        _iteratorError2 = context$1$0.t1;

      case 50:
        context$1$0.prev = 50;
        context$1$0.prev = 51;

        if (!_iteratorNormalCompletion2 && _iterator2['return']) {
          _iterator2['return']();
        }

      case 53:
        context$1$0.prev = 53;

        if (!_didIteratorError2) {
          context$1$0.next = 56;
          break;
        }

        throw _iteratorError2;

      case 56:
        return context$1$0.finish(53);

      case 57:
        return context$1$0.finish(50);

      case 58:
        _iteratorNormalCompletion3 = true;
        _didIteratorError3 = false;
        _iteratorError3 = undefined;
        context$1$0.prev = 61;
        _iterator3 = _getIterator(uiExports.getBundleProviders());

      case 63:
        if (_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done) {
          context$1$0.next = 72;
          break;
        }

        gen = _step3.value;
        context$1$0.next = 67;
        return _regeneratorRuntime.awrap(gen(UiBundle, bundlerEnv, uiExports.getAllApps()));

      case 67:
        bundle = context$1$0.sent;

        if (bundle) bundles.add(bundle);

      case 69:
        _iteratorNormalCompletion3 = true;
        context$1$0.next = 63;
        break;

      case 72:
        context$1$0.next = 78;
        break;

      case 74:
        context$1$0.prev = 74;
        context$1$0.t2 = context$1$0['catch'](61);
        _didIteratorError3 = true;
        _iteratorError3 = context$1$0.t2;

      case 78:
        context$1$0.prev = 78;
        context$1$0.prev = 79;

        if (!_iteratorNormalCompletion3 && _iterator3['return']) {
          _iterator3['return']();
        }

      case 81:
        context$1$0.prev = 81;

        if (!_didIteratorError3) {
          context$1$0.next = 84;
          break;
        }

        throw _iteratorError3;

      case 84:
        return context$1$0.finish(81);

      case 85:
        return context$1$0.finish(78);

      case 86:

        // render all views from the ui/views directory
        server.setupViews(resolve(__dirname, 'views'));
        server.exposeStaticFile('/loading.gif', resolve(__dirname, 'public/loading.gif'));

        server.route({
          path: '/app/{id}',
          method: 'GET',
          handler: function handler(req, reply) {
            var id = req.params.id;
            var app = uiExports.apps.byId[id];
            if (!app) return reply(Boom.notFound('Unknown app ' + id));

            if (kbnServer.status.isGreen()) {
              return reply.renderApp(app);
            } else {
              return reply.renderStatusPage();
            }
          }
        });

        server.decorate('reply', 'renderApp', function (app) {
          var payload = {
            app: app,
            nav: uiExports.apps,
            version: kbnServer.version,
            buildNum: config.get('pkg.buildNum'),
            buildSha: config.get('pkg.buildSha'),
            vars: app.getInjectedVars()
          };

          return this.view(app.templateName, {
            app: app,
            loadingGif: loadingGif,
            kibanaPayload: payload
          });
        });

      case 90:
      case 'end':
        return context$1$0.stop();
    }
  }, null, _this, [[22, 26, 30, 38], [31,, 33, 37], [42, 46, 50, 58], [51,, 53, 57], [61, 74, 78, 86], [79,, 81, 85]]);
};
