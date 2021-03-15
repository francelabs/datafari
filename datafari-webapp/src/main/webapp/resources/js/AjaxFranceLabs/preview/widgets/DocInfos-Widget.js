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
AjaxFranceLabs.DocInfosWidget = AjaxFranceLabs.WidgetCore.extend({

  // Variables
  elm : null,
  name : null,

  // Methods

  initContent : function(widgetContentDiv) {

  },

  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
    var doc = data.response.docs[0];
    // The statistics need info about the previous and next doc so they are always returned in the query response.
    // So, the current doc that must be displayed in the preview is always the in the 2nd position of the array of docs
    // Unless of course it is the first doc of the results. In that case the doc is the 1st of the array as it has no previous doc !
    // This is the reason why if the docPos is present and different from 0 (1st position) then take the second doc of the array
    if (docPos && docPos != 0) {
      doc = data.response.docs[1];
    }
    widgetContentDiv.html("");
    var docIdDiv = $("<div id='docId'></div>");
    widgetContentDiv.append(docIdDiv);
    var sourceLink = doc.url;
    // If there is a qId then change the sourceLink so that the URL servlet can
    // update the statistics (one click for this doc)
    if (qId != null && qId != undefined && qId != "") {
      sourceLink = "URL?url=" + doc.url + "&id=" + qId + "&action=OPEN_FROM_PREVIEW";
    }
    widgetContentDiv.append('<div>' + '<div id="docIcon" class="doc-icon"></div>' + '<div class="heading-content">' + '<div>' + '<h1 id="docTitle"></h1>' + '</div>' + '<div class="heading-desc">'
        + '<div id="doclastModified" class="heading-date"></div>' + '<a id="sourceLink" target="_blank" class="source-doc-link heading-box" href="' + sourceLink + '">'
        + '<i class="fas fa-download"></i> ' + window.i18n.msgStore['preview-open-from-source'] + '</a>' + '<div class="heading-box" style="display:none;">' + '<i class="fas fa-bookmark-o"></i>'
        + '</div>' + '<div class="heading-box" style="display:none;">' + '<img src="images/icons/pdf-icon-24x24.png"/>' + '</div>' + '</div>' + '</div>' + '</div>');
    $("#docId").html(doc.id);
    var extension = doc.extension;

    if (extension !== undefined && extension != "") {
      var icon = "";
      var path = "resources/images/icons/";
      if (AjaxFranceLabs.imageExists("resources/images/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
        icon = extension.toLowerCase() + '-icon-24x24.png';
      } else if (AjaxFranceLabs.imageExists("resources/customs/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
        path = "resources/customs/icons/";
        icon = extension.toLowerCase() + '-icon-24x24.png';
      } else {
        icon = "default-icon-24x24.png";
      }
      $("#docIcon").html('<img src="' + path + icon + '"/>&nbsp;');
    } else {
      $("#docIcon").html('<img src="resources/images/icons/default-icon-24x24.png"/>&nbsp;');
    }
    var title = doc.title[0].truncate(75, 36);
    $("#docTitle").html(title);
    var lastModified = window.i18n.msgStore['preview-modified-on'] + " ";
    if (doc.last_author != undefined && doc.last_author != null) {
      lastModified = window.i18n.msgStore['preview-modified-by'] + " " + doc.last_author + " " + window.i18n.msgStore['preview-on'] + " ";
    }
    lastModified += doc.last_modified[0];
    $("#doclastModified").html(lastModified);
  }
});
