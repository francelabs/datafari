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
AjaxFranceLabs.PreviewNavigationWidget = AjaxFranceLabs.FacetCore.extend({

  // Variables
  elm : null,
  name : null,

  // Methods

  initContent : function(widgetContentDiv) {

  },

  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
    if (!isNaN(docPos) && docPos != undefined && docPos != null) {
      params = params.replace(/action=[^&]*\&/g, "");
      var previousDoc = $("<span class='preview-nav-button'><i class='fa fa-chevron-left'></i> " + window.i18n.msgStore['preview-previous-doc'] + "</span>");
      previousDoc.click(function() {
        var newDocId = data.response.docs[0].id;
        if (newDocId.startsWith("http")) {
          newDocId = encodeURIComponent(newDocId);
        }
        var prevParams = params.replace(/docId=[^&]*\&/g, "");
        prevParams = "docId=" + newDocId + "&" + prevParams;
        var newDocPos = docPos - 1;
        window.location.href = "Preview?docPos=" + newDocPos + "&action=PREVIEW_CHANGE_DOC&" + prevParams;
      });
      var nextDoc = $("<span class='preview-nav-button'>" + window.i18n.msgStore['preview-next-doc'] + " <i class='fa fa-chevron-Right'></i></span>");
      nextDoc.click(function() {
        var newDocId = data.response.docs[data.response.docs.length-1].id;
        if (newDocId.startsWith("http")) {
          newDocId = encodeURIComponent(newDocId);
        }
        var nextParams = params.replace(/docId=[^&]*\&/g, "");
        nextParams = "docId=" + newDocId + "&" + nextParams;
        var newDocPos = docPos + 1;
        window.location.href = "Preview?docPos=" + newDocPos + "&action=PREVIEW_CHANGE_DOC&" + nextParams;
      });
      widgetContentDiv.html("");
      var displayWidget = false;
      if (docPos > 0) {
        widgetContentDiv.append(previousDoc);
        displayWidget = true;
      }
      if (docPos < data.response.numFound - 1) {
        widgetContentDiv.append(nextDoc);
        displayWidget = true;
      }
      if (!displayWidget) {
        widgetDiv.hide();
      }
    } else {
      widgetDiv.hide();
    }
  }
});
