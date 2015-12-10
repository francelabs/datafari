'use strict';

var _Object$assign = require('babel-runtime/core-js/object/assign')['default'];

var fromRoot = require('requirefrom')('src/utils')('fromRoot');

exports.webpack = {
  stage: 1,
  nonStandard: false,
  optional: ['runtime']
};

exports.node = _Object$assign({
  ignore: [fromRoot('src'), /[\\\/](node_modules|bower_components)[\\\/]/]
}, exports.webpack);
