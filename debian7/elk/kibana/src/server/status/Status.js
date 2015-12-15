'use strict';

var _get = require('babel-runtime/helpers/get')['default'];

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _ = require('lodash');
var EventEmitter = require('events').EventEmitter;
var states = require('./states');

var Status = (function (_EventEmitter) {
  _inherits(Status, _EventEmitter);

  function Status(name, server) {
    _classCallCheck(this, Status);

    _get(Object.getPrototypeOf(Status.prototype), 'constructor', this).call(this);

    this.name = name;
    this.since = new Date();
    this.state = 'uninitialized';
    this.message = 'uninitialized';

    this.on('change', function (previous, previousMsg) {
      this.since = new Date();
      var tags = ['status', name];
      tags.push(this.state === 'red' ? 'error' : 'info');

      server.log(tags, {
        tmpl: 'Status changed from <%= prevState %> to <%= state %><%= message ? " - " + message : "" %>',
        name: name,
        state: this.state,
        message: this.message,
        prevState: previous,
        prevMsg: previousMsg
      });
    });
  }

  _createClass(Status, [{
    key: 'toJSON',
    value: function toJSON() {
      return {
        name: this.name,
        state: this.state,
        icon: states.get(this.state).icon,
        message: this.message,
        since: this.since
      };
    }
  }]);

  return Status;
})(EventEmitter);

states.all.forEach(function (state) {
  Status.prototype[state.id] = function (message) {
    if (this.state === 'disabled') return;

    var previous = this.state;
    var previousMsg = this.message;

    this.error = null;
    this.message = message || state.title;
    this.state = state.id;

    if (message instanceof Error) {
      this.error = message;
      this.message = message.message;
    }

    if (previous === this.state && previousMsg === this.message) {
      // noop
      return;
    }

    this.emit(state.id, previous, previousMsg);
    this.emit('change', previous, previousMsg);
  };
});

module.exports = Status;
