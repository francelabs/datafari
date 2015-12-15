'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _toConsumableArray = require('babel-runtime/helpers/to-consumable-array')['default'];

var _Symbol = require('babel-runtime/core-js/symbol')['default'];

var _ = require('lodash');
var UiApp = require('./UiApp');
var Collection = require('requirefrom')('src')('utils/Collection');

var byIdCache = _Symbol('byId');

module.exports = (function (_Collection) {
  _inherits(UiAppCollection, _Collection);

  function UiAppCollection(uiExports, parent) {
    _classCallCheck(this, UiAppCollection);

    _get(Object.getPrototypeOf(UiAppCollection.prototype), 'constructor', this).call(this);

    this.uiExports = uiExports;

    if (!parent) {
      this.claimedIds = [];
      this.hidden = new UiAppCollection(uiExports, this);
    } else {
      this.claimedIds = parent.claimedIds;
    }
  }

  _createClass(UiAppCollection, [{
    key: 'new',
    value: function _new(spec) {
      if (this.hidden && spec.hidden) {
        return this.hidden['new'](spec);
      }

      var app = new UiApp(this.uiExports, spec);

      if (_.includes(this.claimedIds, app.id)) {
        throw new Error('Unable to create two apps with the id ' + app.id + '.');
      } else {
        this.claimedIds.push(app.id);
      }

      this[byIdCache] = null;
      this.add(app);
      return app;
    }
  }, {
    key: 'byId',
    get: function get() {
      return this[byIdCache] || (this[byIdCache] = _.indexBy([].concat(_toConsumableArray(this)), 'id'));
    }
  }]);

  return UiAppCollection;
})(Collection);
