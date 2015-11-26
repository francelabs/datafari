'use strict';

var _ = require('lodash');

module.exports = function (command, spaces) {
  if (!_.size(command.commands)) {
    return command.outputHelp();
  }

  var defCmd = _.find(command.commands, function (cmd) {
    return cmd._name === 'serve';
  });

  var desc = !command.description() ? '' : command.description();
  var cmdDef = !defCmd ? '' : '=' + defCmd._name;

  return ('\nUsage: ' + command._name + ' [command' + cmdDef + '] [options]\n\n' + desc + '\n\nCommands:\n' + indent(commandsSummary(command), 2) + '\n\n' + cmdHelp(defCmd) + '\n').trim().replace(/^/gm, spaces || '');
};

function indent(str, n) {
  return String(str || '').trim().replace(/^/gm, _.repeat(' ', n));
}

function commandsSummary(program) {
  var cmds = _.compact(program.commands.map(function (cmd) {
    var name = cmd._name;
    if (name === '*') return;
    var opts = cmd.options.length ? ' [options]' : '';
    var args = cmd._args.map(function (arg) {
      return humanReadableArgName(arg);
    }).join(' ');

    return [name + ' ' + opts + ' ' + args, cmd.description()];
  }));

  var cmdLColWidth = cmds.reduce(function (width, cmd) {
    return Math.max(width, cmd[0].length);
  }, 0);

  return cmds.reduce(function (help, cmd) {
    return '' + (help || '') + _.padRight(cmd[0], cmdLColWidth) + ' ' + (cmd[1] || '') + '\n';
  }, '');
}

function cmdHelp(cmd) {
  if (!cmd) return '';
  return ('\n"' + cmd._name + '" Options:\n\n' + indent(cmd.optionHelp(), 2) + '\n').trim();
}

function humanReadableArgName(arg) {
  var nameOutput = arg.name + (arg.variadic === true ? '...' : '');
  return arg.required ? '<' + nameOutput + '>' : '[' + nameOutput + ']';
}
