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
AjaxFranceLabs.PreviewManager = AjaxFranceLabs.Class.extend({

  // Variables
  widgets : {},
  requestWidgetId : null,
  contentBuilderWidgetId : null,

  // Methods

  init : function() {
    var self = this;
  },

  addWidget : function(widget) {
    if (widget.type == "request") {
      this.addRequestWidget(widget);
    } else if (widget.type == "content-builder") {
      this.addContentBuilderWidget(widget);
    } else {
      widget.buildWidget();
      this.widgets[widget.id] = widget;
    }
  },

  addRequestWidget : function(widget) {
    this.requestWidgetId = widget.id;
    this.widgets[widget.id] = widget;
  },

  addContentBuilderWidget : function(widget) {
    this.contentBuilderWidgetId = widget.id;
    this.widgets[widget.id] = widget;
  },

  buildPreviewContent : function(doc, docContentDiv) {
    var contentBuilderWidget = this.widgets[this.contentBuilderWidgetId];
    contentBuilderWidget.buildContent(doc, docContentDiv);
  },

  performRequestFromQuery : function(servlet, params, docPos, responseHandler) {
    var requestWidget = this.widgets[this.requestWidgetId]
    requestWidget.requestFromQuery(servlet, params, docPos, responseHandler);
  },

  performRequestFromDocId : function(servlet, docId, responseHandler, aggregator) {
    var requestWidget = this.widgets[this.requestWidgetId]
    requestWidget.requestFromDocId(servlet, docId, responseHandler, aggregator);
  },

  removeWidget : function(widgetId) {
    if (widgetId in this.widgets) {
      var widget = this.widgets[widgetId]
      if (widget != undefined && widget != null) {
        widget.manager = null;
        delete this.widgets[widgetId];
      }
    }
  },

  getFieldsList : function() {
    var self = this;
    var fields = "";
    for ( var widget in self.widgets) {
      if (fields == "") {
        self.widgets[widget].getFields();
      } else {
        fields += "%2C" + self.widgets[widget].getFields();
      }
    }
  },

  uiUpdate : function(docContentDiv, docId, docPos, params, data, qId) {
    var self = this;
    for ( var widget in self.widgets) {
      if (self.widgets[widget].type != "request" && self.widgets[widget].type != "content-builder") {
        self.widgets[widget].update(docContentDiv, docId, docPos, params, data, qId);
      }
    }
  }
});
