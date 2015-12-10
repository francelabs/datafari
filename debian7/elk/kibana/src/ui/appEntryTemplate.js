'use strict';

module.exports = function (_ref) {
  var env = _ref.env;
  var bundle = _ref.bundle;

  var pluginSlug = env.pluginInfo.sort().map(function (p) {
    return ' *  - ' + p;
  }).join('\n');

  var requires = bundle.modules.map(function (m) {
    return 'require(\'' + m + '\');';
  }).join('\n');

  return '\n/**\n * Test entry file\n *\n * This is programatically created and updated, do not modify\n *\n * context: ' + JSON.stringify(env.context) + '\n * includes code from:\n' + pluginSlug + '\n *\n */\n\nrequire(\'ui/chrome\')\n' + requires + '\nrequire(\'ui/chrome\').bootstrap(/* xoxo */);\n\n';
};
