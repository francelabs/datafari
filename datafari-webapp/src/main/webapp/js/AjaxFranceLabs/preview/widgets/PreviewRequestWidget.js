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

  requestFromDocId : function(servlet, docId, responseHandler) {
    // Normalize docId before sending the request, which means to URI encode the docId
    var docParts = [];
    var docBase = "";
    // If the docId is an http link or a file link, then remove the docBase (file:// for files and http(s):// for http links) from the
    // encoding process because the ":" char must not be encoded !
    if (docId.startsWith("http") || docId.startsWith("file:")) {
      var index = docId.indexOf("://") + 3;
      docBase = docId.substring(0, index);
      var docPath = docId.substring(index);
      // Split on the "/" char because everything must be encoded BUT this char
      docParts = docPath.split("/");
    } else {
      docParts = docId.split("/");
    }
    var encodedDocPath = "";
    // URI encode every chars encountered between "/"
    for (var i = 0; i < docParts.length; i++) {
      var partValue = docParts[i];
      if (partValue != "") {
        encodedDocPath += encodeURIComponent(partValue).replace(/'/g, "%27").replace(/!/g, "%21").replace(/~/g, "%7E").replace(/\(/g, "%28").replace(/\)/g, "%29") + "/";
      } else {
        encodedDocPath += "/";
      }
    }
    // Remove the last char as it obviously is an additional "/" char added by the previous for loop
    encodedDocPath = encodedDocPath.substring(0, encodedDocPath.length - 1);
    var encodedDocId = "";
    if (docId.startsWith("http") || docId.startsWith("file:")) {
      encodedDocId = docBase + encodedDocPath;
    } else {
      encodedDocId = encodedDocPath;
    }
    var params = "id:(\"" + encodedDocId + "\")";
    $.get(servlet + "?json.wrf=?", {
      q : params,
      wt : "json"
    }, responseHandler, "json");
  }
});
