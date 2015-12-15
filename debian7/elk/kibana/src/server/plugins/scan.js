'use strict';

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _this3 = this;

module.exports = function callee$0$0(kbnServer, server, config) {
  var _, _require, fromNode, _require2, readdir, stat, _require3, resolve, _require4, each, PluginCollection, plugins, scanDirs, pluginPaths, debug, warning, _iteratorNormalCompletion, _didIteratorError, _iteratorError, _iterator, _step, path, modulePath;

  return _regeneratorRuntime.async(function callee$0$0$(context$1$0) {
    var _this2 = this;

    while (1) switch (context$1$0.prev = context$1$0.next) {
      case 0:
        _ = require('lodash');
        _require = require('bluebird');
        fromNode = _require.fromNode;
        _require2 = require('fs');
        readdir = _require2.readdir;
        stat = _require2.stat;
        _require3 = require('path');
        resolve = _require3.resolve;
        _require4 = require('bluebird');
        each = _require4.each;
        PluginCollection = require('./PluginCollection');
        plugins = kbnServer.plugins = new PluginCollection(kbnServer);
        scanDirs = [].concat(config.get('plugins.scanDirs') || []);
        pluginPaths = [].concat(config.get('plugins.paths') || []);
        debug = _.bindKey(server, 'log', ['plugins', 'debug']);
        warning = _.bindKey(server, 'log', ['plugins', 'warning']);
        context$1$0.next = 18;
        return _regeneratorRuntime.awrap(each(scanDirs, function callee$1$0(dir) {
          var filenames;
          return _regeneratorRuntime.async(function callee$1$0$(context$2$0) {
            var _this = this;

            while (1) switch (context$2$0.prev = context$2$0.next) {
              case 0:
                debug({ tmpl: 'Scanning `<%= dir %>` for plugins', dir: dir });

                filenames = null;
                context$2$0.prev = 2;
                context$2$0.next = 5;
                return _regeneratorRuntime.awrap(fromNode(function (cb) {
                  return readdir(dir, cb);
                }));

              case 5:
                filenames = context$2$0.sent;
                context$2$0.next = 14;
                break;

              case 8:
                context$2$0.prev = 8;
                context$2$0.t0 = context$2$0['catch'](2);

                if (!(context$2$0.t0.code !== 'ENOENT')) {
                  context$2$0.next = 12;
                  break;
                }

                throw context$2$0.t0;

              case 12:

                filenames = [];
                warning({
                  tmpl: '<%= err.code %>: Unable to scan non-existent directory for plugins "<%= dir %>"',
                  err: context$2$0.t0,
                  dir: dir
                });

              case 14:
                context$2$0.next = 16;
                return _regeneratorRuntime.awrap(each(filenames, function callee$2$0(name) {
                  var path, stats;
                  return _regeneratorRuntime.async(function callee$2$0$(context$3$0) {
                    while (1) switch (context$3$0.prev = context$3$0.next) {
                      case 0:
                        if (!(name[0] === '.')) {
                          context$3$0.next = 2;
                          break;
                        }

                        return context$3$0.abrupt('return');

                      case 2:
                        path = resolve(dir, name);
                        context$3$0.next = 5;
                        return _regeneratorRuntime.awrap(fromNode(function (cb) {
                          return stat(path, cb);
                        }));

                      case 5:
                        stats = context$3$0.sent;

                        if (stats.isDirectory()) {
                          pluginPaths.push(path);
                        }

                      case 7:
                      case 'end':
                        return context$3$0.stop();
                    }
                  }, null, _this);
                }));

              case 16:
              case 'end':
                return context$2$0.stop();
            }
          }, null, _this2, [[2, 8]]);
        }));

      case 18:
        _iteratorNormalCompletion = true;
        _didIteratorError = false;
        _iteratorError = undefined;
        context$1$0.prev = 21;
        _iterator = _getIterator(pluginPaths);

      case 23:
        if (_iteratorNormalCompletion = (_step = _iterator.next()).done) {
          context$1$0.next = 40;
          break;
        }

        path = _step.value;
        modulePath = undefined;
        context$1$0.prev = 26;

        modulePath = require.resolve(path);
        context$1$0.next = 34;
        break;

      case 30:
        context$1$0.prev = 30;
        context$1$0.t0 = context$1$0['catch'](26);

        warning({ tmpl: 'Skipping non-plugin directory at <%= path %>', path: path });
        return context$1$0.abrupt('continue', 37);

      case 34:
        context$1$0.next = 36;
        return _regeneratorRuntime.awrap(plugins['new'](path));

      case 36:
        debug({ tmpl: 'Found plugin at <%= path %>', path: modulePath });

      case 37:
        _iteratorNormalCompletion = true;
        context$1$0.next = 23;
        break;

      case 40:
        context$1$0.next = 46;
        break;

      case 42:
        context$1$0.prev = 42;
        context$1$0.t1 = context$1$0['catch'](21);
        _didIteratorError = true;
        _iteratorError = context$1$0.t1;

      case 46:
        context$1$0.prev = 46;
        context$1$0.prev = 47;

        if (!_iteratorNormalCompletion && _iterator['return']) {
          _iterator['return']();
        }

      case 49:
        context$1$0.prev = 49;

        if (!_didIteratorError) {
          context$1$0.next = 52;
          break;
        }

        throw _iteratorError;

      case 52:
        return context$1$0.finish(49);

      case 53:
        return context$1$0.finish(46);

      case 54:
      case 'end':
        return context$1$0.stop();
    }
  }, null, _this3, [[21, 42, 46, 54], [26, 30], [47,, 49, 53]]);
};

// scan all scanDirs to find pluginPaths
