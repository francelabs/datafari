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
  elm : null,
  name : null,

  // Methods

  initContent : function(widgetContentDiv) {

  },

  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
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
      var idDocLink = idDoc;
      var sharePreviewLink = window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/Datafari/Preview?docId=" + idDocLink
      var sharePreview = $('<div class="share-link-div"><i class="fas fa-chevron-right"></i> ' + window.i18n.msgStore['preview-share-preview'] + ' <br/><input type="text" value="' + sharePreviewLink
          + '" /></div>');

      var shareDoc = $('<div class="share-link-div"><i class="fas fa-chevron-right"></i> ' + window.i18n.msgStore['preview-share-doc'] + ' <br/><input type="text" value="' + idDoc + '" /></div>');
      widgetContentDiv.html("");
      widgetContentDiv.append(sharePreview).append(shareDoc);
    } else {
      widgetDiv.hide();
    }
  }
});
