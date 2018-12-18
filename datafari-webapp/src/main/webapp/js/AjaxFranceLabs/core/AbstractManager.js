/*****************************************************************************************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 ****************************************************************************************************************************************************/
AjaxFranceLabs.AbstractManager = AjaxFranceLabs.Class.extend({

  // Variables

  constellio : true,

  serverUrl : 'http://localhost:8080/constellio/app/',

  servlet : 'select',

  response : {},

  widgets : {},

  modules : {},

  externalSources : {},

  store : new AjaxFranceLabs.ParameterStore({}),

  initialized : false,

  collections : [],

  collection : null,

  connectionInfo : {},

  // Used to abort a previous request
  previousRequestXhr : null,

  // Methods

  init : function() {
    if (this.initialized) {
      return $.Deferred().resolve;
    }
    if (this.constellio && this.connectionInfo.collection === undefined) {
      throw 'AbstractManager: connectionInfo.collection not defined';
    }
    var self = this;
    this.initialized = true;
    this.store.addByValue('q', '*:*');
    this.store.addByValue('rows', 10);
    if (this.constellio) {
      return $.when(
          this.executeRequest(this.connectionInfo.collection.serverUrl, this.connectionInfo.collection.servlet,
              this.connectionInfo.collection.queryString, function(data) {
                self.collection = data.defaultCollection;
                self.collections = data.collections;
              })).done(function() {
        self.store.addByValue('collectionName', self.collection);
      }).always(function() {
        for ( var widget in self.widgets) {
          self.widgets[widget].init();
        }
        for ( var module in self.modules) {
          self.modules[module].init();
        }
      });
    } else {
      for ( var widget in self.widgets) {
        self.widgets[widget].init();
      }
      for ( var module in self.modules) {
        self.modules[module].init();
      }
      return $.Deferred().resolve;
    }
  },

  addExternalSource : function(source) {
    this.externalSources[source.id] = source;
  },

  addModule : function(module) {
    module.manager = this;
    this.modules[module.id] = module;
  },

  addWidget : function(widget) {
    widget.manager = this;
    this.widgets[widget.id] = widget;
  },

  handleResponse : function(data) {
    this.response = data;
    for ( var widget in this.widgets) {
      this.widgets[widget].afterRequest();
    }
    for ( var module in this.modules) {
      this.modules[module].afterRequest();
    }
  },

  handleError : function(jqxhr, status, error) {
    for ( var widget in this.widgets) {
      this.widgets[widget].requestError(status, error);
    }
    for ( var module in this.modules) {
      this.modules[module].requestError(status, error);
    }
  },

  makeRequest : function(servlet) {
    var self = this;
    // Abort the previous request in case not finished to avoid weird behavior
    // in the search UI
    if (self.previousRequestXhr != null && self.previousRequestXhr != undefined) {
      self.previousRequestXhr.abort();
    }
    $.when(this.init()).done(function() {
      for ( var widget in self.widgets) {
        self.widgets[widget].beforeRequest();
      }
      for ( var module in self.modules) {
        self.modules[module].beforeRequest();
      }
      // Keep the request object to be able to abort it
      self.previousRequestXhr = self.executeRequest('', servlet);

      // Perform external sources requests
      for ( var externalSourceId in self.externalSources) {
        var externalSource = self.externalSources[externalSourceId];
        self.executeExternalRequest(externalSource);
      }
    });
  },

  generateAndSetQueryID : function() {
    this.store.addByValue("id", UUID.generate());
  },

  executeExternalRequest : function() {
    throw 'AbstractManager.executeRequest must be implemented.';
  },

  executeRequest : function() {
    throw 'AbstractManager.executeRequest must be implemented.';
  },

  /**
   * Get the handle to the widget with ID specified as input
   */
  getWidgetByID : function(id) {
    for ( var widget in this.widgets) {
      if (this.widgets[widget].id === id) {
        return this.widgets[widget];
      }
    }
  },

  /**
   * Perform a select all (*:*) request
   */
  makeDefaultRequest : function() {
    // Update the Q parameter of the manager store, to be used for
    // the new search query
    this.store.get('q').val('*:*');

    // Submit a new query with the value set in the Q parameter of the manager
    // store
    this.generateAndSetQueryID();
    this.makeRequest();

    // Reset the URL: window.location.origin not used as it is not standard
    window.history.replaceState({
      'Default search request' : ''
    }, 'Default search request', window.location.protocol + '//' + window.location.host + window.location.pathname + '?query=*:*&lang='
        + window.i18n.language);

  }
});
