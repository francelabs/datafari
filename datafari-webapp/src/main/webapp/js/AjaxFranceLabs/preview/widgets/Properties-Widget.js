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
AjaxFranceLabs.PropertiesWidget = AjaxFranceLabs.FacetCore.extend({

  // Variables
  elm : null,
  name : null,

  // Methods

  initContent : function(widgetContentDiv) {

  },

  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
    if (data != undefined && data != null) {
      var doc = data.response.docs[0];
      widgetContentDiv.html("");

      var titles = "(" + window.i18n.msgStore['preview-no-title'] + ")";
      if (doc.title != undefined && doc.title != null) {
        titles = "";
        for (var i = 0; i < doc.title.length; i++) {
          if (titles == "") {
            titles = doc.title[i];
          } else {
            titles += " || " + doc.title[i];
          }
        }
      }
      widgetContentDiv.append("<div class='property-line'><span class='property-name'>" + window.i18n.msgStore['preview-titles'] + "</span> <span class='property-value'>" + titles + "</span></div>");

      var description = "(" + window.i18n.msgStore['preview-no-description'] + ")";
      if (doc.description != undefined && doc.description != null) {
        description = doc.description;
      }
      widgetContentDiv.append("<div class='property-line'><span class='property-name'>" + window.i18n.msgStore['preview-description'] + "</span> <span class='property-value'>" + description
          + "</span></div>");

      var authors = "(" + window.i18n.msgStore['preview-no-authors'] + ")";
      if (doc.author != undefined && doc.author != null) {
        authors = "";
        for (var i = 0; i < doc.author.length; i++) {
          if (authors == "") {
            authors = doc.author[i];
          } else {
            authors += " , " + doc.author[i];
          }
        }
      }
      if (doc.last_author != undefined && doc.last_author != null) {
        if (authors == "(" + window.i18n.msgStore['preview-no-authors'] + ")") {
          authors = "";
        }
        for (var i = 0; i < doc.last_author.length; i++) {
          if (authors == "") {
            authors = doc.last_author[i];
          } else {
            authors += " , " + doc.last_author[i];
          }
        }
      }
      widgetContentDiv
          .append("<div class='property-line'><span class='property-name'>" + window.i18n.msgStore['preview-authors'] + "</span> <span class='property-value'>" + authors + "</span></div>");

      var mime = "(" + window.i18n.msgStore['preview-no-mime'] + ")";
      if (doc.mime != undefined && doc.mime != null) {
        mime = doc.mime;
      }
      widgetContentDiv.append("<div class='property-line'><span class='property-name'>Mime type</span> <span class='property-value'>" + mime + "</span></div>");

      var created = "(" + window.i18n.msgStore['preview-no-date'] + ")";
      if (doc.creation_date != undefined && doc.creation_date != null) {
        created = doc.creation_date;
      }
      widgetContentDiv.append("<div class='property-line'><span class='property-name'>" + window.i18n.msgStore['preview-created-on'] + "</span> <span class='property-value'>" + created
          + "</span></div>");

      var modified = "(" + window.i18n.msgStore['preview-no-date'] + ")";
      if (doc.last_modified != undefined && doc.last_modified != null) {
        modified = doc.last_modified;
      }
      widgetContentDiv.append("<div class='property-line'><span class='property-name'>" + window.i18n.msgStore['lastModification'] + "</span> <span class='property-value'>" + modified
          + "</span></div>");
    } else {
      widgetDiv.hide();
    }
  }
});
