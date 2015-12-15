'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _ = require('lodash');

var states = require('./states');
var Status = require('./Status');

module.exports = (function () {
  function ServerStatus(server) {
    _classCallCheck(this, ServerStatus);

    this.server = server;
    this._created = {};
  }

  _createClass(ServerStatus, [{
    key: 'create',
    value: function create(name) {
      return this._created[name] = new Status(name, this.server);
    }
  }, {
    key: 'each',
    value: function each(fn) {
      var self = this;
      _.forOwn(self._created, function (status, i, list) {
        if (status.state !== 'disabled') {
          fn.call(self, status, i, list);
        }
      });
    }
  }, {
    key: 'get',
    value: function get(name) {
      return this._created[name];
    }
  }, {
    key: 'getState',
    value: function getState(name) {
      return _.get(this._created, [name, 'state'], 'uninitialized');
    }
  }, {
    key: 'overall',
    value: function overall() {
      var state = _(this._created).map(function (status) {
        return states.get(status.state);
      }).sortBy('severity').pop();

      var statuses = _.where(this._created, { state: state.id });
      var since = _.get(_.sortBy(statuses, 'since'), [0, 'since']);

      return {
        state: state.id,
        title: state.title,
        nickname: _.sample(state.nicknames),
        icon: state.icon,
        since: since
      };
    }
  }, {
    key: 'isGreen',
    value: function isGreen() {
      return this.overall().state === 'green';
    }
  }, {
    key: 'notGreen',
    value: function notGreen() {
      return !this.isGreen();
    }
  }, {
    key: 'toString',
    value: function toString() {
      var overall = this.overall();
      return overall.title + ' – ' + overall.nickname;
    }
  }, {
    key: 'toJSON',
    value: function toJSON() {
      return {
        overall: this.overall(),
        statuses: _.values(this._created)
      };
    }
  }]);

  return ServerStatus;
})();
