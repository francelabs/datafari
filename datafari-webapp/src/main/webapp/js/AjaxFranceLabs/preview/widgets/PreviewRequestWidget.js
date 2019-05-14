/*******************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
AjaxFranceLabs.PreviewRequestWidget = AjaxFranceLabs.Class.extend({

  // Variables
  id : "preview-request",
  type : "request",

  // Methods
  requestFromQuery : function(servlet, params, docPos, responseHandler) {
    params = params.replace(/fl=[^&]*&/, "");
    params = params.replace(/rows=[0-9]*/, "rows=1");
    params = params.replace(/start=[0-9]*/, "");
    params += "&start=" + docPos;
    $.get(servlet + params + '&wt=json&json.wrf=?', responseHandler, "json");
  },

  requestFromDocId : function(servlet, docId, responseHandler) {
    // Normalize docName before sending the request
    var index = docId.lastIndexOf("/") + 1;
    var docBase = docId.substring(0, index);
    var docName = docId.substring(index);
    docId = docBase + encodeURIComponent(docName).replace(/'/g, "%27").replace(/!/g, "%21").replace(/~/g, "%7E").replace(/\(/g, "%28").replace(/\)/g, "%29");
    var params = "id:(\"" + docId + "\")";
    $.get(servlet + "?json.wrf=?", {
      q : params,
      wt : "json"
    }, responseHandler, "json");
  }
});
