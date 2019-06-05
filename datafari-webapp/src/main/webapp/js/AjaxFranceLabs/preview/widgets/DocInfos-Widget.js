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
AjaxFranceLabs.DocInfosWidget = AjaxFranceLabs.WidgetCore.extend({

  // Variables
  elm : null,
  name : null,

  // Methods

  initContent : function(widgetContentDiv) {

  },

  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
    var doc = data.response.docs[0];
    widgetContentDiv.html("");
    var docIdDiv = $("<div id='docId'></div>");
    widgetContentDiv.append(docIdDiv);
    var sourceLink = doc.id;
    // If there is a qId then change the sourceLink so that the URL servlet can
    // update the statistics (one click for this doc)
    if (qId != null && qId != undefined && qId != "") {
      sourceLink = "URL?url=" + doc.id + "&id=" + qId;
    }
    widgetContentDiv.append('<div>' + '<div id="docIcon" class="doc-icon"></div>' + '<div class="heading-content">' + '<div>' + '<h1 id="docTitle"></h1>' + '</div>' + '<div class="heading-desc">'
        + '<div id="doclastModified" class="heading-date"></div>' + '<a target="_blank" class="source-doc-link heading-box" href="' + sourceLink + '">' + '<i class="fas fa-download"></i> '
        + window.i18n.msgStore['preview-open-from-source'] + '</a>' + '<div class="heading-box" style="display:none;">' + '<i class="fas fa-bookmark-o"></i>' + '</div>'
        + '<div class="heading-box" style="display:none;">' + '<img src="images/icons/pdf-icon-24x24.png"/>' + '</div>' + '</div>' + '</div>' + '</div>');
    $("#docId").html(doc.id);
    var extension = doc.extension;

    if (extension !== undefined && extension != "") {
      var icon = "";
      var path = "images/icons/";
      if (AjaxFranceLabs.imageExists("./images/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
        icon = extension.toLowerCase() + '-icon-24x24.png';
      } else if (AjaxFranceLabs.imageExists("./customs/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
        path = "customs/icons/";
        icon = extension.toLowerCase() + '-icon-24x24.png';
      } else {
        icon = "default-icon-24x24.png";
      }
      $("#docIcon").html('<img src="' + path + icon + '"/>&nbsp;');
    } else {
      $("#docIcon").html('<img src="images/icons/default-icon-24x24.png"/>&nbsp;');
    }
    var title = "";
    // if the document is an html file, get the extracted title metadata
    if (doc.extension == "html") {
      title = doc.title;
      // for other files, get the filename from the url
    } else {
      title = doc.url.split('/');
      title = decodeURIComponent(title[title.length - 1]);
    }
    $("#docTitle").html(title);
    var lastModified = window.i18n.msgStore['preview-modified-on'] + " ";
    if (doc.last_author != undefined && doc.last_author != null) {
      lastModified = window.i18n.msgStore['preview-modified-by'] + " " + doc.last_author + " " + window.i18n.msgStore['preview-on'] + " ";
    }
    lastModified += doc.last_modified;
    $("#doclastModified").html(lastModified);
  }
});
