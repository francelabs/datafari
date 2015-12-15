'use strict';

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _ = require('lodash');
var ansicolors = require('ansicolors');

var log = _.restParam(function (color, label, rest1) {
  console.log.apply(console, [color(' ' + _.trim(label) + ' ')].concat(rest1));
});

var color = require('./color');

module.exports = function Log(quiet, silent) {
  _classCallCheck(this, Log);

  this.good = quiet || silent ? _.noop : _.partial(log, color.green);
  this.warn = quiet || silent ? _.noop : _.partial(log, color.yellow);
  this.bad = silent ? _.noop : _.partial(log, color.red);
};
