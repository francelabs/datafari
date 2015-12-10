'use strict';

var _ = require('lodash');
var Promise = require('bluebird');
var Boom = require('boom');
var getBasicAuthRealm = require('./get_basic_auth_realm');

module.exports = function (client) {
  return function (req, endpoint) {
    var params = arguments.length <= 2 || arguments[2] === undefined ? {} : arguments[2];

    if (req.headers.authorization) {
      _.set(params, 'headers.authorization', req.headers.authorization);
    }
    var api = _.get(client, endpoint);
    if (!api) throw new Error('callWithRequest called with an invalid endpoint: ' + endpoint);
    return api.call(client, params)['catch'](function (err) {
      if (err.status === 401) {
        // TODO: The err.message is temporary until we have support for getting headers in the client.
        // Once we have that, we should be able to pass the contents of the WWW-Authenticate head to getRealm
        var realm = getBasicAuthRealm(err.message) || 'Authorization Required';
        var options = { realm: realm };
        return Promise.reject(Boom.unauthorized('Unauthorized', 'Basic', options));
      }
      return Promise.reject(err);
    });
  };
};
