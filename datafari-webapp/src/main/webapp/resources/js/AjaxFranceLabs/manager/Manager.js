/*******************************************************************************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************************************************************************/
AjaxFranceLabs.Manager = AjaxFranceLabs.AbstractManager.extend({

  defaultTimeout : 60000,

  /**
   * A timeout of 0 means no timeout. If timeout is invalid, the default timeout is used.
   */
  executeRequest : function(server, servlet, string, handler, errorHandler, requestTimeout) {
    var self = this;
    server = server || this.serverUrl;
    servlet = servlet || this.servlet;
    string = string || this.store.string();
    handler = handler || function(data) {
      self.handleResponse(data);
    };
    errorHandler = errorHandler || function(jqxhr, status, error) {
      self.handleError(jqxhr, status, error);
    };
    if (!Number.isInteger(requestTimeout) || requestTimeout < 0) {
      requestTimeout = self.defaultTimeout;
    }
    if (this.proxyUrl) {
      return $.post(this.proxyUrl, {
        query : string
      }, handler, 'json');
    } else {
      // Using $.ajax to provide a timeout. Timeout can't be given using $.get.
      return $.ajax({
        url : server + servlet + '?' + string + '&wt=json&json.wrf=?',
        dataType : "json",
        success : handler,
        error : errorHandler,
        timeout : requestTimeout
      });
    }
  },

  executeExternalRequest : function(externalSource) {
    externalSource.manager = this;
    externalSource.prepareRequest();
    var self = this;
    server = externalSource.serverUrl;
    servlet = externalSource.servlet;
    request = this.store.string();
    parameters = JSON.stringify(externalSource.parameters);
    handler = function(data) {
      externalSource.handler(data);
    };
    errorHandler = externalSource.errorHandler;
    requestTimeout = externalSource.requestTimeout
    if (!Number.isInteger(requestTimeout) || requestTimeout < 0) {
      requestTimeout = self.defaultTimeout;
    }
    if (this.proxyUrl) {
      return $.post(this.proxyUrl, {
        query : string
      }, handler, 'json');
    } else {
      // Using $.ajax to provide a timeout. Timeout can't be given using $.get.

      return $.ajax({
        url : server + servlet + "?" + request,
        type : "POST",
        data : {
          parameters : parameters
        },
        dataType : "json",
        success : handler,
        error : errorHandler,
        timeout : requestTimeout
      });
    }
  }

});
