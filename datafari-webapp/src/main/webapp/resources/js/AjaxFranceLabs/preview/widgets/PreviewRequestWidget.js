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
AjaxFranceLabs.PreviewRequestWidget = AjaxFranceLabs.Class.extend({

  // Variables
  id : "preview-request",
  type : "request",

  // Methods
  requestFromQuery : function(servlet, params, docPos, responseHandler) {
    // The statistics need to also retrieve the previous and next doc of the request
    // So set the num of rows to 3 and the startPos to original docPos - 1 (unless of course the original docPos is 0)
    let rows = 3;
    let startPos = docPos - 1;
    if (docPos == 0) {
      rows = 2;
      startPos = 0;
    }
    params = params.replace(/fl=[^&]*&/, "");
    params = params.replace(/rows=[0-9]*/, "rows=" + rows);
    params = params.replace(/start=[0-9]*/, "");
    params += "&start=" + startPos;
    $.get(servlet + params + '&wt=json&json.wrf=?', responseHandler, "json");
  },

  requestFromDocId : function(servlet, docId, responseHandler, aggregator) {
    var params = "id:(\"" + docId + "\")";
    var queryBody = {
        q : params,
        wt : "json"
    }
    if (aggregator && aggregator.length !== 0) {
      queryBody.aggregator = aggregator;
    }
    $.get(servlet + "?json.wrf=?", queryBody, responseHandler, "json");
  }
});
