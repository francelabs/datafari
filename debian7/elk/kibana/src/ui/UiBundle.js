'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _require = require('path');

var join = _require.join;

var _require2 = require('bluebird');

var promisify = _require2.promisify;

var read = promisify(require('fs').readFile);
var write = promisify(require('fs').writeFile);
var unlink = promisify(require('fs').unlink);
var stat = promisify(require('fs').stat);

module.exports = (function () {
  function UiBundle(opts) {
    _classCallCheck(this, UiBundle);

    opts = opts || {};
    this.id = opts.id;
    this.modules = opts.modules;
    this.template = opts.template;
    this.env = opts.env;

    var pathBase = join(this.env.workingDir, this.id);
    this.entryPath = pathBase + '.entry.js';
    this.outputPath = pathBase + '.bundle.js';
  }

  _createClass(UiBundle, [{
    key: 'renderContent',
    value: function renderContent() {
      return this.template({
        env: this.env,
        bundle: this
      });
    }
  }, {
    key: 'readEntryFile',
    value: function readEntryFile() {
      var content;
      return _regeneratorRuntime.async(function readEntryFile$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.prev = 0;
            context$2$0.next = 3;
            return _regeneratorRuntime.awrap(read(this.entryPath));

          case 3:
            content = context$2$0.sent;
            return context$2$0.abrupt('return', content.toString('utf8'));

          case 7:
            context$2$0.prev = 7;
            context$2$0.t0 = context$2$0['catch'](0);
            return context$2$0.abrupt('return', null);

          case 10:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[0, 7]]);
    }
  }, {
    key: 'writeEntryFile',
    value: function writeEntryFile() {
      return _regeneratorRuntime.async(function writeEntryFile$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.next = 2;
            return _regeneratorRuntime.awrap(write(this.entryPath, this.renderContent(), { encoding: 'utf8' }));

          case 2:
            return context$2$0.abrupt('return', context$2$0.sent);

          case 3:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'clearBundleFile',
    value: function clearBundleFile() {
      return _regeneratorRuntime.async(function clearBundleFile$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.prev = 0;
            context$2$0.next = 3;
            return _regeneratorRuntime.awrap(unlink(this.outputPath));

          case 3:
            context$2$0.next = 8;
            break;

          case 5:
            context$2$0.prev = 5;
            context$2$0.t0 = context$2$0['catch'](0);
            return context$2$0.abrupt('return', null);

          case 8:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[0, 5]]);
    }
  }, {
    key: 'checkForExistingOutput',
    value: function checkForExistingOutput() {
      return _regeneratorRuntime.async(function checkForExistingOutput$(context$2$0) {
        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            context$2$0.prev = 0;
            context$2$0.next = 3;
            return _regeneratorRuntime.awrap(stat(this.outputPath));

          case 3:
            return context$2$0.abrupt('return', true);

          case 6:
            context$2$0.prev = 6;
            context$2$0.t0 = context$2$0['catch'](0);
            return context$2$0.abrupt('return', false);

          case 9:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this, [[0, 6]]);
    }
  }, {
    key: 'toJSON',
    value: function toJSON() {
      return {
        id: this.id,
        modules: this.modules,
        entryPath: this.entryPath,
        outputPath: this.outputPath
      };
    }
  }]);

  return UiBundle;
})();
