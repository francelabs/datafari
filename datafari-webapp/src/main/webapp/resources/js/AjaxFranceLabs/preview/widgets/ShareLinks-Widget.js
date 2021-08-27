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
AjaxFranceLabs.ShareLinksWidget = AjaxFranceLabs.FacetCore.extend({

  // Variables
  elm: null,
  name: null,

  // Methods

  initContent: function(widgetContentDiv) {

  },

  getQueryString: function() {
    var key = false, res = {}, itm = null;
    // get the query string without the ?
    var qs = window.location.search.substring(1);
    // check for the key as an argument
    if (arguments.length > 0) {
      key = arguments[0];
    }
    // make a regex pattern to grab key/value
    var pattern = /([^&=]+)=([^&]*)/g;
    // loop the items in the query string, either
    // find a match to the argument, or build an object
    // with key/value pairs
    while (itm = pattern.exec(qs)) {
      if (key !== false && decodeURIComponent(itm[1]) === key) {
        return decodeURIComponent(itm[2]);
      } else if (key === false) {
        res[decodeURIComponent(itm[1])] = decodeURIComponent(itm[2]);
      }
    }

    return key === false ? res : null;
  },

  decodeURLIfWebLink: function(url) {
    if (!url.startsWith("http")) {
      return url;
    } else {
      var paramIndex = url.indexOf("?");
      if (paramIndex != -1) {
        var path = url.substring(0, paramIndex);
        var params = url.substring(paramIndex + 1);
        var decodedUrl = path + "?" + decodeURIComponent(params);
        return decodedUrl;
      } else {
        return url;
      }
    }
  },

  updateWidgetContent: function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
    if (data != undefined && data != null) {
      var doc = data.response.docs[0];
      // The statistics need info about the previous and next doc so they are always returned in the query response.
      // So, the current doc that must be displayed in the preview is always the in the 2nd position of the array of docs
      // Unless of course it is the first doc of the results. In that case the doc is the 1st of the array as it has no previous doc !
      // This is the reason why if the docPos is present and different from 0 (1st position) then take the second doc of the array
      if (docPos && docPos != 0) {
        doc = data.response.docs[1];
      }
      var idDoc = doc.id;
      var idDocLink = encodeURIComponent(idDoc);
      var aggregator = this.getQueryString("aggregator");
      var sharePreviewLink = window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/Datafari/Preview?docId=" + idDocLink;
      if (aggregator && aggregator.length !== 0) {
        sharePreviewLink = sharePreviewLink + "&aggregator=" + aggregator;
      }
      var sharePreview = $('<div class="share-link-div"><i class="fas fa-chevron-right"></i> ' + window.i18n.msgStore['preview-share-preview'] + ' <br/><input type="text" value="' + sharePreviewLink
        + '" /></div>');

      var shareDoc = $('<div class="share-link-div"><i class="fas fa-chevron-right"></i> ' + window.i18n.msgStore['preview-share-doc'] + ' <br/><input type="text" value="' + this.decodeURLIfWebLink(doc.url) + '" /></div>');
      widgetContentDiv.html("");
      widgetContentDiv.append(sharePreview).append(shareDoc);
    } else {
      widgetDiv.hide();
    }
  }
});
