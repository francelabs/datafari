'use strict';

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _regeneratorRuntime = require('babel-runtime/regenerator')['default'];

var _require = require('util');

var inherits = _require.inherits;

var _require2 = require('lodash');

var defaults = _require2.defaults;

var _require3 = require('path');

var resolve = _require3.resolve;

var _require4 = require('fs');

var writeFile = _require4.writeFile;

var webpack = require('webpack');
var Boom = require('boom');
var DirectoryNameAsMain = require('webpack-directory-name-as-main');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CommonsChunkPlugin = require('webpack/lib/optimize/CommonsChunkPlugin');

var utils = require('requirefrom')('src/utils');
var fromRoot = utils('fromRoot');
var babelOptions = require('./babelOptions');
var babelExclude = [/[\/\\](webpackShims|node_modules|bower_components)[\/\\]/];

var BaseOptimizer = (function () {
  function BaseOptimizer(opts) {
    _classCallCheck(this, BaseOptimizer);

    this.env = opts.env;
    this.bundles = opts.bundles;
    this.profile = opts.profile || false;

    switch (opts.sourceMaps) {
      case true:
        this.sourceMaps = 'source-map';
        break;

      case 'fast':
        this.sourceMaps = 'cheap-module-eval-source-map';
        break;

      default:
        this.sourceMaps = opts.sourceMaps || false;
        break;
    }

    this.unsafeCache = opts.unsafeCache || false;
    if (typeof this.unsafeCache === 'string') {
      this.unsafeCache = [new RegExp(this.unsafeCache.slice(1, -1))];
    }
  }

  _createClass(BaseOptimizer, [{
    key: 'initCompiler',
    value: function initCompiler() {
      var compilerConfig;
      return _regeneratorRuntime.async(function initCompiler$(context$2$0) {
        var _this = this;

        while (1) switch (context$2$0.prev = context$2$0.next) {
          case 0:
            if (!this.compiler) {
              context$2$0.next = 2;
              break;
            }

            return context$2$0.abrupt('return', this.compiler);

          case 2:
            compilerConfig = this.getConfig();

            this.compiler = webpack(compilerConfig);

            this.compiler.plugin('done', function (stats) {
              if (!_this.profile) return;

              var path = resolve(_this.env.workingDir, 'stats.json');
              var content = JSON.stringify(stats.toJson());
              writeFile(path, content, function (err) {
                if (err) throw err;
              });
            });

            return context$2$0.abrupt('return', this.compiler);

          case 6:
          case 'end':
            return context$2$0.stop();
        }
      }, null, this);
    }
  }, {
    key: 'getConfig',
    value: function getConfig() {
      var mapQ = this.sourceMaps ? '?sourceMap' : '';

      return {
        context: fromRoot('.'),
        entry: this.bundles.toWebpackEntries(),

        devtool: this.sourceMaps,
        profile: this.profile || false,

        output: {
          path: this.env.workingDir,
          filename: '[name].bundle.js',
          sourceMapFilename: '[file].map',
          publicPath: '/bundles/',
          devtoolModuleFilenameTemplate: '[absolute-resource-path]'
        },

        recordsPath: resolve(this.env.workingDir, 'webpack.records'),

        plugins: [new webpack.ResolverPlugin([new DirectoryNameAsMain()]), new webpack.NoErrorsPlugin(), new ExtractTextPlugin('[name].style.css', {
          allChunks: true
        }), new CommonsChunkPlugin({
          name: 'commons',
          filename: 'commons.bundle.js'
        })],

        module: {
          loaders: [{
            test: /\.less$/,
            loader: ExtractTextPlugin.extract('style', 'css' + mapQ + '!autoprefixer' + (mapQ ? mapQ + '&' : '?') + '{ "browsers": ["last 2 versions","> 5%"] }!less' + mapQ)
          }, { test: /\.css$/, loader: ExtractTextPlugin.extract('style', 'css' + mapQ) }, { test: /\.jade$/, loader: 'jade' }, { test: /\.(html|tmpl)$/, loader: 'raw' }, { test: /\.png$/, loader: 'url?limit=10000&name=[path][name].[ext]' }, { test: /\.(woff|woff2|ttf|eot|svg|ico)(\?|$)/, loader: 'file?name=[path][name].[ext]' }, { test: /[\/\\]src[\/\\](plugins|ui)[\/\\].+\.js$/, loader: 'rjs-repack' + mapQ }, {
            test: /\.js$/,
            exclude: babelExclude.concat(this.env.noParse),
            loader: 'babel',
            query: babelOptions.webpack
          }, {
            test: /\.jsx$/,
            exclude: babelExclude.concat(this.env.noParse),
            loader: 'babel',
            query: defaults({
              nonStandard: true
            }, babelOptions.webpack)
          }].concat(this.env.loaders),
          postLoaders: this.env.postLoaders || [],
          noParse: this.env.noParse
        },

        resolve: {
          extensions: ['.js', '.jsx', '.less', ''],
          postfixes: [''],
          modulesDirectories: ['webpackShims', 'node_modules'],
          loaderPostfixes: ['-loader', ''],
          root: fromRoot('.'),
          alias: this.env.aliases,
          unsafeCache: this.unsafeCache
        }
      };
    }
  }, {
    key: 'failedStatsToError',
    value: function failedStatsToError(stats) {
      var statFormatOpts = {
        hash: false, // add the hash of the compilation
        version: false, // add webpack version information
        timings: false, // add timing information
        assets: false, // add assets information
        chunks: false, // add chunk information
        chunkModules: false, // add built modules information to chunk information
        modules: false, // add built modules information
        cached: false, // add also information about cached (not built) modules
        reasons: false, // add information about the reasons why modules are included
        source: false, // add the source code of modules
        errorDetails: false, // add details to errors (like resolving log)
        chunkOrigins: false, // add the origins of chunks and chunk merging info
        modulesSort: false, // (string) sort the modules by that field
        chunksSort: false, // (string) sort the chunks by that field
        assetsSort: false, // (string) sort the assets by that field
        children: false
      };

      var details = stats.toString(defaults({ colors: true }, statFormatOpts));

      return Boom.create(500, 'Optimizations failure.\n' + details.split('\n').join('\n    ') + '\n', stats.toJson(statFormatOpts));
    }
  }]);

  return BaseOptimizer;
})();

module.exports = BaseOptimizer;
