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
AjaxFranceLabs.FacetCore = AjaxFranceLabs.WidgetCore.extend({

  // Variables
  name : null,

  // Methods

  buildWidget : function() {
    var endAnimationEvents = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
    var animation = 'animated rotateIn';
    var self = this, elm = $(this.elm);
    elm.hide();
    elm.addClass('preview-widget box box-margin').attr('widgetId', this.id).append("<div class='facet widget-content'></div>");
    if (this.name != null) {
      elm.prepend('<div class="facetName">').find('.facetName').append('<i class="fas fa-chevron-down"></i>').append('<span class="label la"></span>').find('.label.la').append(this.name);
      elm.find('.facetName').click(function() {
        $('.widget-content', $(this).parents('.preview-widget')).toggle();
        var chevron = elm.find(".facetName i");
        if (chevron.hasClass("fa-chevron-down")) {
          chevron.removeClass('fa-chevron-down').addClass('fa-chevron-up ' + animation).on(endAnimationEvents, function() {
            $(this).removeClass(animation);
          });
        } else {
          chevron.removeClass('fa-chevron-up').addClass(animation + ' fa-chevron-down').on(endAnimationEvents, function() {
            $(this).removeClass(animation);
          });
        }
      });
    }
    this.initContent(elm.find(".widget-content"));
  },

  // To override
  initContent : function(widgetContentDiv) {

  },

  // To override
  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {

  }
});
