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
AjaxFranceLabs.WidgetCore = AjaxFranceLabs.Class.extend({

  // Variables
  id : null,
  elm : null,
  fieldsList : [],

  // Methods

  buildWidget : function() {
    var endAnimationEvents = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
    var animation = 'animated rotateIn';
    var self = this, elm = $(this.elm);
    elm.hide();
    elm.addClass('preview-widget').attr('widgetId', this.id).append("<div class='widget-content'></div>");
    this.initContent(elm.find(".widget-content"));
  },

  getFields : function() {
    var fields = "";
    for (var i = 0; i < this.fieldsList.length; i++) {
      if (fields == "") {
        fields = fieldsList[i];
      } else {
        fields += "%2C" + fieldsList[i];
      }
    }
    return fields;
  },

  // To override
  initContent : function(widgetContentDiv) {

  },

  update : function(docContentDiv, docId, docPos, params, data) {
    var widgetDiv = $(this.elm);
    widgetDiv.show();
    var widgetContentDiv = widgetDiv.find(".widget-content");
    this.updateWidgetContent(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data)
  },

  // To override
  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data) {

  }
});
