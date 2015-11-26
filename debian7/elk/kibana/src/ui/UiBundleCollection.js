'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _getIterator = require('babel-runtime/core-js/get-iterator')['default'];

var _require = require('lodash');

var pull = _require.pull;
var transform = _require.transform;
var pluck = _require.pluck;

var _require2 = require('path');

var join = _require2.join;

var _require3 = require('bluebird');

var resolve = _require3.resolve;
var promisify = _require3.promisify;

var _require4 = require('minimatch');

var makeRe = _require4.makeRe;

var rimraf = promisify(require('rimraf'));
var mkdirp = promisify(require('mkdirp'));
var unlink = promisify(require('fs').unlink);
var readdir = promisify(require('fs').readdir);
var readSync = require('fs').readFileSync;

var UiBundle = require('./UiBundle');
var appEntryTemplate = require('./appEntryTemplate');

var UiBundleCollection = (function () {
  function UiBundleCollection(bundlerEnv, filter) {
    _classCallCheck(this, UiBundleCollection);

    this.each = [];
    this.env = bundlerEnv;
    this.filter = makeRe(filter || '*', {
      noglobstar: true,
      noext: true,
      matchBase: true
    });
  }

  _createClass(UiBundleCollection, [{
    key: 'add',
    value: function add(bundle) {
      if (!(bundle instanceof UiBundle)) {
        throw new TypeError('expected bundle to be an instance of UiBundle');
      }

      if (this.filter.test(bundle.id)) {
        this.each.push(bundle);
      }
    }
  }, {
    key: 'addApp',
    value: function addApp(app) {
      this.add(new UiBundle({
        id: app.id,
        modules: app.getModules(),
        template: appEntryTemplate,
        env: this.env
      }));
    }
  }, {
    key: 'desc',
    value: function desc() {
      switch (this.each.length) {
        case 0:
          return '0 bundles';
        case 1:
          return 'bundle for ' + this.each[0].id;
        default:
          var ids = this.getIds();
          var last = ids.pop();
          var commas = ids.join(', ');
          return 'bundles for ' + commas + ' and ' + last;
      }
    }
  }, {
    key: 'ensureDir',
    value: function ensureDir() {
      return _regeneratorRuntime.async(function ensureDir$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(mkdirp(this.env.workingDir));

          case 2:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'writeEntryFiles',
    value: function writeEntryFiles() {
      var _iteratorNormalCompletion, _didIteratorError, _iteratorError, _iterator, _step, bundle, existing, expected;

      return _regeneratorRuntime.async(function writeEntryFiles$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(this.ensureDir());

          case 2:
            _iteratorNormalCompletion = true;
            _didIteratorError = false;
            _iteratorError = undefined;
            context$2$0.prev = 5;
            _iterator = _getIterator(this.each);

          case 7:
            if (_iteratorNormalCompletion = (_step = _iterator.next()).done) {
              context$2$0.next = 21;
              break;
            }

            bundle = _step.value;
            context$2$0.next = 11;
            return _regeneratorRuntime.awrap(bundle.readEntryFile());

          case 11:
            existing = context$2$0.sent;
            expected = bundle.renderContent();

            if (!(existing !== expected)) {
              context$2$0.next = 18;
              break;
            }

            context$2$0.next = 16;
            return _regeneratorRuntime.awrap(bundle.writeEntryFile());

          case 16:
            context$2$0.next = 18;
            return _regeneratorRuntime.awrap(bundle.clearBundleFile());

          case 18:
            _iteratorNormalCompletion = true;
            context$2$0.next = 7;
            break;

          case 21:
            context$2$0.next = 27;
            break;

          case 23:
            context$2$0.prev = 23;
            context$2$0.t0 = context$2$0['catch'](5);
            _didIteratorError = true;
            _iteratorError = context$2$0.t0;

          case 27:
            context$2$0.prev = 27;
            context$2$0.prev = 28;

            if (!_iteratorNormalCompletion && _iterator['return']) {
              _iterator['return']();
            }

          case 30:
            context$2$0.prev = 30;

            if (!_didIteratorError) {
              context$2$0.next = 33;
              break;
            }

            throw _iteratorError;

          case 33:
            return context$2$0.finish(30);

          case 34:
            return context$2$0.finish(27);

          case 35:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[5, 23, 27, 35], [28,, 30, 34]]);
    }
  }, {
    key: 'getInvalidBundles',
    value: function getInvalidBundles() {
      var invalids, _iteratorNormalCompletion2, _didIteratorError2, _iteratorError2, _iterator2, _step2, bundle, exists;

      return _regeneratorRuntime.async(function getInvalidBundles$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            invalids = new UiBundleCollection(this.env);
            _iteratorNormalCompletion2 = true;
            _didIteratorError2 = false;
            _iteratorError2 = undefined;
            context$2$0.prev = 4;
            _iterator2 = _getIterator(this.each);

          case 6:
            if (_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done) {
              context$2$0.next = 15;
              break;
            }

            bundle = _step2.value;
            context$2$0.next = 10;
            return _regeneratorRuntime.awrap(bundle.checkForExistingOutput());

          case 10:
            exists = context$2$0.sent;

            if (!exists) {
              invalids.add(bundle);
            }

          case 12:
            _iteratorNormalCompletion2 = true;
            context$2$0.next = 6;
            break;

          case 15:
            context$2$0.next = 21;
            break;

          case 17:
            context$2$0.prev = 17;
            context$2$0.t0 = context$2$0['catch'](4);
            _didIteratorError2 = true;
            _iteratorError2 = context$2$0.t0;

          case 21:
            context$2$0.prev = 21;
            context$2$0.prev = 22;

            if (!_iteratorNormalCompletion2 && _iterator2['return']) {
              _iterator2['return']();
            }

          case 24:
            context$2$0.prev = 24;

            if (!_didIteratorError2) {
              context$2$0.next = 27;
              break;
            }

            throw _iteratorError2;

          case 27:
            return context$2$0.finish(24);

          case 28:
            return context$2$0.finish(21);

          case 29:
            return context$2$0.abrupt('return', invalids);

          case 30:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[4, 17, 21, 29], [22,, 24, 28]]);
    }
  }, {
    key: 'toWebpackEntries',
    value: function toWebpackEntries() {
      return transform(this.each, function (entries, bundle) {
        entries[bundle.id] = bundle.entryPath;
      }, {});
    }
  }, {
    key: 'getIds',
    value: function getIds() {
      return pluck(this.each, 'id');
    }
  }, {
    key: 'toJSON',
    value: function toJSON() {
      return this.each;
    }
  }]);

  return UiBundleCollection;
})();

module.exports = UiBundleCollection;
