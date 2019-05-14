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
AjaxFranceLabs.PreviewContentBuilderWidget = AjaxFranceLabs.Class.extend({

  // Variables
  id : "preview-content-builder",
  type : "content-builder",

  // Methods
  buildContent : function(doc, docContentDiv) {

    var docContent = "";
    if (doc.emptied != null && doc.emptied != undefined && doc.emptied == true) {
      docContent = window.i18n.msgStore['emptied_content'];
    } else {
      docContent = this.extractContent(doc);
    }
    this.fillContentDiv(docContent, docContentDiv);

  },

  extractContent : function(doc) {
    // Define the doc content
    var docContent = "";
    for (var i = 0; i < doc.exactContent.length; i++) {
      if (doc.exactContent[i] != undefined && doc.exactContent[i] != null && doc.exactContent[i].trim() != "") {
        docContent = doc.exactContent[i];
      }
    }
    return docContent;
  },

  fillContentDiv : function(docContent, docContentDiv) {
    docContentDiv.html(docContent);
  }
});
