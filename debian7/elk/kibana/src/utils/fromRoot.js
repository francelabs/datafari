'use strict';

var _ = require('lodash');
var root = require('./packageJson').__dirname;

var _require = require('path');

var join = _require.join;
var dirname = _require.dirname;
var normalize = _require.normalize;

module.exports = _.flow(_.partial(join, root), normalize);
